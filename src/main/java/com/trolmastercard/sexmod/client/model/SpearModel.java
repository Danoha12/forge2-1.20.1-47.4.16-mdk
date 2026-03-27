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
 * SpearModel - ported from a5.class (and a7.class which is identical)
 * (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Single-bone prop model used for spear / tribe weapon entities.
 * Implements {@link com.trolmastercard.sexmod.client.model.IBoneAccessor}
 * so the rendering system can read bone rotations at runtime.
 *
 * In 1.12.2:
 *   - Extended {@code ModelBase}
 *   - Used {@code ModelRenderer} + {@code ModelBox}
 *   - Pivoted at (-5, 2.5, 0), box at (-2,-6,0) size(2,6,2) texOff(0,0)
 *
 * In 1.20.1:
 *   - Extends {@code EntityModel<T>} (no generic bound needed for props)
 *   - Uses {@code LayerDefinition} / {@code CubeListBuilder} / {@code ModelPart}
 *
 * Register the layer via:
 * <pre>
 *   EntityModelLayerRegistry.registerModelLayer(SpearModel.LAYER, SpearModel::createBodyLayer);
 * </pre>
 */
public class SpearModel<T extends Entity> extends EntityModel<T>
        implements IBoneAccessor {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation("sexmod", "spear"), "main");

    private final ModelPart root;

    public SpearModel(ModelPart root) {
        this.root = root.getChild("spear");
    }

    /** Factory for {@code EntityModelLayerRegistry}. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("spear",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -6.0F, 0.0F, 2, 6, 2, new CubeDeformation(0.0F)),
            PartPose.offset(-5.0F, 2.5F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    // =========================================================================
    //  EntityModel
    // =========================================================================

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Static prop - no animation
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // =========================================================================
    //  IBoneAccessor
    // =========================================================================

    @Override
    public void setBoneRotation(ModelPart part, float x, float y, float z) {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    @Override
    public ModelPart getRootBone() {
        return root;
    }
}
