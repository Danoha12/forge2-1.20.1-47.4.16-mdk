package com.trolmastercard.sexmod.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.UUID;

/**
 * ElytraLayer — Portado a 1.20.1 / GeckoLib 4.
 * * Capa que dibuja una Elytra en la espalda del NPC si la tiene equipada.
 */
public class ElytraLayer<T extends NpcInventoryEntity> extends GeoRenderLayer<T> {

    private static final ResourceLocation ELYTRA_TEXTURE = new ResourceLocation("textures/entity/elytra.png");
    private final ElytraModel<LivingEntity> elytraModel;

    public ElytraLayer(GeoEntityRenderer<T> entityRendererIn) {
        super(entityRendererIn);
        // Cargamos el modelo nativo de la Elytra de Minecraft
        this.elytraModel = new ElytraModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
    }

    // ── Renderizado Principal (Firma Oficial de GeckoLib 4) ──────────────────

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        // 1. Verificar si tiene la Elytra equipada
        ItemStack elytraStack = animatable.getClothingItem(NpcInventoryEntity.ELYTRA_SLOT_INDEX);
        if (!elytraStack.is(Items.ELYTRA)) return;

        // 2. Resolver a quién le copiamos la animación de vuelo
        LivingEntity animTarget = animatable;
        if (animatable instanceof PlayerKoboldEntity pkEntity) {
            UUID playerUUID = pkEntity.getBoundPlayerUUID();
            if (playerUUID != null) {
                Player player = animatable.level().getPlayerByUUID(playerUUID);
                if (player != null) animTarget = player;
            }
        }

        poseStack.pushPose();

        // 3. Ajuste de matriz (Las coordenadas originales de 1.12.2)
        poseStack.translate(0.0D, 0.0D, 0.125D); // Un ligero empuje hacia atrás para que no atraviese el pecho
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // 4. Calcular animación
        this.elytraModel.setupAnim(animTarget, 0.0F, 0.0F, animatable.tickCount + partialTick, 0.0F, 0.0F);

        // 5. Renderizar al buffer (¡Usando la luz del entorno!)
        VertexConsumer elytraConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ELYTRA_TEXTURE));
        this.elytraModel.renderToBuffer(poseStack, elytraConsumer,
                packedLight, // <--- Usamos packedLight, no FULL_BRIGHT
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}