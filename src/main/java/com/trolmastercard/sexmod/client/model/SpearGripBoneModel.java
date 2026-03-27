package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.IBoneAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * Tiny single-part model used as the spear grip bone accessor.
 * Ported from f1.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original geometry: one box [-2,-6,0] size 2-6-2, pivot at (-5, 2.5, 0).
 */
public class SpearGripBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    public static final String PART_NAME = "grip";

    private final ModelPart root;
    private final ModelPart grip;

    public SpearGripBoneModel(ModelPart root) {
        this.root = root;
        this.grip = root.getChild(PART_NAME);
    }

    // -- LayerDefinition factory -----------------------------------------------

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild(
                PART_NAME,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, 0.0F),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    // -- EntityModel ----------------------------------------------------------

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // no per-tick animation
    }

    @Override
    public void renderToBuffer(PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        grip.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // -- IBoneAccessor --------------------------------------------------------

    @Override
    public void setBoneRotation(ModelPart bone, float rx, float ry, float rz) {
        bone.xRot = rx;
        bone.yRot = ry;
        bone.zRot = rz;
    }

    @Override
    public ModelPart getBone() {
        return this.grip;
    }
}
