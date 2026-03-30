package com.trolmastercard.sexmod.client.model; // Ajusta al paquete de tus modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * PropModel — Portado a 1.20.1.
 * * Modelo de un solo hueso (cubo básico) usado para utilería (varitas, velas, etc.).
 * * Implementa IBoneAccessor para manipulación de rotación en tiempo real.
 */
public class PropModel<T extends Entity> extends EntityModel<T> implements IBoneAccessor {

    // Capa de renderizado registrada en el sistema de Forge
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("sexmod", "prop"), "main");

    // Renombrado de 'root' a 'propPart' para mayor claridad
    private final ModelPart propPart;

    public PropModel(ModelPart root) {
        this.propPart = root.getChild("prop");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("prop",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Las animaciones de los props generalmente se controlan desde el renderizador
        // externo usando IBoneAccessor, por lo que esto queda vacío.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.propPart.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    @Override
    public ModelPart getRootBone() {
        return this.propPart;
    }
}