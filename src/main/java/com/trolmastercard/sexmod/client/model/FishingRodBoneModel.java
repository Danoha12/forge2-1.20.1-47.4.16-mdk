package com.trolmastercard.sexmod.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * FishingRodBoneModel — Portado a 1.20.1.
 * * Modelo mínimo de un solo hueso usado como ancla para el hilo de pescar/tentáculo.
 */
public class FishingRodBoneModel extends BaseNpcModel<Entity> implements IBoneAccessor {

    // Registramos la capa para poder instanciarla en tu ClientSetup
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("sexmod", "fishing_rod_bone"), "main");

    private final ModelPart tipBone;

    public FishingRodBoneModel(ModelPart root) {
        super(root);
        this.tipBone = root.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // El hueso que servirá como ancla para el FishingLineSegmentRenderer
        root.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 16, 8);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Hueso estático / movido externamente
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    // ¡setBoneRotation ya viene gratis por la interfaz!

    @Override
    public ModelPart getTipBone() {
        return this.tipBone;
    }
}