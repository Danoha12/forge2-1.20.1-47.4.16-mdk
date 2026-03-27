package com.trolmastercard.sexmod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import com.mojang.math.Axis;

/**
 * EnderPearlModel - ported from e8.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A simple model for the ender pearl / energy ball entity:
 *   - An outer glass shell (8-8-8 cube, UV 0,0)
 *   - An inner spinning cube (8-8-8 cube, UV 32,0), scaled 0.875- twice
 *
 * Originally a ModelBase with manual GlStateManager calls.
 * Ported to EntityModel + PoseStack for 1.20.1.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - ModelBase - EntityModel&lt;T extends Entity&gt;
 *   - ModelRenderer.addBox - cube builder via LayerDefinition
 *   - func_78784_a(u,v) - CubeListBuilder.texOffs(u, v)
 *   - func_78789_a(x,y,z,w,h,d) - addBox(x,y,z,w,h,d)
 *   - func_78785_a(scale) - model.render(poseStack, ...)
 *   - GlStateManager.func_179094_E/F - poseStack.pushPose/popPose
 *   - GlStateManager.func_179152_a(sx,sy,sz) - poseStack.scale(sx,sy,sz)
 *   - GlStateManager.func_179114_b(angle,x,y,z) - poseStack.mulPose(Axis)
 *   - paramFloat2 = yRot from EntityRenderer (used as spin angle)
 */
public class EnderPearlModel extends EntityModel<Entity> {

    private final ModelPart glass;
    private final ModelPart cube;

    public EnderPearlModel(ModelPart root) {
        this.glass = root.getChild("glass");
        this.cube  = root.getChild("cube");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root  = mesh.getRoot();

        root.addOrReplaceChild("glass",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4F, -4F, -4F, 8, 8, 8),
                PartPose.ZERO);

        root.addOrReplaceChild("cube",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4F, -4F, -4F, 8, 8, 8),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    // -- Render -----------------------------------------------------------------

    /**
     * @param entity      the entity being rendered
     * @param limbSwing   unused
     * @param limbSwingAmount unused
     * @param ageInTicks  unused
     * @param netHeadYaw  spin angle in degrees (from EntityRenderer yaw param)
     * @param headPitch   unused
     */
    @Override
    public void setupAnim(Entity entity,
                          float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // No pose needed - spin is applied in renderToBuffer
    }

    @Override
    public void renderToBuffer(PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.scale(2.0F, 2.0F, 2.0F);

        float spinRad = (float) Math.toRadians(spinAngle);

        // Outer glass layer 1
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        poseStack.mulPose(Axis.of(new org.joml.Vector3f(0.7071F, 0F, 0.7071F)).rotation((float) Math.toRadians(60F)));
        glass.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // Outer glass layer 2 (shrunk)
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(Axis.of(new org.joml.Vector3f(0.7071F, 0F, 0.7071F)).rotation((float) Math.toRadians(60F)));
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        glass.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // Inner cube (shrunk again)
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(Axis.of(new org.joml.Vector3f(0.7071F, 0F, 0.7071F)).rotation((float) Math.toRadians(60F)));
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        cube.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();
    }

    /** Spin angle (degrees) passed from the renderer each frame. */
    public float spinAngle = 0.0F;
}
