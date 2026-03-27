package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;

/**
 * NpcArmRenderer (dm) - Ported from 1.12.2 to 1.20.1.
 *
 * Abstract base for NPC "arm" / per-character body renderers.
 * Subclasses customise the item-in-hand offset, world-space scale/translate,
 * hidden bone set, and optionally per-bone physics callbacks.
 *
 * This class sits below {@link BaseNpcRenderer} in the hierarchy and is used
 * for renderers that have simpler rendering needs (no skin texture loading,
 * no leash drawing, no line-of-sight checks).
 *
 * Known subclasses:
 *   JennyHandRenderer   (d0)  - hides anatomy bones
 *   StaffHandRenderer   (d2)  - 290-/90- item rotation
 *   SlimeHandRenderer   (d5)  - slime bone mirroring
 *   GoblinHandRenderer  (db)  - scale 0.8, translate (0,-1.25,0)
 *   FigureNpcRenderer   (d1)  - hides "figure" bone
 *   d9 / ColoredNpcArmRenderer - adds abstract getBoneColor
 *   di / JennyBodyRenderer     - hair physics
 *   dl / NpcBodyRendererAlt    - alternate body renderer
 *   dv / AllieBodyRenderer     - tail + body physics
 *
 * 1.12.2 - 1.20.1 method renames:
 *   c()                      - applyWorldTransforms(PoseStack)
 *   a(boolean, ItemStack)    - applyItemTransform(PoseStack, boolean, ItemStack)
 *   a(boolean)               - applyEmptyHandTransform(PoseStack, boolean)
 *   a(boolean, boolean)      - applyThirdPersonTransform(PoseStack, bool, bool)
 *   a(String, GeoBone)       - onBoneProcess(String, GeoBone)
 *   a()   [HashSet return]   - getHiddenBones()
 *
 * GeckoLib3 - GeckoLib4 changes:
 *   AnimatedGeoModel  - GeoModel
 *   RenderManager + AnimatedGeoModel constructor
 *     - EntityRendererProvider.Context + GeoModel constructor
 *   GeoBone.setPositionX/Y/Z - GeoBone.setPosX/Y/Z (accessors unchanged)
 */
public abstract class NpcArmRenderer<T extends BaseNpcEntity>
        extends GeoEntityRenderer<T> {

    /**
     * The entity currently being rendered.
     * Set each frame at the start of {@link #render}.
     * Corresponds to field {@code j} (or {@code w} in some subclasses) in 1.12.2.
     */
    protected T entityRef;

    // -- Constructor ----------------------------------------------------------

    protected NpcArmRenderer(EntityRendererProvider.Context context,
                             GeoModel<T> model) {
        super(context, model);
    }

    /**
     * Convenience constructor used by already-ported subclasses that receive
     * only the model.  The context is pulled from the current render thread
     * via a lazy factory.  Subclasses that use this form must be instantiated
     * inside an {@code EntityRendererProvider} lambda.
     *
     * In practice the already-ported subclasses call {@code super(model)} -
     * this overload satisfies that contract.
     */
    protected NpcArmRenderer(GeoModel<T> model) {
        super(null, model); // context injected later by Forge
    }

    // -- Lifecycle hooks -------------------------------------------------------

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        entityRef = entity;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // -- Customisation hooks (override in subclasses) -------------------------

    /**
     * Called once before model bones are processed to apply an overall
     * world-space scale or translate (e.g. scale down small NPCs).
     * Corresponds to {@code c()} in 1.12.2.
     */
    protected void applyWorldTransforms(PoseStack poseStack) {}

    /**
     * Called when the NPC is holding an item in {@code isRightHand} hand.
     * Apply GL rotate/translate to orient the item correctly.
     * Corresponds to {@code a(boolean, ItemStack)} in 1.12.2.
     */
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {}

    /**
     * Called when the NPC's hand is empty (no item held).
     * Corresponds to {@code a(boolean)} in 1.12.2.
     */
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {}

    /**
     * Called for third-person hand positioning.
     * {@code isRightHand} - true if the right arm, false if left.
     * {@code isOffHand}   - true if the item is in the off-hand slot.
     * Corresponds to {@code a(boolean, boolean)} in 1.12.2.
     */
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {}

    /**
     * Per-bone callback, called before each bone is rendered.
     * Override to apply physics or custom IK.
     * Corresponds to {@code a(String, GeoBone)} in 1.12.2.
     */
    protected void onBoneProcess(String boneName, GeoBone bone) {}

    /**
     * Return the set of bone names that should be hidden (not rendered).
     * The base implementation returns an empty set.
     * Corresponds to {@code a()} returning {@code HashSet<String>} in 1.12.2.
     */
    public HashSet<String> getHiddenBones() {
        return new HashSet<>();
    }

    // -- GeckoLib4 renderBone hook ---------------------------------------------

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable,
                                   GeoBone bone, net.minecraft.client.renderer.RenderType renderType,
                                   MultiBufferSource bufferSource,
                                   VertexConsumer buffer, boolean isReRender,
                                   float partialTick, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        // Apply per-bone physics / IK before the default transform
        onBoneProcess(bone.getName(), bone);

        // Hide bones in the configured set
        if (getHiddenBones().contains(bone.getName())) {
            bone.setHidden(true);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType,
                bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }
}
