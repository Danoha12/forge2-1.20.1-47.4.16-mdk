package com.trolmastercard.sexmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

/**
 * EnergyBallLegacyModel - ported from e8.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A simple spinning-cube model used for the energy ball entity.
 * The outer cube ("glass") uses UV (0,0) and the inner cube ("cube") uses UV (32,0).
 * Both are rendered with successive 60- rotations and 0.875 scales for a gyroscope effect.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - ModelBase - EntityModel (legacy ModelBase removed in 1.17)
 *   - new ModelRenderer(this, "glass") - created via LayerDefinition / ModelPart factory
 *   - func_78784_a(u,v) - texOffs(u,v) in MeshDefinition builder
 *   - func_78789_a(x,y,z,w,h,d) - addBox(x,y,z,w,h,d) in CubeListBuilder
 *   - modelRenderer.func_78785_a(scale) - modelPart.render(poseStack, consumer, light, overlay)
 *   - GlStateManager.func_179094_E / func_179121_F - poseStack.pushPose / popPose
 *   - GlStateManager.func_179152_a(sx,sy,sz) - poseStack.scale(sx,sy,sz)
 *   - GlStateManager.func_179114_b(angle,x,y,z) - poseStack.mulPose(Axis.of(x,y,z).rotationDegrees(angle))
 *   - func_78088_a(entity, ..., scale) - renderToBuffer(poseStack, consumer, light, overlay, r,g,b,a)
 */
public class EnergyBallLegacyModel extends EntityModel<Entity> {

    private final ModelPart glass;
    private final ModelPart cube;

    public EnergyBallLegacyModel(ModelPart root) {
        this.glass = root.getChild("glass");
        this.cube  = root.getChild("cube");
    }

    /** Builds the layer definition for baking into the entity renderer. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("glass",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8),
                PartPose.ZERO);

        root.addOrReplaceChild("cube",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    // -- EntityModel contract ---------------------------------------------------

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                           float ageInTicks, float netHeadYaw, float headPitch) {
        // Rotation is applied procedurally in renderToBuffer
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer consumer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        ps.pushPose();

        // Layer 1: outer glass shell - scale 2, rotate 60- + spin
        ps.scale(2.0F, 2.0F, 2.0F);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));       // spin driven by ageInTicks in renderer
        ps.mulPose(com.mojang.math.Axis.of(new org.joml.Vector3f(0.7071F, 0, 0.7071F)).rotationDegrees(60.0F));
        glass.render(ps, consumer, packedLight, packedOverlay, r, g, b, a);

        // Layer 2: inner glass - scale 0.875, additional 60- twist
        ps.scale(0.875F, 0.875F, 0.875F);
        ps.mulPose(com.mojang.math.Axis.of(new org.joml.Vector3f(0.7071F, 0, 0.7071F)).rotationDegrees(60.0F));
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
        glass.render(ps, consumer, packedLight, packedOverlay, r, g, b, a);

        // Layer 3: inner solid cube - further 0.875 - 60- twist
        ps.scale(0.875F, 0.875F, 0.875F);
        ps.mulPose(com.mojang.math.Axis.of(new org.joml.Vector3f(0.7071F, 0, 0.7071F)).rotationDegrees(60.0F));
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
        cube.render(ps, consumer, packedLight, packedOverlay, r, g, b, a);

        ps.popPose();
    }
}
