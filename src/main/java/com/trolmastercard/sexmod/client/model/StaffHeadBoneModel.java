package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor; // Asegúrate de la ruta correcta
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
 * StaffHeadBoneModel — Portado a 1.20.1.
 * * Modelo "legacy" de dos partes usado como el cabezal del ítem Bastón.
 * * Implementa IBoneAccessor para que el renderizador de la mano pueda obtener su raíz.
 */
public class StaffHeadBoneModel extends EntityModel<Entity> implements IBoneAccessor {

    private final ModelPart rootPart;
    private final ModelPart secondaryPart;

    public StaffHeadBoneModel(ModelPart root) {
        this.rootPart = root.getChild("root");
        this.secondaryPart = root.getChild("secondary");
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pivote raíz: desplazamiento de (-5.0, 1.5708, 0.0)
        PartDefinition rootDef = root.addOrReplaceChild("root",
                CubeListBuilder.create(),
                PartPose.offset(-5.0F, 1.5708F, 0.0F)
        );

        // Hijo rotado: pivote en (-1, -3, 1), rotado 90 grados en Y, caja de 2x6x2
        rootDef.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // 1.20.1: Flotantes explícitos
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, -3.0F, 1.0F,
                        0.0F, (float) Math.PI / 2.0F, 0.0F)
        );

        // Pivote secundario vacío en el origen
        root.addOrReplaceChild("secondary",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── EntityModel ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Sin animación por tick; controlado externamente
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.rootPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.secondaryPart.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── IBoneAccessor ────────────────────────────────────────────────────────

    // 🚨 CORREGIDO: Usando el estándar getRootBone() de tus otras clases
    @Override
    public ModelPart getRootBone() {
        return this.rootPart;
    }
}