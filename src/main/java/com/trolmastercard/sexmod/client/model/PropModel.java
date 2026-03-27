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
 * PropModel - ported from bf.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Another single-bone prop model identical in geometry to SpearModel (a5/a7),
 * but registered under a distinct layer location so it can be used for
 * different prop types (wand, candle, etc.).
 *
 * Implements {@link IBoneAccessor} so external rendering code can manipulate
 * bone rotations at runtime.
 *
 * Geometry:
 *   pivot: (-5, 2.5, 0)
 *   box: (-2, -6, 0) size(2, 6, 2) texOff(0, 0)
 *
 * In 1.12.2:
 *   - Extended {@code ModelBase} + implemented {@code at} (IBoneAccessor).
 *   - {@code func_78793_a(x,y,z)} - {@code PartPose.offset(x,y,z)}.
 *   - {@code new ModelBox(-, tx, ty, x, y, z, w, h, d, scale, mirror)} -
 *     {@code CubeListBuilder.create().texOffs(tx,ty).addBox(x,y,z,w,h,d,-)}.
 *   - {@code func_78785_a(scale)} - {@code root.render(poseStack, buffer, -)}.
 *   - {@code field_78795_f/78796_g/78808_h} - {@code part.xRot/yRot/zRot}.
 */
public class PropModel<T extends Entity> extends EntityModel<T> implements IBoneAccessor {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation("sexmod", "prop"), "main");

    private final ModelPart root;

    public PropModel(ModelPart root) {
        this.root = root.getChild("prop");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("prop",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
            PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // IBoneAccessor
    @Override public ModelPart getRootBone() { return root; }
}
