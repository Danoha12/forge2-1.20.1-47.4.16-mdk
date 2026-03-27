package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * NpcColoredRenderer (d6) - Abstract GeoEntityRenderer base that adds
 * per-bone colour overrides, bone-child selection helpers, and inline item
 * rendering at named attachment bones ("weapon", "itemRenderer").
 *
 * Subclasses implement {@link #getBoneColor(String)} to return an RGB triple
 * (each component 0-255) per bone name.
 *
 * The static colour cache {@link #boneColorCache} is keyed on
 * (boneName.hashCode + entity.UUID.hashCode) to avoid recomputing every frame.
 * Call {@link #clearColorCache()} between sessions / dimension changes.
 */
public abstract class NpcColoredRenderer<T extends BaseNpcEntity>
        extends BaseNpcRenderer<T> {

    protected static final int[] WHITE_RGB = {255, 255, 255};

    /** Global cache: (boneName.hash ^ entity.UUID.hash) - RGB[3] */
    static final HashMap<Integer, int[]> boneColorCache = new HashMap<>();

    public NpcColoredRenderer(EntityType<T> type, GeoModel<T> model, double shadowRadius) {
        super(type, model, shadowRadius);
    }

    public static void clearColorCache() {
        boneColorCache.clear();
    }

    // -- Colour resolution -----------------------------------------------------

    /**
     * Returns a cached RGB triple for the given bone on this entity.
     * Falls back to {@link #transformColor(int[])} for any post-processing.
     */
    protected int[] getCachedBoneColor(GeoBone bone) {
        String name = bone.getName();
        int key = name.hashCode() ^ animatable.getUUID().hashCode();
        int[] cached = boneColorCache.get(key);
        if (cached != null) return cached;
        int[] color = getBoneColor(name);
        int[] transformed = transformColor(color);
        boneColorCache.put(key, transformed);
        return transformed;
    }

    /**
     * Returns the base RGB (0-255 each component) for the given bone name.
     * Return {@link #WHITE_RGB} for bones that should use the default colour.
     */
    protected abstract int[] getBoneColor(String boneName);

    /**
     * Optional post-transform applied to every resolved colour (e.g. tinting).
     * Default: identity.
     */
    protected int[] transformColor(int[] rgb) { return rgb; }

    // -- Child-bone selection helpers ------------------------------------------

    /**
     * Shows only child bone at {@code index} (sets all others hidden).
     * Returns the shown bone, or null if index is out of range.
     */
    protected static GeoBone selectChildBone(GeoBone parent, int index) {
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

    /**
     * Shows child bone at {@code index} without hiding others.
     */
    protected static void showChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        if (index >= 0 && index < children.size()) {
            children.get(index).setHidden(false);
        }
    }

    // -- Render override -------------------------------------------------------

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable,
                                   GeoBone bone, net.minecraft.client.renderer.RenderType renderType,
                                   net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                   VertexConsumer buffer, boolean isReRender, float partialTick,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // Item attachment: "weapon" bone renders the held weapon
        if ("weapon".equals(name)) {
            renderWeaponBone(poseStack, animatable, bone, bufferSource, partialTick, packedLight, packedOverlay);
        }
        // "itemRenderer" during PAYMENT state renders payment item
        if ("itemRenderer".equals(name) && animatable.getAnimState() == AnimState.PAYMENT) {
            renderPaymentItemBone(poseStack, animatable, bone, bufferSource, partialTick, packedLight, packedOverlay);
        }

        // Per-bone colour from subclass
        int[] rgb = getCachedBoneColor(bone);
        float r = rgb[0] / 255f;
        float g = rgb[1] / 255f;
        float b2 = rgb[2] / 255f;

        // Bone callback for subclasses
        onBoneProcess(name, bone);

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource,
            buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b2, alpha);
    }

    /** Called for every bone before super render - override for IK/animation tweaks. */
    protected void onBoneProcess(String boneName, GeoBone bone) {}

    /** Renders the weapon item at the "weapon" bone location. */
    protected void renderWeaponBone(PoseStack poseStack, T entity, GeoBone bone,
                                     net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                     float partialTick, int packedLight, int packedOverlay) {
        // TODO: push matrix to bone, render held item via ItemRenderer
    }

    /** Renders a payment item at the "itemRenderer" bone location. */
    protected void renderPaymentItemBone(PoseStack poseStack, T entity, GeoBone bone,
                                          net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
        // TODO: push matrix to bone, render payment item via ItemRenderer
    }
}
