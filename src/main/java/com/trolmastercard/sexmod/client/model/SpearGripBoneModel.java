package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor; // Asegúrate de que la ruta sea correcta
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
 * SpearGripBoneModel — Portado a 1.20.1.
 * * Modelo minúsculo de una sola parte usado como hueso de agarre para lanzas.
 * * Geometría original: una caja [-2, -6, 0] tamaño 2x6x2, pivote en (-5, 2.5, 0).
 */
public class SpearGripBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    public static final String PART_NAME = "grip";

    // Puedes ignorar guardar el 'root' si nunca lo vas a usar,
    // pero lo dejamos por si acaso lo necesitas a futuro.
    private final ModelPart root;
    private final ModelPart grip;

    public SpearGripBoneModel(ModelPart root) {
        this.root = root;
        this.grip = root.getChild(PART_NAME);
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild(
                PART_NAME,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // 🚨 1.20.1: Usamos CubeDeformation explícito (o lo omitimos)
                        .addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── EntityModel ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Sin animación por tick; el control es externo vía IBoneAccessor
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.grip.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    @Override
    public void setBoneRotation(ModelPart bone, float rx, float ry, float rz) {
        bone.xRot = rx;
        bone.yRot = ry;
        bone.zRot = rz;
    }

    @Override
    public ModelPart getBone() {
        return this.grip;
    }
}