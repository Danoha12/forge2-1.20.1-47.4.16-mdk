package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * WispFaceModel — Portado a 1.20.1.
 * * Un pequeño modelo "legacy" de 5 partes usado como prop facial.
 * * Jerarquía modernizada aprovechando PartDefinition.
 */
public class WispFaceModel extends EntityModel<Entity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("sexmod", "wisp_face"), "main");

    private final ModelPart body;
    private final ModelPart eyeRight;
    private final ModelPart eyeLeft;
    private final ModelPart snout;
    private final ModelPart jaw;

    public WispFaceModel(ModelPart root) {
        this.body = root.getChild("body");
        this.eyeRight = root.getChild("eye_right");
        this.eyeLeft = root.getChild("eye_left");
        this.snout = root.getChild("snout");
        this.jaw = root.getChild("jaw");
    }

    // ── Fábrica de Capas (LayerDefinition) ───────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- body (a) ----
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-3.0F, 17.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- eye_right (d) ----
        root.addOrReplaceChild("eye_right",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(1.3F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- eye_left (e) ----
        root.addOrReplaceChild("eye_left",
                CubeListBuilder.create()
                        .texOffs(32, 4)
                        .addBox(-3.3F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- snout (c) ----
        root.addOrReplaceChild("snout",
                CubeListBuilder.create()
                        .texOffs(32, 8)
                        .addBox(-1.0F, 21.0F, -3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ---- jaw (b) — pivote padre y 4 segmentos rotados ----
        PartDefinition jaw = root.addOrReplaceChild("jaw",
                CubeListBuilder.create(),
                PartPose.offset(-0.5F, 0.0F, 0.1F));

        // Segment 1
        jaw.addOrReplaceChild("jaw_seg1",
                CubeListBuilder.create()
                        .texOffs(10, 11)
                        .addBox(-2.5F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 20.7406F, 4.0504F, 1.0908F, 0.0F, 0.0F));

        // Segment 2
        jaw.addOrReplaceChild("jaw_seg2",
                CubeListBuilder.create()
                        .texOffs(10, 11)
                        .addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 19.9214F, 3.4768F, 0.6109F, 0.0F, 0.0F));

        // Segment 3
        jaw.addOrReplaceChild("jaw_seg3",
                CubeListBuilder.create()
                        .texOffs(10, 11)
                        .addBox(-4.0F, 0.0F, 0.075F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 19.0074F, 3.0643F, 0.3491F, 0.0F, 0.0F));

        // Segment 4
        jaw.addOrReplaceChild("jaw_seg4",
                CubeListBuilder.create()
                        .texOffs(10, 11)
                        .addBox(-3.0F, -1.0F, -0.5F, 7.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 17.925F, 3.5F, 0.1309F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Sin animación — malla estática
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        // Al renderizar el pivote 'jaw', se renderizarán automáticamente sus 4 segmentos hijos
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyeRight.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyeLeft.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.snout.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.jaw.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}