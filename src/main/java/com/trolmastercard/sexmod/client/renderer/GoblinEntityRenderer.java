package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.NpcModelCodeEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.io.IOException;
import java.util.UUID;

/**
 * GoblinEntityRenderer - ported from dy.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Top-level renderer for GoblinEntity (e3). Extends {@link GoblinBodyRenderer} (dx),
 * which in turn extends {@link NpcBodyRenderer} (dm).
 *
 * Key responsibilities:
 *  1. Skin texture resolution - uses the carrier/owner UUID for the skin when
 *     the goblin is being carried (shoulder-idle / pick-up state), or falls
 *     back to the entity's own UUID.
 *  2. Shoulder/pick-up rendering - when partialTick == SENTINEL (-420.69) the
 *     caller is the shoulder handler, so we enter the special shoulder-idle path.
 *  3. Pre-render setup - reads light level at goblin's block position and stores
 *     it in {@code z} field for body-tint use.
 *  4. Body-bone pivot offset - nudges the "body" bone's pivotY by -0.15 to
 *     align the goblin with the world correctly.
 *  5. Player-facing alignment - when the goblin is in a sex state that involves
 *     a player (e.g. CATCH/BJ), aligns goblin rotation to face the partner.
 *  6. Static helpers:
 *     {@link #preloadAll()} - forces model cache to refresh (dy.c() in original)
 *     {@link #renderForPlayer(GoblinEntity, Player, double, double, double, float)} - mirrors
 *     the static {@code a(em, float)} helper used by GoblinBodyRenderer.
 *
 * 1.12.2 - 1.20.1 migrations:
 *  - Render - EntityRenderer (via GeoEntityRenderer inheritance chain)
 *  - d6<e3> - GoblinBodyRenderer extends NpcBodyRenderer<GoblinEntity>
 *  - animatedGeoModel - GeoModel<GoblinEntity>
 *  - double shadow radius - removed (handled by renderer config)
 *  - func_76979_b - render
 *  - a(e3,...) entry point - render (overriding GoblinBodyRenderer)
 *  - a(em, float) static - preload/invalidate static method
 *  - Minecraft.func_175598_ae() - mc.getEntityRenderDispatcher()
 *  - mc.func_175598_ae().func_188391_a(..., SENTINEL, partialTick) - dispatcher.render with sentinel
 *  - mc.field_71439_g.getPersistentID() - mc.player.getGameProfile().getId()
 *  - mc.field_71474_y.field_74320_O - mc.options.getCameraType()
 *  - a(fp) == true for PICK_UP/SHOULDER_IDLE/RUN/CATCH/THROWN/START_THROWING - sex-render states
 *  - b6.a(prev, curr, t) - MathUtil.lerpVec3(...)
 *  - GlStateManager.func_179094_E/F/translate/rotate - PoseStack ops
 *  - entity.func_70040_Z() - entity.getLookAngle()
 *  - entity.field_70125_A / field_70126_B / field_70177_z - getXRot/xRotO/getYRot
 *  - GlStateManager.func_179137_b(x,y,z) - poseStack.translate(x,y,z)
 *  - param1Float1 -420.69F used as SENTINEL to detect shoulder rendering call
 *  - em.h() - entity.isSexModeActive() (checks if entity has sex partner)
 *  - em.y() - entity.getAnimState()
 *  - e3.a0 - GoblinEntity.HELD_ITEM
 *  - e4.a(entity) - NpcModelCodeEntity.getModelCodeSegments(entity)
 */
@OnlyIn(Dist.CLIENT)
public class GoblinEntityRenderer extends GoblinBodyRenderer {

    /** Sentinel yaw value passed to the render method during shoulder rendering. */
    public static final float SENTINEL = -420.69F;

    /** Current partial tick value (set during render). */
    float partialTick = 0.0F;
    /** Whether this entity is currently rendering in shoulder/carry mode. */
    boolean isShoulder = false;
    /** Whether the entity is being "picked up" (carry intro). */
    boolean isPickup   = false;
    /** Block light level at the entity's position. */
    float blockLight   = 0.0F;

    public GoblinEntityRenderer(EntityRendererProvider.Context ctx, GeoModel<GoblinEntity> model) {
        super(ctx, model);
    }

    // -- Texture resolution -----------------------------------------------------

    @Nullable
    @Override
    protected NpcSkinTexture getSkinTexture(GoblinEntity entity) {
        try {
            if (entity.level instanceof FakeWorld) return null;
        } catch (RuntimeException ignored) {}
        try {
            if (entity.isCarried()) return null;
        } catch (RuntimeException ignored) {}

        UUID skinUUID = null;
        // If carried, use the carrier's skin; otherwise use the entity's linked UUID
        UUID carrierId = entity.getCarrierUUID();
        if (carrierId != null) {
            skinUUID = carrierId;
        } else {
            skinUUID = entity.getOwnerUUID();
        }

        if (skinUUID == null) skinUUID = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getGameProfile().getId() : null;

        if (skinUUID == null) return NpcColorData.DEFAULT_TEXTURE;

        NpcSkinTexture cached = skinTextureCache.get(skinUUID);
        if (cached != null) return cached;

        // Load the skin asynchronously
        return NpcColorData.loadSkinTexture(skinUUID, entity.level);
    }

    // -- Main render override ---------------------------------------------------

    @Override
    public void render(GoblinEntity entity, float yaw, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light) {
        this.partialTick = partialTick;
        this.isShoulder  = (yaw == SENTINEL && entity.getAnimState() == AnimState.SHOULDER_IDLE);
        this.isPickup    = (yaw == SENTINEL && entity.getAnimState() == AnimState.PICK_UP);

        Minecraft mc = Minecraft.getInstance();

        // If yaw == SENTINEL and state is SHOULDER_IDLE - caller is shoulder handler, skip normal render
        if (yaw == SENTINEL && entity.getAnimState() == AnimState.SHOULDER_IDLE) return;
        if (yaw == SENTINEL && entity.getAnimState() == AnimState.PICK_UP)       return;

        // Read block light for body tint
        blockLight = (float) entity.level.getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK, entity.blockPosition());

        // First-person: suppress render if this is the local player's goblin in THROW/START_THROWING
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON && mc.player != null) {
            UUID localId = mc.player.getGameProfile().getId();
            UUID ownerId = entity.getOwnerUUID();
            if (localId.equals(ownerId)) {
                AnimState state = entity.getAnimState();
                if (isSexRenderState(state) && entity.isSexModeActive()) {
                    if (!entity.isSexModeActive()) return; // first-person eye position
                }
            }
        }

        // Partner yaw alignment
        if (entity.isSexModeActive()) {
            UUID partnerId = entity.getPartnerUUID();
            if (partnerId != null) {
                Player partner = entity.level.getPlayerByUUID(partnerId);
                if (partner != null) {
                    entity.yHeadRot = partner.getYRot();
                    entity.yBodyRot = partner.getYRot();
                }
            }
        }

        super.render(entity, yaw, partialTick, ps, buf, light);
        NpcColorData.applyOutlineTinting(entity, partialTick);
    }

    // -- Bone overrides ---------------------------------------------------------

    @Override
    public void renderRecursively(PoseStack ps, GoblinEntity entity, GeoBone bone,
                                   net.minecraft.client.renderer.RenderType renderType,
                                   MultiBufferSource buf, com.mojang.blaze3d.vertex.VertexConsumer vc,
                                   boolean isReRender, float partialTick,
                                   int light, int overlay, float r, float g, float b, float a) {
        // Physics-only: hide when carried
        if (entity.isCarried() && PHYSICS_ONLY_BONES.contains(bone.getName())) return;

        // Apply model-code driven color to specific bones
        Vec3 colorVec = computeBoneColor(entity, bone.getName());
        if (colorVec != null) {
            r = (float) colorVec.x / 255.0F;
            g = (float) colorVec.y / 255.0F;
            b = (float) colorVec.z / 255.0F;
        }

        // Crown visibility control
        if (bone.getName().contains("crown")) {
            setCrownVisibility(entity, bone);
        }

        super.renderRecursively(ps, entity, bone, renderType, buf, vc,
                isReRender, partialTick, light, overlay, r, g, b, a);
    }

    // -- Static helpers ---------------------------------------------------------

    /**
     * Forces a model-cache refresh for all goblin entities.
     * Called when the model code changes (mirrors dy.c() in original).
     */
    public static void preloadAll() {
        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
                if (npc instanceof GoblinEntity goblin) {
                    goblin.getAnimatableInstanceCache().invalidateCache();
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Renders a GoblinEntity as if it were at the given player's position.
     * Used by the shoulder-render system. Mirrors the static a(em, float) method.
     */
    public static void renderForPlayer(GoblinEntity entity, Player player,
                                        double dx, double dy, double dz, float pt) {
        KoboldShoulderRenderHandler.renderAsPlayer((PlayerKoboldEntity)(Object)entity, player, dx, dy, dz, pt);
    }

    /**
     * Static render-for-preload utility (mirrors dy.a(em, float) in original).
     * Invoked via RenderManager during preload in ClientProxy.
     */
    public static void renderAtSentinel(GoblinEntity entity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0,
                SENTINEL, partialTick, new PoseStack(),
                mc.renderBuffers().bufferSource(),
                mc.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick));
    }

    // -- Helpers ----------------------------------------------------------------

    /**
     * Returns true for AnimState values that require special render handling
     * when the goblin is involved in a sex animation (mirrors dy.a(em,fp) check).
     */
    private boolean isSexRenderState(AnimState state) {
        return state == AnimState.START_THROWING
            || state == AnimState.PICK_UP
            || state == AnimState.CATCH
            || state == AnimState.THROWN
            || state == AnimState.SHOULDER_IDLE;
    }

    /**
     * Computes the per-bone color from the model-code segments.
     * Mirrors the original a(String) - Vec3i method in dy.
     *
     * Segments:
     *   [0][1][2][3] = ear type / ear side A / ear side B
     *   [5] = hair color index   - g5 enum
     *   [6] = body color index   - by enum  - segment for bone tinting
     *   [7] = eye color index    - eh enum
     *   [8] = eye color 2        - eh enum (eyeColor / eyeColor2 bones)
     *   [9] = crown setting
     */
    @Nullable
    private Vec3 computeBoneColor(GoblinEntity entity, String boneName) {
        String[] seg = NpcModelCodeEntity.getModelCodeSegments(entity);
        if (seg == null || seg.length < 8) return null;

        if (boneName.contains("band")) return new Vec3(255, 255, 255);
        if (boneName.contains("eyeColor")) return eyeColorVec(seg[8]);
        if (boneName.contains("eyeColor2")) return eyeColorVec(seg[8]);
        if (boneName.contains("hair") || EYELASH_BONES.contains(boneName)) return hairColorVec(seg[6]);
        if (boneName.contains("variant") || boneName.contains("boob")) return bodyColorVec(seg[7]);
        if (GoblinBodyRenderer.TINTABLE_BONES.contains(boneName)) return bodyColorVec(seg[7]);
        if (boneName.equals("earL")) return null; // handled by a(bone, seg[0], seg[1], seg[3])
        if (boneName.equals("earR")) return null; // handled by a(bone, seg[0], seg[2], seg[4])
        return null;
    }

    private static java.util.HashSet<String> EYELASH_BONES = new java.util.HashSet<>(
            java.util.Arrays.asList("lashR", "lashL", "closedR", "closedL", "browL", "browR"));

    private Vec3 eyeColorVec(String segment) {
        try {
            return EyeColor.values()[Integer.parseInt(segment)].getColorVec();
        } catch (Exception e) { return new Vec3(255, 255, 255); }
    }

    private Vec3 hairColorVec(String segment) {
        try {
            return HairColor.values()[Integer.parseInt(segment)].getColorVec();
        } catch (Exception e) { return new Vec3(255, 255, 255); }
    }

    private Vec3 bodyColorVec(String segment) {
        try {
            return GoblinColor.values()[Integer.parseInt(segment)].getColorVec();
        } catch (Exception e) { return new Vec3(255, 255, 255); }
    }

    private void setCrownVisibility(GoblinEntity entity, GeoBone bone) {
        try {
            String[] seg = NpcModelCodeEntity.getModelCodeSegments(entity);
            if (seg == null || seg.length < 10) { bone.setHidden(true); return; }
            int crownSetting = Integer.parseInt(seg[9]);
            bone.setHidden(crownSetting == 0);
        } catch (Exception e) {
            bone.setHidden(true);
        }
    }
}
