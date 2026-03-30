package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor; // Ajusta el paquete según tu estructura
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
 * SpearModel — Portado a 1.20.1.
 * * Modelo de un solo hueso (prop) usado para lanzas o armas de tribu.
 * * Implementa IBoneAccessor para que el renderizador pueda manipular su rotación en tiempo real.
 */
public class SpearModel<T extends Entity> extends EntityModel<T> implements IBoneAccessor {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("sexmod", "spear"), "main");

    // Renombrado a spearPart para mayor claridad semántica
    private final ModelPart spearPart;

    public SpearModel(ModelPart root) {
        this.spearPart = root.getChild("spear");
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("spear",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // 1.20.1: Usamos flotantes explícitos en las dimensiones
                        .addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 16, 16);
    }

    // ── EntityModel ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Objeto estático — sin animaciones por tick de entidad
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.spearPart.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    @Override
    public void setBoneRotation(ModelPart part, float x, float y, float z) {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    @Override
    public ModelPart getRootBone() {
        return this.spearPart;
    }
}