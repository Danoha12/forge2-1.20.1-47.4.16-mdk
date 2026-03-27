package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

/**
 * SpearTipBoneModel - ported from cq.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Single-part model used as the spear-tip attachment on the Spear item.
 * Implements {@link IBoneAccessor} so the NPC hand-renderer can retrieve its
 * root {@link ModelPart}.
 *
 * Part:
 *   a (root)  - pivot at (-5, 2.5, 0), UV(0,0) box at (-2,-6,0, 2-6-2)
 *
 * Field mapping:
 *   a - rootPart
 *
 * In 1.12.2:
 *   ModelBase / ModelRenderer   - EntityModel<T> / ModelPart
 *   func_78793_a(x,y,z)        - PartPose.offset
 *   ModelBox(..., inflate=0)    - CubeListBuilder.addBox
 *   func_78785_a(scale)         - modelPart.render(...)
 *   implements at              - implements IBoneAccessor
 *   a() returns a              - getBoneRoot() returns rootPart
 */
public class SpearTipBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    private final ModelPart rootPart;

    public SpearTipBoneModel(ModelPart root) {
        this.rootPart = root.getChild("root");
    }

    // =========================================================================
    //  LayerDefinition
    // =========================================================================

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pivot (-5, 2.5, 0), UV(0,0) box at (-2,-6,0) size 2-6-2
        root.addOrReplaceChild("root",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
            PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    // =========================================================================
    //  EntityModel
    // =========================================================================

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        rootPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // =========================================================================
    //  IBoneAccessor
    // =========================================================================

    @Override
    public ModelPart getBoneRoot() { return rootPart; }
}
