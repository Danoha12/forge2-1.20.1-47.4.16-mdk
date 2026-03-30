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
 * EggModel — Portado a 1.20.1.
 * * Modelo estático de un solo hueso (prop) usado durante las animaciones de Mating Press.
 */
public class EggModel<T extends Entity> extends EntityModel<T> implements IBoneAccessor {

    // Registramos la capa en el bus de eventos del cliente
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("sexmod", "egg"), "main");

    private final ModelPart egg;

    public EggModel(ModelPart root) {
        this.egg = root.getChild("egg");
    }

    // ── Construcción de la Malla (Nuevo estándar 1.20.1) ─────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Creamos el cubo con las mismas medidas y offset originales
        root.addOrReplaceChild(
                "egg",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 16, 16);
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Es un prop estático, la animación se maneja externamente modificando la rotación del hueso
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.egg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    @Override
    public ModelPart getBoneRoot() {
        return this.egg;
    }
}