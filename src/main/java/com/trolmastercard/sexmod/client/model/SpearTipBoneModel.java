package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor; // Asegúrate del paquete
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * SpearTipBoneModel — Portado a 1.20.1.
 * * Modelo de una sola parte usado como anclaje/punta para el ítem de la lanza.
 * * Implementa IBoneAccessor para recuperar su ModelPart raíz.
 */
public class SpearTipBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    private final ModelPart rootPart;

    public SpearTipBoneModel(ModelPart root) {
        this.rootPart = root.getChild("root");
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pivote (-5, 2.5, 0), UV(0,0), caja en (-2,-6,0) tamaño 2x6x2
        root.addOrReplaceChild("root",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // 1.20.1: Uso de flotantes explícitos para evitar ambigüedades
                        .addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── EntityModel ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Objeto estático, controlado por el renderizador de la mano
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.rootPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    // 🚨 CORREGIDO: Estandarizado a getRootBone() para coincidir con SpearModel
    @Override
    public ModelPart getRootBone() {
        return this.rootPart;
    }

    // Opcional: Si IBoneAccessor exige setBoneRotation, deberías agregarlo aquí también
    /*
    @Override
    public void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }
    */
}