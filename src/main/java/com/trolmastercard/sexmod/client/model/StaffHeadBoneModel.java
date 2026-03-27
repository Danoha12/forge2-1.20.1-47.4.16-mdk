package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * StaffHeadBoneModel - ported from cf.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A two-part legacy model used as the "head" attachment on the Staff item.
 * It implements {@link IBoneAccessor} so the NPC hand-renderer can retrieve its
 * root {@link ModelPart} for direct rendering.
 *
 * Parts:
 *   a (root pivot)       - pivot at (-5, -/2, 0)   - effectively (-5, 1.5708, 0)
 *   b (child, rotated)   - pivot at (-1, -3, 1), UV(0,0) box (-1,-3,-1, 2-6-2)
 *   c (empty secondary)  - pivot at (0,0,0), no boxes
 *
 * Field mapping:
 *   a - rootPart        (the root pivot rendered on first call to func_78088_a)
 *   b - headPart        (child with actual cube + xRot=0, yRot=-/2, zRot=0)
 *   c - secondaryPart   (empty pivot)
 *
 * In 1.12.2:
 *   ModelBase / ModelRenderer   - EntityModel<T> / ModelPart
 *   func_78793_a(x,y,z)        - PartPose.offset(x,y,z)
 *   func_78792_a(child)         - parent.addOrReplaceChild()
 *   field_78795_f/g/h           - rotX/Y/Z on PartPose.offsetAndRotation
 *   ModelBox(..., inflate, mirror) - CubeListBuilder.addBox(..., CubeDeformation)
 *   func_78785_a(scale)         - modelPart.render(poseStack, consumer, light, overlay, ...)
 *   implements at              - implements IBoneAccessor
 *   a() returns a              - getBoneRoot() returns rootPart
 */
public class StaffHeadBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    private final ModelPart rootPart;
    private final ModelPart secondaryPart;

    public StaffHeadBoneModel(ModelPart root) {
        this.rootPart      = root.getChild("root");
        this.secondaryPart = root.getChild("secondary");
    }

    // =========================================================================
    //  LayerDefinition
    // =========================================================================

    public static net.minecraft.client.model.geom.builders.LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Root pivot: offset(-5, -/2 - 1.5708, 0)
        // The original uses func_78793_a(-5, 1.5708, 0) which is a POSITION offset,
        // but combined with the yRot=-/2 on child b it effectively becomes a rotation.
        PartDefinition rootDef = root.addOrReplaceChild("root",
            CubeListBuilder.create(),
            PartPose.offset(-5.0F, 1.5708F, 0.0F));

        // Child b: pivot at (-1, -3, 1), yRot = -/2, cube UV(0,0) at (-1,-3,-1) 2-6-2
        rootDef.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.0F, -3.0F, -1.0F, 2, 6, 2, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-1.0F, -3.0F, 1.0F,
                0.0F, (float) Math.PI / 2.0F, 0.0F));

        // Empty secondary pivot at (0,0,0)
        root.addOrReplaceChild("secondary",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    // =========================================================================
    //  EntityModel overrides
    // =========================================================================

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // No per-tick animation
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        rootPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        secondaryPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // =========================================================================
    //  IBoneAccessor  (original: at.a())
    // =========================================================================

    /** Returns the root ModelPart so the hand-renderer can position it correctly. */
    @Override
    public ModelPart getBoneRoot() {
        return rootPart;
    }
}
