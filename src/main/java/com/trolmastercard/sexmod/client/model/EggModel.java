package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * EggModel - ported from a0.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A tiny single-part model used for the tribe egg prop rendered above the
 * kobold during the mating-press-cum sequence ({@code showEgg = true}).
 *
 * Original geometry:
 *   pivot : x=-5, y=2.5, z=0
 *   cuboid: texOff(0,0), from(-2,-6,0), size(2,6,2)
 */
public class EggModel<T extends Entity> extends EntityModel<T> implements IBoneAccessor {

    // =========================================================================
    //  Layer registration
    // =========================================================================

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation("sexmod", "egg"), "main");

    // =========================================================================
    //  Parts
    // =========================================================================

    /** The single egg/prop bone. */
    private final ModelPart egg;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public EggModel(ModelPart root) {
        this.egg = root.getChild("egg");
    }

    // =========================================================================
    //  LayerDefinition factory  (registered in ClientSetup)
    // =========================================================================

    /**
     * Builds the {@link LayerDefinition} for this model.
     *
     * <pre>
     * Texture size : 16 - 16  (minimal atlas; adjust if a real texture is supplied)
     * Bone "egg"   : pivot (-5, 2.5, 0)
     *   Cube       : texOffset(0,0), from(-2,-6,0), size(2,6,2), inflate=0
     * </pre>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
            "egg",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -6.0F, 0.0F,  // origin within the part
                         2,     6,     2,      // size X, Y, Z
                         new CubeDeformation(0.0F)),
            PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 16, 16);
    }

    // =========================================================================
    //  Render
    // =========================================================================

    @Override
    public void setupAnim(T entity,
                          float limbSwing,
                          float limbSwingAmount,
                          float ageInTicks,
                          float netHeadYaw,
                          float headPitch) {
        // Static prop - no procedural animation needed.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack,
                               VertexConsumer buffer,
                               int packedLight,
                               int packedOverlay,
                               float red, float green, float blue, float alpha) {
        egg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // =========================================================================
    //  IBoneAccessor  (replaces the at interface + a(ModelRenderer) / a() methods)
    // =========================================================================

    /**
     * Sets the local-space rotation angles on any {@link ModelPart}.
     *
     * Equivalent to the original {@code a(ModelRenderer, float, float, float)}.
     *
     * @param part  the part to rotate
     * @param xRot  X rotation in radians
     * @param yRot  Y rotation in radians
     * @param zRot  Z rotation in radians
     */
    @Override
    public void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    /**
     * Returns the root bone of this model.
     *
     * Equivalent to the original {@code a()} method.
     */
    @Override
    public ModelPart getRootBone() {
        return egg;
    }
}
