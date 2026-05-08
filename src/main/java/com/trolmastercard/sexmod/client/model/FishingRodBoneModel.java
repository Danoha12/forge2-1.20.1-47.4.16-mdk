package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * FishingRodBoneModel — Portado a 1.20.1.
 * * Modelo de un solo hueso que sirve como ancla para el hilo de pescar.
 */
public class FishingRodBoneModel extends EntityModel<BaseNpcEntity> implements IBoneAccessor {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("sexmod", "fishing_rod_bone"), "main");

    // Este es el hueso que el renderizador buscará
    private final ModelPart tip;

    public FishingRodBoneModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        // En modelos de un solo hueso, el hijo suele ser el "root" funcional
        this.tip = root.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 16, 8);
    }

    @Override
    public void setupAnim(BaseNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Estático
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.tip.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    // ── IBoneAccessor CONTRACT ───────────────────────────────────────────────

    /**
     * REPARACIÓN: Implementamos el método que exige la interfaz.
     */
    @Override
    public ModelPart getBoneRoot() {
        return this.tip;
    }

    /**
     * Si tu interfaz todavía pide getTipBone, déjalo.
     * Si te da error de "method does not override", quítale el @Override.
     */
    public ModelPart getTipBone() {
        return this.tip;
    }
}