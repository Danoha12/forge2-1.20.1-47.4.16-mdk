package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.client.model.IBoneFilter;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * BaseNpcRenderer (d_) - Ported from 1.12.2 to 1.20.1.
 *
 * Abstract base for all sexmod NPC entity renderers.
 * Extends {@link GeoEntityRenderer} with:
 *
 *  1. Skin texture loading from player UUID (downloads and caches as DynamicTexture).
 *  2. Per-bone colour support via {@link #getBoneColor(String)} / {@link IBoneFilter}.
 *  3. Hidden bone set (from customization manager / NPC's configured set).
 *  4. Leash (lead) rendering in vanilla style.
 *  5. Name tag rendering with optional colour prefix.
 *  6. Visibility checking (line-of-sight AABB test for third-person camera).
 *  7. Bra-string line rendering at "braString*" bone attachment points.
 *  8. Elytra glide rotation pass-through.
 *
 * Key 1.12.2 - 1.20.1 changes:
 *   - {@code GeoEntityRenderer<T>} base class API (GeckoLib3 - GeckoLib4):
 *       setLivingAnimations - setCustomAnimations
 *       renderEarly/renderLate unchanged signatures
 *   - {@code GlStateManager} - {@code PoseStack} + {@code RenderSystem}
 *   - {@code Tessellator.getInstance().getBuilder()} - {@code Tesselator.getInstance().getBuilder()}
 *   - {@code BufferBuilder} vertex API: func_181662_b(x,y,z) - vertex(x,y,z)
 *                                        func_187315_a(u,v)  - uv(u,v)
 *                                        func_181666_a(r,g,b,a) - color(r,g,b,a)
 *                                        func_181675_d() - endVertex()
 *   - {@code World} - {@code Level}, {@code EntityLivingBase} - {@code LivingEntity}
 *   - {@code DynamicTexture} constructor unchanged
 *   - {@code func_147906_a(entity,label,x,y,z,dist)} - renderNameTag override
 *   - {@code gj} (FakeLevel) - {@code FakeLevel}
 *   - Skin URL helper: {@code y.a(uuid)} - {@code PlayerSkinUtil.getSkinImage(uuid)}
 *
 * Subclasses:
 *   NpcColoredRenderer - adds abstract per-bone colour
 *   AllieRenderer      - scale-in spawn animation
 *   PlayerKoboldRenderer, MangleLieRenderer, NpcSubtypeRenderer - trivial
 *
 * Field mapping:
 *   e   - LINE_TEXTURE
 *   m   - SCALE_FACTOR (1.5f)
 *   c   - shadowRadius
 *   j   - entityRef
 *   i   - mc (Minecraft instance)
 *   l   - skinCache (HashMap<UUID, ResourceLocation>)
 *   f   - skinBaseColor
 *   o   - skinSecondaryColor
 *   h   - hasSkinLoadError
 *   p   - hiddenBoneSet (HashSet<String>)
 *   k/b/d - overlay RGB channels
 *   a   - bowAnimProgress
 *   g   - savedMatrix
 *   q   - currentBone
 */
@OnlyIn(Dist.CLIENT)
public abstract class BaseNpcRenderer<T extends BaseNpcEntity>
        extends GeoEntityRenderer<T> implements IBoneFilter {

    // -- Constants -------------------------------------------------------------

    protected static final ResourceLocation LINE_TEXTURE =
            new ResourceLocation("sexmod", "textures/line.png");
    protected static final float SCALE_FACTOR = 1.5f;

    // -- State -----------------------------------------------------------------

    /** Shadow / leash radius supplied by subclasses. */
    protected double shadowRadius;

    /** Entity being rendered this frame. */
    protected T entityRef;

    /** Minecraft singleton. */
    protected static final Minecraft mc = Minecraft.getInstance();

    /** Skin texture cache: ownerUUID - ResourceLocation of DynamicTexture. */
    protected static final HashMap<UUID, ResourceLocation> skinCache = new HashMap<>();

    /** Skin default colours (used if skin loading fails). */
    Color skinBaseColor      = new Color(245, 199, 165);
    Color skinSecondaryColor = new Color(245, 157, 169);
    boolean hasSkinLoadError = false;

    /** Bones excluded from rendering (set per-frame from CustomModelManager). */
    protected final HashSet<String> hiddenBoneSet = new HashSet<>();

    /** Current bone being rendered (for colour lookup). */
    protected GeoBone currentBone = null;

    /** Accumulated bow-animation progress. */
    protected float bowAnimProgress = 0.0f;

    // -- Constructor -----------------------------------------------------------

    protected BaseNpcRenderer(EntityRendererProvider.Context context,
                               GeoModel<T> model,
                               double shadowRadius) {
        super(context, model);
        this.shadowRadius = shadowRadius;
    }

    // -- Skin texture loading --------------------------------------------------

    /**
     * Return the ResourceLocation for the given owner UUID's skin,
     * downloading and caching it as a DynamicTexture if necessary.
     * Corresponds to {@code d(T)} in 1.12.2.
     */
    protected ResourceLocation getSkinTexture(T entity) throws IOException {
        UUID ownerUUID;
        if (entity.level() instanceof FakeLevel || entity.getOwnerUUID() == null) {
            ownerUUID = mc.getUser().getGameProfile().getId();
        } else {
            ownerUUID = entity.getOwnerUUID();
        }

        ResourceLocation cached = skinCache.get(ownerUUID);
        if (cached != null) return cached;

        return loadAndCacheSkin(ownerUUID);
    }

    private ResourceLocation loadAndCacheSkin(UUID uuid) throws IOException {
        BufferedImage image;
        try {
            image = PlayerSkinUtil.getSkinImage(uuid);
            Graphics g = image.getGraphics();
            g.setColor(skinBaseColor);
            g.fillRect(0, 0, 4, 3);
            g.setColor(skinSecondaryColor);
            g.fillRect(4, 0, 3, 3);
        } catch (Exception e) {
            hasSkinLoadError = true;
            image = ImageIO.read(
                mc.getResourceManager()
                  .getResource(new ResourceLocation("sexmod", "textures/player/steve.png"))
                  .orElseThrow().open());
        }

        ResourceLocation loc = mc.getTextureManager().register(
                "player" + uuid,
                new DynamicTexture(image));
        skinCache.put(uuid, loc);
        return loc;
    }

    // -- Hidden bone set -------------------------------------------------------

    /**
     * Build the hidden-bone set from either the global customization manager
     * or the entity's own Y() set.
     * Corresponds to {@code a(Boolean, boolean)} in 1.12.2.
     */
    protected HashSet<String> buildHiddenBoneSet(boolean useFrozenSet, boolean allowNsfw) {
        if (ClientProxy.IS_PRELOADING) return new HashSet<>();

        Set<String> rawSet = useFrozenSet
                ? CustomModelManager.getDefaultHiddenBones()
                : entityRef.getHiddenBoneNames();

        HashSet<String> result = new HashSet<>();
        for (String boneName : rawSet) {
            CustomModelManager.BoneEntry entry = CustomModelManager.getBoneEntry(boneName);
            if (entry == null) continue;
            if (!entry.isNsfw() && allowNsfw) continue;
            result.addAll(entry.getBoneNames());
        }
        return result;
    }

    // -- Bone colour (override in coloured subclasses) -------------------------

    /**
     * Return the RGB colour for {@code boneName} (components 0-255).
     * Base returns white (255, 255, 255).
     */
    protected net.minecraft.core.Vec3i getBoneColor(String boneName) {
        return new net.minecraft.core.Vec3i(255, 255, 255);
    }

    // -- Per-bone hooks for subclasses -----------------------------------------

    /** Called before each bone is rendered. Override for physics / IK. */
    protected void onBoneProcess(String boneName, GeoBone bone) {}

    /** Called for UV-offset hooks (mouth open, blowOpening, etc.). */
    protected void onBoneUvOffset(String boneName, GeoBone bone) {}

    /**
     * Called when the "offhand" bone is encountered and an off-hand item
     * should be rendered there. Override in inventory-bearing renderers.
     */
    protected void renderItemAtBone(PoseStack poseStack, MultiBufferSource buffers,
                                     GeoBone bone, T entity, int light) {}

    /** Resolve which ItemStack to render in the weapon slot. */
    @Nullable
    protected ItemStack resolveHeldItem(@Nullable ItemStack defaultItem) {
        return defaultItem;
    }

    // -- GeckoLib4 render pipeline ---------------------------------------------

    @Override
    public void render(T entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        entityRef = entity;

        // Visibility check (LOS from player camera)
        if (mc.player != null && !entity.isFrozen() && entity.requiresLos()) {
            if (!isVisible(entity, mc.player)) return;
        }

        // Refresh hidden bone set
        hiddenBoneSet.clear();
        hiddenBoneSet.addAll(buildHiddenBoneSet(entity.isFrozen(), entity.getAnimAge() == 0));

        super.render(entity, yaw, partialTick, poseStack, buffers, light);

        // Post-render: update bone matrix data for selectables
        updateBoneMatrices(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone,
                                   RenderType renderType, MultiBufferSource bufferSource,
                                   VertexConsumer buffer, boolean isReRender,
                                   float partialTick, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        // Skip rendering for fake/preview levels if needed
        if (entityRef != null && entityRef.level() instanceof FakeLevel) return;

        String name = bone.getName();
        currentBone = bone;

        // Per-bone callbacks
        onBoneProcess(name, bone);
        onBoneUvOffset(name, bone);

        // Apply hidden-bone flag
        if (hiddenBoneSet.contains(name)) {
            bone.setHidden(true);
        }

        // Bone colour override
        net.minecraft.core.Vec3i color = getBoneColor(name);
        float r = color.getX() / 255.0f;
        float g = color.getY() / 255.0f;
        float b = color.getZ() / 255.0f;

        super.renderRecursively(poseStack, animatable, bone, renderType,
                bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, r, g, b, alpha);
    }

    // -- Visibility (line-of-sight) check --------------------------------------

    /**
     * Returns {@code true} if the NPC should be rendered from the player's
     * camera position.  Performs an 8-corner AABB ray-cast against solid
     * blocks.  Corresponds to {@code a(T, EntityPlayer)} in 1.12.2.
     */
    protected boolean isVisible(T npc, Player player) {
        // PlayerKoboldEntity always renders
        if (npc instanceof PlayerKoboldEntity) return true;

        // First-person: always render
        var settings = mc.options;
        if (settings.getCameraType().isFirstPerson()) return true;

        Vec3 npcPos = npc.position();
        float hw = npc.getBbWidth() * SCALE_FACTOR * 0.5f;
        float hh = npc.getBbHeight() * SCALE_FACTOR;

        Vec3 camPos = player.getEyePosition(1.0f);
        var level = npc.level();

        Vec3[] corners = new Vec3[] {
            npcPos.add(-hw, 0, -hw), npcPos.add(-hw, 0, hw),
            npcPos.add( hw, 0, -hw), npcPos.add( hw, 0, hw),
            npcPos.add(-hw, hh,-hw), npcPos.add(-hw, hh, hw),
            npcPos.add( hw, hh,-hw), npcPos.add( hw, hh, hw)
        };

        for (Vec3 corner : corners) {
            var clip = level.clip(new net.minecraft.world.level.ClipContext(
                    camPos, corner,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player));
            if (clip.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;

            BlockPos hitPos = clip.getBlockPos();
            var state = level.getBlockState(hitPos);
            if (state.getRenderShape() == RenderShape.INVISIBLE) return true;
            if (!state.isSolidRender(level, hitPos)) return true;
        }
        return false;
    }

    // -- Name tag --------------------------------------------------------------

    @Override
    protected void renderNameTag(T entity, net.minecraft.network.chat.Component name,
                                  PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.isFrozen()) return;
        if (entity.getAnimState() != null && entity.getAnimState().isHideNameTag()) return;
        if (mc.getCameraEntity() == null) return;

        super.renderNameTag(entity, name, poseStack, buffers, light);
    }

    // -- Bone matrix update ----------------------------------------------------

    /**
     * After rendering, walk all selectable bone names and update the entity's
     * stored world-space Vec3 for each (used by SelectableEntityPart).
     * Corresponds to {@code void a(T)} in 1.12.2.
     */
    protected void updateBoneMatrices(T entity) {
        List<String> boneNames = new ArrayList<>(SelectableEntityPart.BONE_NAMES);
        boneNames.addAll(entity.getExtraBoneNames());

        for (String boneName : boneNames) {
            // GeckoLib4: request the bone matrix from the cached model
            software.bernie.geckolib.util.GeckoLibUtil.getGeoModel(entity)
                .ifPresent(model -> {
                    GeoBone bone = model.getBone(boneName).orElse(null);
                    if (bone == null) return;
                    // Bone world position from pivot
                    Vec3 pos = new Vec3(
                            -bone.getWorldPosition().x(),
                             bone.getWorldPosition().y(),
                            -bone.getWorldPosition().z());
                    entity.updateBonePosition(boneName, pos);
                });
        }
    }

    // -- Leash (lead) rendering ------------------------------------------------

    /**
     * Draw a Minecraft-style leash between the NPC and the entity it is
     * tethered to.  Corresponds to {@code a(em, double, double, double, float)}
     * in 1.12.2.
     */
    @Override
    protected void renderLeash(T entity, float partialTick, PoseStack poseStack,
                                MultiBufferSource buffers, LivingEntity leashHolder) {
        // Delegate to vanilla-style leash drawing
        super.renderLeash(entity, partialTick, poseStack, buffers, leashHolder);
    }

    // -- Bra-string line helper ------------------------------------------------

    /**
     * Draw a single line segment between two named bones.
     * Used by Jenny / Allie renderers that draw clothing strings.
     * Corresponds to static {@code a(BufferBuilder, Tessellator, em, String, String, ...)}
     * in 1.12.2.
     */
    protected static void drawBoneSegment(PoseStack poseStack,
                                           MultiBufferSource buffers,
                                           BaseNpcEntity entity,
                                           String boneA, String boneB,
                                           float r, float g, float b, float alpha) {
        Vec3 posA = entity.getBonePosition(boneA);
        Vec3 posB = entity.getBonePosition(boneB);
        if (posA == null || posB == null) return;

        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();

        vc.vertex(mat, (float) posA.x, (float) posA.y, (float) posA.z)
          .color(r, g, b, alpha)
          .normal(poseStack.last().normal(), 0, 1, 0)
          .endVertex();
        vc.vertex(mat, (float) posB.x, (float) posB.y, (float) posB.z)
          .color(r, g, b, alpha)
          .normal(poseStack.last().normal(), 0, 1, 0)
          .endVertex();
    }

    /**
     * Draw all bra-string segments for the given entity.
     * Corresponds to the static overload of {@code a(Tessellator, BufferBuilder, em, f7, float)}
     * in 1.12.2.
     */
    protected static void drawBraStrings(PoseStack poseStack,
                                          MultiBufferSource buffers,
                                          BaseNpcEntity entity,
                                          RgbColor color, float alpha) {
        float r = color.r / 255.0f, g = color.g / 255.0f, b = color.b / 255.0f;
        String[][] segments = {
            {"braStringMidStartR","braStringMidMid1R"},{"braStringMidMid1R","braStringMidMid2R"},
            {"braStringMidMid2R","braStringMidMid3R"},{"braStringMidMid3R","braStringMidEndR"},
            {"braStringMidEndR","braStringBackR"},{"braStringBackR","braStringRightEndR"},
            {"braStringRightEndR","braStringRightStartR"},{"braStringRightR","braStringRightL"},
            {"braStringMidStartL","braStringMidMid1L"},{"braStringMidMid1L","braStringMidMid2L"},
            {"braStringMidMid2L","braStringMidMid3L"},{"braStringMidMid3L","braStringMidEndL"},
            {"braStringMidEndL","braStringBackL"},{"braStringBackL","braStringLeftEndL"},
            {"braStringLeftEndL","braStringLeftStartL"}
        };
        for (String[] seg : segments) {
            drawBoneSegment(poseStack, buffers, entity, seg[0], seg[1], r, g, b, alpha);
        }
    }

    // -- IBoneFilter -----------------------------------------------------------

    @Override
    public boolean isHidden(String boneName) {
        return hiddenBoneSet.contains(boneName);
    }
}
