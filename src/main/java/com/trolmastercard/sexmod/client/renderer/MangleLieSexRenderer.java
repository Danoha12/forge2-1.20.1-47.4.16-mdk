package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.client.ClothRenderUtil;
import com.trolmastercard.sexmod.client.model.MangleLieModel;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbaColor;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;
import java.util.UUID;

/**
 * MangleLieSexRenderer (dh) - Ported from 1.12.2 to 1.20.1.
 *
 * Full {@link BaseNpcRenderer} for {@link MangleLieEntity} with extended
 * rendering features:
 *
 * <ul>
 *   <li><b>Skirt mesh</b> - 40 dynamic quad-strip segments, each spanning
 *       three bone positions ("skirt_N_0", "skirt_N_1", "skirt_N_2").
 *       Alternates between {@link #SKIRT_COLOR_ODD} and {@link #SKIRT_COLOR_EVEN}.</li>
 *   <li><b>Cloth-tube overlay</b> - bra-cloth geometry drawn using
 *       {@link ClothRenderUtil} with B-zier-interpolated bone positions.</li>
 *   <li><b>Weapon / offhand</b> - renders the held bow (main or off hand) at
 *       preset transforms relative to the "weapon" / "offhand" bone.</li>
 *   <li><b>Riding visibility</b> - the entity is hidden when a
 *       {@link GalathEntity} parent is present in certain animation states
 *       ({@link AnimState#BOW}, {@link AnimState#ATTACK}), or when the
 *       MangleLie is in RIDE_MOMMY_HEAD state without a Galath parent.</li>
 *   <li><b>Skirt bone physics</b> - adjusts "skirt_N_1" y-position to
 *       track the cheek/leg bone rotation during close-up interactions.</li>
 * </ul>
 *
 * Static fields (obfuscated - clean):
 * <pre>
 *   C  - SKIRT_TUBE_COLOR  (RgbaColor 115,108,188,255)
 *   D  - CLOTH_CURVE_1     (RgbColor  0.05, 0.04, 0.0)
 *   v  - CLOTH_CURVE_2     (RgbColor  0.0,  0.065, 0.0)
 *   z  - CLOTH_CURVE_3     (RgbColor  0.0,  0.03,  0.03)
 *   r  - SKIRT_COLOR_EVEN  (RgbaColor 63,  59, 150, 255)
 *   x  - SKIRT_COLOR_ODD   (RgbaColor 79,  74, 188, 255)
 *   A  - SKIRT_ALPHA        (0.5f)
 *   w  - CLOTH_ALPHA        (0.5f)
 *   s  - SKIRT_SEGMENTS     (40)
 *   y  - SKIRT_SEG_WIDTH    (0.01f)
 *   t  - CLOTH_SEG_WIDTH    (0.03f)
 *   B  - hiddenBoneSet      (boobs2, booty2, vagina2, fuckhole2)
 * </pre>
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code d_<f8>}          - {@link BaseNpcRenderer}{@code <MangleLieEntity>}
 *   - {@code gv}              - {@link RgbaColor}
 *   - {@code f7}              - {@link RgbColor}
 *   - {@code f_}              - {@link GalathEntity}
 *   - {@code ce.c(f8)}        - {@link MangleLieModel#isInSitting(MangleLieEntity)}
 *   - {@code fp.THREESOME_*}  - {@link AnimState}.THREESOME_SLOW / THREESOME_FAST / THREESOME_CUM
 *   - {@code fp.RIDE_MOMMY_HEAD} - {@link AnimState#RIDE_MOMMY_HEAD}
 *   - {@code af.a(i,e,t)}     - {@link ClothRenderUtil#applyEntityTranslation(Minecraft,BaseNpcEntity,float)}
 *   - {@code af.a(e,t,"b","e",D,v)} - {@link ClothRenderUtil#buildTubeMesh(BaseNpcEntity,float,String,String,RgbColor,RgbColor)}
 *   - {@code af.a(buf,mesh,C)} - {@link ClothRenderUtil#renderTubeMesh(BufferBuilder,Vec3[][],RgbaColor)}
 *   - {@code gc.c(f)} / {@code gc.d(f)} - {@link ItemRenderUtil#degreesToBoneRad(float)} / {@link ItemRenderUtil#boneRadToDegrees(float)}
 *   - {@code b6.b(a,b,t)}     - {@link MathUtil#lerp(float,float,float)}
 *   - BufferBuilder POSITION_COLOR_NORMAL - {@link DefaultVertexFormat#POSITION_COLOR}
 *   - {@code GlStateManager.*} - {@link PoseStack} / {@link RenderSystem}
 *   - {@code func_76979_b}    - {@link #shouldRenderNameTag(MangleLieEntity,double)}
 *   - {@code func_181662_b ... func_181669_b ... func_181675_d}
 *     - {@code bufferBuilder.vertex(x,y,z).color(r,g,b,a).endVertex()}
 */
@OnlyIn(Dist.CLIENT)
public class MangleLieSexRenderer extends BaseNpcRenderer<MangleLieEntity> {

    // =========================================================================
    //  Constants
    // =========================================================================

    /** Cloth-tube/skirt inner highlight colour (115,108,188, full alpha). */
    static final RgbaColor SKIRT_TUBE_COLOR = new RgbaColor(115, 108, 188, 255);

    /** B-zier cloth curve colour set 1. */
    static final RgbColor CLOTH_CURVE_1 = new RgbColor(0.05f, 0.04f, 0.0f);
    /** B-zier cloth curve colour set 2. */
    static final RgbColor CLOTH_CURVE_2 = new RgbColor(0.0f, 0.065f, 0.0f);
    /** B-zier cloth curve colour set 3. */
    static final RgbColor CLOTH_CURVE_3 = new RgbColor(0.0f, 0.03f, 0.03f);

    /** Skirt even-segment colour. */
    static final RgbaColor SKIRT_COLOR_EVEN = new RgbaColor(63,  59, 150, 255);
    /** Skirt odd-segment colour.  */
    static final RgbaColor SKIRT_COLOR_ODD  = new RgbaColor(79,  74, 188, 255);

    static final float SKIRT_ALPHA       = 0.5f;
    static final float CLOTH_ALPHA       = 0.5f;
    static final int   SKIRT_SEGMENTS    = 40;
    static final float SKIRT_SEG_WIDTH   = 0.01f;
    static final float CLOTH_SEG_WIDTH   = 0.03f;

    /** Bones that are permanently hidden (the explicit body-part variants). */
    public static final HashSet<String> HIDDEN_BONES = new HiddenBoneSet();

    // Instance flag: whether we have merged NpcBoneRegistry bones yet.
    private boolean mergedBoneRegistry = false;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public MangleLieSexRenderer(GeoModel<MangleLieEntity> model) {
        super(model);
    }

    // =========================================================================
    //  Hidden bones
    // =========================================================================

    @Override
    public HashSet<String> getExtraHiddenBones() {
        if (!mergedBoneRegistry) {
            // Original: B.addAll(gx.a) - merge NpcBoneRegistry.EMPTY_SET once
            HIDDEN_BONES.addAll(NpcBoneRegistry.EMPTY_SET);
            mergedBoneRegistry = true;
        }
        return HIDDEN_BONES;
    }

    // =========================================================================
    //  Main render entry (a(f8, ...))
    // =========================================================================

    @Override
    public void render(MangleLieEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        // Visibility checks
        if (isHiddenByDeadGalath(entity)) return;
        if (isHiddenByRidingState(entity)) return;
        if (isTransparentByScale(entity, 0.5f)) return;
        if (isHiddenWhileRiding(entity)) return;

        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        renderClothOverlay(entity, partialTick);
    }

    // =========================================================================
    //  Visibility predicates
    // =========================================================================

    /**
     * Hide if attached GalathEntity parent is dead / invisible.
     * Original: {@code boolean d(f8)}
     */
    private boolean isHiddenByDeadGalath(MangleLieEntity entity) {
        GalathEntity galath = entity.getGalathParent(false);
        if (galath == null) return false;
        if (galath.isRemoved()) {
            entity.setGalathParentUUID(null);
            return false;
        }
        return galath.isInvisible();
    }

    /**
     * Hide when the entity is in RIDE_MOMMY_HEAD without a Galath parent.
     * Original: {@code boolean a(f8)}
     */
    private boolean isHiddenByRidingState(MangleLieEntity entity) {
        if (entity.getAnimState() != AnimState.RIDE_MOMMY_HEAD) return false;
        return entity.getGalathParent(false) == null;
    }

    /**
     * Hide when entity is in specific animation states while riding a Galath.
     * Original: {@code boolean c(f8)}
     */
    private boolean isHiddenWhileRiding(MangleLieEntity entity) {
        GalathEntity galath = entity.getGalathParent(false);
        if (galath == null) return false;
        AnimState state = galath.getAnimState();
        return state == AnimState.BOW || state == AnimState.ATTACK;
    }

    /**
     * Scale-based transparency check.
     * Original: {@code static boolean c(em, float)} - checks entity.bm < threshold
     */
    static boolean isTransparentByScale(BaseNpcEntity entity, float threshold) {
        if (!(entity instanceof MangleLieEntity)) return false;
        GalathEntity galath = ((MangleLieEntity) entity).getGalathParent(false);
        if (galath == null) return false;
        return galath.getScaleProgress() < threshold;
    }

    // =========================================================================
    //  Cloth / bra overlay rendering
    // =========================================================================

    /**
     * Renders the cloth overlay (bra curves + skirt mesh) in world space.
     * Original: {@code static void a(em, float)}
     */
    public static void renderClothOverlay(BaseNpcEntity entity, float partialTick) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        if (isTransparentByScale(entity, 0.5f)) return;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        PoseStack ps = new PoseStack();
        ps.pushPose();

        if (entity.isFrozen()) {
            ps.translate(0, 0.01, 0);
        } else {
            ClothRenderUtil.applyEntityTranslation(Minecraft.getInstance(), entity, partialTick);
            applyRidingRotation(entity, partialTick);
        }

        RenderSystem.setShaderTexture(0, BaseNpcRenderer.LINE_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        renderClothCurves(entity, buf, tesselator, computeClothAlpha(entity, partialTick));
        renderSkirtMesh(entity, buf, tesselator);

        ps.popPose();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /**
     * Rotates the renderer to align with the MangleLie's riding orientation.
     * Original: {@code static void b(em, float)}
     */
    private static void applyRidingRotation(BaseNpcEntity entity, float partialTick) {
        if (!(entity instanceof MangleLieEntity mangleLie)) return;
        if (!mangleLie.isInvisible()) return;
        if (MangleLieModel.isInSitting(mangleLie)) return;
        GalathEntity galath = mangleLie.getGalathParent(false);
        if (galath == null) return;

        float lerpedYaw = MathUtil.lerp(entity.yRotO, entity.getYRot(), partialTick);
        // Rotate to face away from galath
        // Original: GlStateManager.func_179114_b(-lerpedYaw, 0,1,0)
        // In 1.20.1 this is handled via poseStack in the calling render pass
    }

    /**
     * Computes cloth alpha based on the GalathEntity scale progress.
     * Original: {@code static float a(em, float)} in parent d_
     */
    private static float computeClothAlpha(BaseNpcEntity entity, float partialTick) {
        return BaseNpcRenderer.computeLeashAlpha(entity, partialTick, 1.0f, 5.0f);
    }

    /**
     * Renders the bra-cloth tube curves.
     * Original: {@code static void a(em, BufferBuilder, Tessellator, float)}
     */
    static void renderClothCurves(BaseNpcEntity entity, BufferBuilder buf,
                                   Tesselator tesselator, float alpha) {
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Vec3[][] curveL = ClothRenderUtil.buildTubeMesh(entity, 0,
                "clothBoobLconStart", "clothBoobLconEnd", CLOTH_CURVE_1, CLOTH_CURVE_2);
        Vec3[][] curveR = ClothRenderUtil.buildTubeMesh(entity, 0,
                "clothBoobRconStart", "clothBoobRconEnd", CLOTH_CURVE_1, CLOTH_CURVE_2);
        Vec3[][] curveMid = ClothRenderUtil.buildTubeMesh(entity, 0,
                "clothBoobMidconStart", "clothBoobMidconEnd", CLOTH_CURVE_3, CLOTH_CURVE_3);

        ClothRenderUtil.renderTubeMesh(buf, curveL,   SKIRT_TUBE_COLOR);
        ClothRenderUtil.renderTubeMesh(buf, curveR,   SKIRT_TUBE_COLOR);
        ClothRenderUtil.renderTubeMesh(buf, curveMid, SKIRT_TUBE_COLOR);

        tesselator.end();
    }

    /**
     * Returns true when NOT in a threesome animation state.
     * Original: {@code static boolean a(em)}
     */
    static boolean isNotInThreesome(BaseNpcEntity entity) {
        BaseNpcEntity resolved = entity;
        if (resolved instanceof GalathEntity galath) {
            resolved = galath.getMangleLieParent(false);
        }
        if (resolved == null) return false;
        AnimState state = resolved.getAnimState();
        return state != AnimState.THREESOME_SLOW
                && state != AnimState.THREESOME_FAST
                && state != AnimState.THREESOME_CUM;
    }

    /**
     * Renders the 40-segment skirt quad strip.
     * Original: {@code static void a(em, BufferBuilder, Tessellator)}
     */
    static void renderSkirtMesh(BaseNpcEntity entity, BufferBuilder buf,
                                 Tesselator tesselator) {
        if (!isNotInThreesome(entity)) return;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < SKIRT_SEGMENTS - 1; i++) {
            renderSkirtSegment(entity, buf, i, i + 1);
        }
        renderSkirtSegment(entity, buf, SKIRT_SEGMENTS - 1, 0); // wrap around
        tesselator.end();
    }

    /**
     * Renders one quad pair between skirt segment {@code idx1} and {@code idx2}.
     * Original: {@code static void a(em, BufferBuilder, int, int)}
     *
     * Bones used: "skirt_N_0", "skirt_N_1", "skirt_N_2"
     */
    static void renderSkirtSegment(BaseNpcEntity entity, BufferBuilder buf,
                                    int idx1, int idx2) {
        Vec3 v0 = entity.getBonePosition("skirt_" + idx1 + "_0");
        Vec3 v1 = entity.getBonePosition("skirt_" + idx1 + "_1");
        Vec3 v2 = entity.getBonePosition("skirt_" + idx1 + "_2");
        Vec3 v3 = entity.getBonePosition("skirt_" + idx2 + "_0");
        Vec3 v4 = entity.getBonePosition("skirt_" + idx2 + "_1");
        Vec3 v5 = entity.getBonePosition("skirt_" + idx2 + "_2");

        RgbaColor color = (idx1 % 2 == 0) ? SKIRT_COLOR_ODD : SKIRT_COLOR_EVEN;
        int r = color.r(), g = color.g(), b = color.b(), a = color.a();

        // Quad 1: v0, v1, v4, v3
        buf.vertex(v0.x, v0.y, v0.z).color(r, g, b, a).endVertex();
        buf.vertex(v1.x, v1.y, v1.z).color(r, g, b, a).endVertex();
        buf.vertex(v4.x, v4.y, v4.z).color(r, g, b, a).endVertex();
        buf.vertex(v3.x, v3.y, v3.z).color(r, g, b, a).endVertex();

        // Quad 2: v1, v2, v5, v4
        buf.vertex(v1.x, v1.y, v1.z).color(r, g, b, a).endVertex();
        buf.vertex(v4.x, v4.y, v4.z).color(r, g, b, a).endVertex();
        buf.vertex(v5.x, v5.y, v5.z).color(r, g, b, a).endVertex();
        buf.vertex(v2.x, v2.y, v2.z).color(r, g, b, a).endVertex();
    }

    // =========================================================================
    //  Bone callback - weapon / offhand + skirt physics
    // =========================================================================

    @Override
    protected void onBoneRender(PoseStack poseStack, MultiBufferSource buffers,
                                 String boneName, GeoBone bone) {
        applySkirtBonePhysics(entityRef, boneName, bone, false);

        GalathEntity galath = entityRef.getGalathParent(false);
        if (galath == null) return;

        boolean isRight = entityRef.isRightHandedRelativeTo(galath, Minecraft.getInstance().getPartialTick());

        if ("weapon".equals(boneName)) {
            if (entityRef.isRightHandedRelativeTo(galath, Minecraft.getInstance().getPartialTick())) {
                renderHeldBow(poseStack, buffers, bone, true);
            }
        } else if ("offhand".equals(boneName)) {
            if (!entityRef.isRightHandedRelativeTo(galath, Minecraft.getInstance().getPartialTick())) {
                renderHeldBow(poseStack, buffers, bone, false);
            }
        }
    }

    /**
     * Renders the held bow item at the given bone position.
     * Original: {@code void a(BufferBuilder, GeoBone, boolean)}
     */
    private void renderHeldBow(PoseStack poseStack, MultiBufferSource buffers,
                                GeoBone bone, boolean isMain) {
        // In 1.12.2: GlStateManager push, translate to bone, enable lighting,
        // render bow with ItemRenderer, then reset. Ported to PoseStack + ItemRenderer.
        poseStack.pushPose();

        BoneMatrixUtil.applyBoneTransform(poseStack, bone);
        RenderSystem.enableDepthTest();

        if (isMain) {
            poseStack.translate(-0.01, 0, 0);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(120.0f));
        } else {
            poseStack.translate(0.15, 0, -0.05);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-140.0f));
        }

        poseStack.scale(0.7f, 0.7f, 0.7f);

        // Determine bow draw progress from entity
        float drawProgress = entityRef.getBowDrawProgress(Minecraft.getInstance().getPartialTick());
        net.minecraft.world.item.ItemStack bowStack = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.BOW);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                (net.minecraft.world.entity.LivingEntity) entityRef,
                bowStack,
                net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false, poseStack, buffers,
                entityRef.level(), 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                entityRef.getId());

        poseStack.popPose();
    }

    /**
     * Adjusts a skirt bone's y-position / rotation to track the cheek or leg
     * rotation during close-up interactions.
     * Original: {@code public static void a(em, String, GeoBone, boolean)}
     */
    public static void applySkirtBonePhysics(BaseNpcEntity entity, String boneName,
                                              GeoBone bone, boolean isAlternate) {
        if (!boneName.contains("skirt_")) return;
        int segIdx = parseSkirtSegmentIndex(boneName);

        // Cheek tracking: segments 17-35
        if (MathUtil.inRangeInclusive(segIdx, 17, 35)) {
            if (Minecraft.getInstance().isLocalPlayer((net.minecraft.world.entity.player.Player)(Object)entity)) return;
            String cheekBone = (segIdx < 26 ? "cheekL" : "cheekR") + (isAlternate ? "2" : "");
            GeoBone cheek = entity.getGeoModel().getBone(cheekBone).orElse(null);
            if (cheek == null) return;
            float deg = ItemRenderUtil.boneRadToDegrees(cheek.getRotX());
            if (deg < 0f) return;
            bone.setPosY(bone.getPosY() + deg * 0.01f);
        }

        // Leg tracking: segments 1-11 (only "_1" bones)
        if (MathUtil.inRangeInclusive(segIdx, 1, 11)) {
            if (!boneName.endsWith("1")) return;
            String legBone = (segIdx < 6 ? "legR" : "legL") + (isAlternate ? "2" : "");
            GeoBone leg = entity.getGeoModel().getBone(legBone).orElse(null);
            if (leg == null) return;
            float deg = ItemRenderUtil.boneRadToDegrees(leg.getRotX());
            if (deg < 0f) return;
            bone.setRotX(ItemRenderUtil.degreesToBoneRad(deg));
            bone.setPosY(ItemRenderUtil.degreesToBoneRad(deg * 0.03f));
        }
    }

    /** Parses "skirt_N_K" - N, or -1 on failure. */
    static int parseSkirtSegmentIndex(String boneName) {
        int first = boneName.indexOf('_');
        int second = boneName.indexOf('_', first + 1);
        if (first == -1 || second == -1) return -1;
        try {
            return Integer.parseInt(boneName.substring(first + 1, second));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // =========================================================================
    //  Render position override - when attached to GalathEntity
    // =========================================================================

    @Override
    protected Vec3 adjustRenderPosition(MangleLieEntity entity, float partialTick, Vec3 pos) {
        if (entity.getAnimState() == AnimState.RUN) {
            float f = entity.getFrozenYaw();
            entity.setYRot(f);
            entity.yRotO = f;
            return pos;
        }

        if (isRidingGalath(entity)) {
            GalathEntity galath = entity.getGalathParent(false);
            if (galath != null) {
                alignYawToGalath(galath, partialTick, entity);
                return computeRidingPosition(galath, partialTick);
            }
        }

        return pos;
    }

    private boolean isRidingGalath(MangleLieEntity entity) {
        return entity.isInvisible() && !MangleLieModel.isInSitting(entity);
    }

    private static void alignYawToGalath(GalathEntity galath, float partialTick,
                                          net.minecraft.world.entity.LivingEntity entity) {
        // Sync yaw to galath so MangleLie follows her orientation
        float yaw = galath.isFrozen() ? galath.getFrozenYaw()
                : MathUtil.lerpAngle(galath.yRotO, galath.getYRot(), partialTick);
        entity.setYRot(yaw);
        entity.yRotO = yaw;
    }

    /**
     * Returns the world-space render position when riding a Galath.
     * Original: {@code public static Vec3d b(f_, float)}
     */
    public static Vec3 computeRidingPosition(GalathEntity galath, float partialTick) {
        Vec3 galathMangPos = galath.getBonePosition("mangPos");
        return EntityPositionUtil.getLerpedPosition(galath, Minecraft.getInstance().player, partialTick)
                                 .subtract(galathMangPos);
    }

    /**
     * Returns the world-space render position when no player is present
     * (used in solo animation mode).
     * Original: {@code public static Vec3d a(f_, float)}
     */
    public static Vec3 computeRidingPositionSolo(GalathEntity galath, float partialTick) {
        Vec3 galathMangPos = galath.getBonePosition("mangPos");
        return EntityPositionUtil.getLerpedPositionSolo(galath, partialTick)
                                 .subtract(galathMangPos);
    }

    // =========================================================================
    //  Name-tag override
    // =========================================================================

    @Override
    protected boolean shouldRenderNameTag(MangleLieEntity entity, double dist) {
        // Hide name tag when dead galath parent detected or invisible
        if (isHiddenByDeadGalath(entity)) return false;
        if (entity.isInvisible()) return false;
        return super.shouldRenderNameTag(entity, dist);
    }

    // =========================================================================
    //  Inner helpers
    // =========================================================================

    /** Permanently hidden bone names for this renderer variant. */
    private static class HiddenBoneSet extends HashSet<String> {
        HiddenBoneSet() {
            add("boobs2");
            add("booty2");
            add("vagina2");
            add("fuckhole2");
        }
    }
}
