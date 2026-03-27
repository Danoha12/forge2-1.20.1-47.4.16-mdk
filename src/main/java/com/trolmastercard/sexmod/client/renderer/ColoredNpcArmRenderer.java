package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * ColoredNpcArmRenderer (d9) - Ported from 1.12.2 to 1.20.1.
 *
 * Abstract extension of {@link NpcArmRenderer} that adds per-bone colour
 * overrides with an entity-scoped cache and a few helpers:
 *
 *   {@link #getBoneColor(String)}  - abstract: return the RGB Vec3i for a bone name.
 *   {@link #clearColorCache()}     - flush the static cache (call on dimension change etc.)
 *   {@link #showChildBone(GeoBone, int)} - un-hide one specific child by index
 *   {@link #selectChildBone(GeoBone, int)} - un-hide one child and hide all others
 *
 * The static cache is keyed on {@code boneName.hashCode() + entity.uuid.hashCode()}.
 *
 * Additional overrides vs NpcArmRenderer:
 *   - {@code applyWorldTransforms} / {@code restoreWorldTransforms}
 *     apply a shrink factor read from {@code e7.aA} (scaleProgress) data param.
 *   - {@code applyItemRotation(ItemStack)} returns preferred rotation (-90-,0,0).
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code AnimatedGeoModel} - {@code GeoModel}
 *   - {@code GlStateManager.func_179152_a(scale)} - {@code PoseStack.scale}
 *   - {@code GlStateManager.func_179139_a(scale)} - inverse {@code PoseStack.scale}
 *   - {@code javax.vecmath.Vector3f} - {@code org.joml.Vector3f}
 *   - {@code Vec3d} - {@code Vec3}
 *   - {@code RenderManager} constructor arg dropped
 */
public abstract class ColoredNpcArmRenderer<T extends BaseNpcEntity>
        extends NpcArmRenderer<T> {

    // -- Constants -------------------------------------------------------------

    /** White - returned when no colour override is defined for a bone. */
    protected static final Vec3i WHITE = new Vec3i(255, 255, 255);

    // -- Colour cache ----------------------------------------------------------

    /** Cache: (boneName.hashCode + entity.uuid.hashCode) - RGB Vec3i. */
    static final HashMap<Integer, Vec3i> boneColorCache = new HashMap<>();

    public static void clearColorCache() {
        boneColorCache.clear();
    }

    // -- Constructor -----------------------------------------------------------

    protected ColoredNpcArmRenderer(EntityRendererProvider.Context context,
                                    GeoModel<T> model) {
        super(context, model);
    }

    protected ColoredNpcArmRenderer(GeoModel<T> model) {
        super(model);
    }

    // -- Abstract colour provider ----------------------------------------------

    /**
     * Return the RGB colour (each component 0-255) for the given bone name.
     * Called at most once per (bone, entity) pair per render session;
     * results are cached in {@link #boneColorCache}.
     *
     * Corresponds to abstract {@code a(String)} in 1.12.2.
     */
    protected abstract Vec3i getBoneColor(String boneName);

    // -- Colour lookup (with cache) --------------------------------------------

    /**
     * Fetch bone colour from cache, or compute and store it.
     * Corresponds to {@code a(GeoBone)} in 1.12.2.
     */
    protected Vec3i getBoneColor(GeoBone bone) {
        int key = bone.getName().hashCode()
                + (entityRef != null ? entityRef.getUUID().hashCode() : 0);
        Vec3i cached = boneColorCache.get(key);
        if (cached != null) return cached;

        Vec3i color = applyColorTransform(getBoneColor(bone.getName()));
        boneColorCache.put(key, color);
        return color;
    }

    /**
     * Optional transform applied to every raw colour value before caching.
     * Base returns the value unchanged.
     * Corresponds to {@code a(Vec3i)} in 1.12.2.
     */
    protected Vec3i applyColorTransform(Vec3i color) {
        return color;
    }

    // -- Child-bone helpers ----------------------------------------------------

    /**
     * Un-hide a single child bone by list index; do not touch others.
     * Corresponds to {@code b(GeoBone, int)} in 1.12.2.
     */
    protected void showChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        if (index < children.size()) {
            children.get(index).setHidden(false);
        }
    }

    /**
     * Un-hide one child (sorted ascending by pivotY) and hide all others.
     * Returns the selected bone, or null.
     * Corresponds to {@code a(GeoBone, int)} in 1.12.2.
     */
    protected GeoBone selectChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        children.sort(Comparator.comparingDouble(GeoBone::getPivotY));
        GeoBone selected = null;
        for (int i = 0; i < children.size(); i++) {
            GeoBone child = children.get(i);
            if (i == index) {
                child.setHidden(false);
                selected = child;
            } else {
                child.setHidden(true);
            }
        }
        return selected;
    }

    // -- Scale shrink from scaleProgress data param ----------------------------

    /**
     * Read the scaleProgress data parameter (e7.aA) and apply a shrink scale.
     * Corresponds to {@code d()} in 1.12.2.
     */
    protected void applyScaleShrink(PoseStack poseStack) {
        float progress = getScaleProgress();
        float scale = 1.0f - (0.25f - progress);
        poseStack.scale(scale, scale, scale);
    }

    /**
     * Inverse of {@link #applyScaleShrink} - restore to normal size.
     * Corresponds to {@code b()} in 1.12.2.
     */
    protected void restoreScaleShrink(PoseStack poseStack) {
        float progress = getScaleProgress();
        double inv = 1.0 / (1.0 - (0.25f - progress));
        poseStack.scale((float) inv, (float) inv, (float) inv);
    }

    /**
     * Read the scaleProgress float from the entity's synched data (e7.aA).
     * Override or replace with the appropriate accessor once BaseNpcEntity
     * exposes the data parameter.
     */
    protected float getScaleProgress() {
        if (entityRef == null) return 0.0f;
        return entityRef.getScaleProgress(); // stub; implement in BaseNpcEntity
    }

    // -- GeckoLib4: inject colour into vertex consumer -------------------------

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable,
                                   GeoBone bone, net.minecraft.client.renderer.RenderType renderType,
                                   MultiBufferSource bufferSource,
                                   VertexConsumer buffer, boolean isReRender,
                                   float partialTick, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        // Apply bone colour override
        Vec3i color = getBoneColor(bone);
        float r = color.getX() / 255.0f;
        float g = color.getY() / 255.0f;
        float b = color.getZ() / 255.0f;

        super.renderRecursively(poseStack, animatable, bone, renderType,
                bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, r, g, b, alpha);
    }
}
