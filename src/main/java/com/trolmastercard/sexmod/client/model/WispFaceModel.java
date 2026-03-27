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
 * WispFaceModel - ported from c_.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A small 5-part legacy model (head, right-eye, left-eye, snout, jaw)
 * used for a wisp/hatchling-face prop entity.  The exact entity is unconfirmed
 * in the decompiled source but the proportions (body at y=17-21, 6-6-6 head)
 * match a small creature head rendered in item/overlay context.
 *
 * Texture sheet layout (inferred from UV offsets):
 *   0,16 - 16-16  body cube
 *   32,0 - 8-8    right eye
 *   32,4 - 8-8    left eye
 *   32,8 - 4-4    snout
 *   10,11 - jaw segments (multiple boxes on the same part)
 *
 * Part mapping:
 *   a - body     (head cube 6-6-6)
 *   d - eyeRight (2-2-2)
 *   e - eyeLeft  (2-2-2)
 *   c - snout    (1-1-1)
 *   b - jaw      (4 rotated segments)
 *
 * ============================================================
 * 1.12.2 - 1.20.1 API changes:
 * ============================================================
 *   ModelBase / ModelRenderer       - EntityModel<T> / ModelPart
 *   ModelBox(renderer, uv, x,y,z, w,h,d, inflate, mirror)
 *     - CubeListBuilder.cube(uv, x,y,z, w,h,d, CubeDeformation, mirror)
 *   renderer.func_78793_a(x,y,z)   - PartPose.offset(x,y,z)
 *   renderer.field_78795_f/g/h (xRot/yRot/zRot) - PartPose.offsetAndRotation
 *   renderer.func_78792_a(child)   - parent.addOrReplaceChild("child", ...)
 *   renderer.func_78785_a(scale)   - modelPart.render(poseStack, consumer, light, overlay)
 *   ModelRenderer.field_78804_l    - CubeListBuilder
 *   a(renderer, rotX, rotY, rotZ)  - encoded in PartPose.offsetAndRotation(...)
 */
public class WispFaceModel extends EntityModel<Entity> {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation("sexmod", "wisp_face"), "main");

    private final ModelPart body;
    private final ModelPart eyeRight;
    private final ModelPart eyeLeft;
    private final ModelPart snout;
    private final ModelPart jaw;

    public WispFaceModel(ModelPart root) {
        this.body     = root.getChild("body");
        this.eyeRight = root.getChild("eye_right");
        this.eyeLeft  = root.getChild("eye_left");
        this.snout    = root.getChild("snout");
        this.jaw      = root.getChild("jaw");
    }

    // =========================================================================
    //  LayerDefinition
    // =========================================================================

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- body (a) -------------------------------------------------------
        // UV(0,16) at (-3,17,-3) size 6-6-6
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-3.0F, 17.0F, -3.0F, 6, 6, 6, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- eye_right (d) --------------------------------------------------
        // UV(32,0) at (1.3,18,-3.5) size 2-2-2
        root.addOrReplaceChild("eye_right",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(1.3F, 18.0F, -3.5F, 2, 2, 2, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- eye_left (e) ---------------------------------------------------
        // UV(32,4) at (-3.3,18,-3.5) size 2-2-2
        root.addOrReplaceChild("eye_left",
            CubeListBuilder.create()
                .texOffs(32, 4)
                .addBox(-3.3F, 18.0F, -3.5F, 2, 2, 2, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- snout (c) ------------------------------------------------------
        // UV(32,8) at (-1,21,-3.5) size 1-1-1
        root.addOrReplaceChild("snout",
            CubeListBuilder.create()
                .texOffs(32, 8)
                .addBox(-1.0F, 21.0F, -3.5F, 1, 1, 1, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- jaw (b) - four rotated segments --------------------------------
        // Parent pivot: (-0.5, 0, 0.1)
        PartDefinition jaw = root.addOrReplaceChild("jaw",
            CubeListBuilder.create(),
            PartPose.offset(-0.5F, 0.0F, 0.1F));

        // Segment 1: pivot(2, 20.7406, 4.0504) rotX=1.0908
        // UV(10,11) box at (-2.5,0,0) size 2-2-1
        jaw.addOrReplaceChild("jaw_seg1",
            CubeListBuilder.create()
                .texOffs(10, 11)
                .addBox(-2.5F, 0.0F, 0.0F, 2, 2, 1, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(2.0F, 20.7406F, 4.0504F,
                1.0908F, 0.0F, 0.0F));

        // Segment 2: pivot(2, 19.9214, 3.4768) rotX=0.6109
        // UV(10,11) box at (-3,0,0) size 3-1-1
        jaw.addOrReplaceChild("jaw_seg2",
            CubeListBuilder.create()
                .texOffs(10, 11)
                .addBox(-3.0F, 0.0F, 0.0F, 3, 1, 1, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(2.0F, 19.9214F, 3.4768F,
                0.6109F, 0.0F, 0.0F));

        // Segment 3: pivot(2, 19.0074, 3.0643) rotX=0.3491
        // UV(10,11) box at (-4,0,0.075) size 5-1-1
        jaw.addOrReplaceChild("jaw_seg3",
            CubeListBuilder.create()
                .texOffs(10, 11)
                .addBox(-4.0F, 0.0F, 0.075F, 5, 1, 1, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(2.0F, 19.0074F, 3.0643F,
                0.3491F, 0.0F, 0.0F));

        // Segment 4: pivot(0, 17.925, 3.5) rotX=0.1309
        // UV(10,11) box at (-3,-1,-0.5) size 7-2-1
        jaw.addOrReplaceChild("jaw_seg4",
            CubeListBuilder.create()
                .texOffs(10, 11)
                .addBox(-3.0F, -1.0F, -0.5F, 7, 2, 1, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 17.925F, 3.5F,
                0.1309F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // =========================================================================
    //  Render
    // =========================================================================

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // No animation - static mesh
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        eyeRight.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        eyeLeft.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        snout.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        jaw.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
