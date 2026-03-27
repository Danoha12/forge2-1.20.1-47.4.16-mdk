package com.trolmastercard.sexmod;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

/**
 * Minimal single-bone model used as the fishing-rod tip bone accessor.
 * Obfuscated name: fv
 */
public class FishingRodBoneModel extends BaseNpcModel<Entity> implements IBoneAccessor {

    private final ModelPart tipBone;

    public FishingRodBoneModel(ModelPart root) {
        super(root);
        this.tipBone = root.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition tip = root.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 16, 8);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // No animation - static bone
    }

    // -- IBoneAccessor ---------------------------------------------------------

    @Override
    public void setBoneRotation(ModelPart bone, float rotX, float rotY, float rotZ) {
        bone.xRot = rotX;
        bone.yRot = rotY;
        bone.zRot = rotZ;
    }

    @Override
    public ModelPart getTipBone() {
        return this.tipBone;
    }
}
