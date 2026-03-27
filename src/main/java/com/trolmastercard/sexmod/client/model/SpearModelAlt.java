package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.IBoneAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * SpearModelAlt - ported from bf.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Vanilla-style single-bone prop model for the spear item.
 * Identical geometry to a5/a7 (SpearModel), but registered as its own layer.
 * Implements {@link IBoneAccessor} so bone rotations can be set at runtime.
 *
 * Geometry:
 *   Pivot: (-5, 2.5, 0)
 *   Box:   offset(-2,-6,0), size(2,6,2)
 *
 * In 1.12.2:
 *   - Extended {@code ModelBase} implements {@code at} (IBoneAccessor).
 *   - {@code func_78088_a} - {@link #renderToBuffer(PoseStack,...)}.
 *   - {@code func_78793_a} (setRotationPoint) - {@link PartPose#offset}.
 *   - {@code field_78804_l} (cubeList) - {@link CubeListBuilder}.
 *   - {@code ModelRenderer.field_78795_f/g/h} (rotateAngle*) - {@code part.xRot/yRot/zRot}.
 */
public class SpearModelAlt extends EntityModel<Entity> implements IBoneAccessor {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation("sexmod", "spear_alt"), "main");

    private final ModelPart root;

    public SpearModelAlt(ModelPart root) {
        this.root = root.getChild("bone");
    }

    // =========================================================================
    //  LayerDefinition factory (call from EntityRenderersEvent.RegisterLayerDefinitions)
    // =========================================================================

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("bone",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2),
            PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    // =========================================================================
    //  EntityModel overrides
    // =========================================================================

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // No animation - static prop
    }

    @Override
    public void renderToBuffer(PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // =========================================================================
    //  IBoneAccessor
    // =========================================================================

    @Override
    public ModelPart getRootBone() {
        return root;
    }

    @Override
    public void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }
}
