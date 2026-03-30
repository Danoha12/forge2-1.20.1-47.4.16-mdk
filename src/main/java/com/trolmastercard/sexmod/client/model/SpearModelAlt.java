package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * SpearModelAlt — Portado a 1.20.1.
 * * Modelo "prop" de un solo hueso estilo vainilla para la lanza.
 * * Geometría idéntica a SpearModel, pero registrado en su propia capa
 * * y diseñado para una hoja de texturas de 64x32.
 */
public class SpearModelAlt extends EntityModel<Entity> implements IBoneAccessor {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("sexmod", "spear_alt"), "main");

    // Renombrado para evitar confusión semántica con la raíz real del modelo
    private final ModelPart altSpearPart;

    public SpearModelAlt(ModelPart root) {
        this.altSpearPart = root.getChild("bone");
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("bone",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // 1.20.1: Forzamos el uso de flotantes explícitos y CubeDeformation
                        .addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        // La diferencia principal: Usa un lienzo de textura de 64x32
        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── EntityModel ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Objeto estático — sin animaciones por tick
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.altSpearPart.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    @Override
    public ModelPart getRootBone() {
        return this.altSpearPart;
    }

    @Override
    public void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }
}