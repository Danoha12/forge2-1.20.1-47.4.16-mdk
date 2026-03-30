package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * EnderPearlModel — Portado a 1.20.1.
 * * Modelo para la bola de energía / ataque de Galath.
 * * Dibuja cubos anidados giratorios usando manipulaciones de matriz.
 */
public class EnderPearlModel extends EntityModel<Entity> {

    // Registramos la capa para el bus del cliente
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("sexmod", "energy_ball"), "main");

    private final ModelPart glass;
    private final ModelPart cube;

    /** Ángulo de giro (en grados) que le pasará el Renderer en cada frame. */
    public float spinAngle = 0.0F;

    public EnderPearlModel(ModelPart root) {
        this.glass = root.getChild("glass");
        this.cube = root.getChild("cube");
    }

    // ── Construcción de la Malla ─────────────────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Capa exterior (Cubo de cristal)
        root.addOrReplaceChild("glass",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4F, -4F, -4F, 8, 8, 8),
                PartPose.ZERO);

        // Capa interior
        root.addOrReplaceChild("cube",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4F, -4F, -4F, 8, 8, 8),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── Renderizado (Manipulación de Matrices) ───────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // No se necesita setup procedural aquí; la rotación matemática se hace en el render.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        // Escala base
        poseStack.scale(2.0F, 2.0F, 2.0F);

        float spinRad = (float) Math.toRadians(this.spinAngle);
        Vector3f tiltAxis = new Vector3f(0.7071F, 0.0F, 0.7071F); // Eje diagonal pre-calculado
        float tiltAngle = (float) Math.toRadians(60.0F);

        // 1. Capa de cristal exterior
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        poseStack.mulPose(Axis.of(tiltAxis).rotation(tiltAngle));
        this.glass.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // 2. Segunda capa de cristal (reducida y girada de nuevo)
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(Axis.of(tiltAxis).rotation(tiltAngle));
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        this.glass.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // 3. Cubo interior (reducido aún más)
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(Axis.of(tiltAxis).rotation(tiltAngle));
        poseStack.mulPose(Axis.YP.rotation(spinRad));
        this.cube.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();
    }
}