package com.trolmastercard.sexmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.client.model.IBoneAccessor;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NpcHandRenderer — Portado a 1.20.1.
 * * Intercepta el renderizado de la mano en primera persona para mostrar
 * * el modelo y la textura de la chica/Kobold actual.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcHandRenderer {

    private static final ResourceLocation MAP_BG = new ResourceLocation("textures/map/map_background.png");

    private boolean didCancel = false;

    // =========================================================================
    //  Interceptor del Evento (Sin Reflexión)
    // =========================================================================

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Instanciamos el handler (o usamos un singleton si prefieres)
        NpcHandRenderer renderer = new NpcHandRenderer();
        renderer.handleEvent(event, mc);
    }

    private void handleEvent(RenderHandEvent event, Minecraft mc) {
        try {
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getByPlayerUUID(mc.player.getUUID());
            if (kobold == null) return;

            int modelIdx = kobold.getHandModelIndex();
            IBoneAccessor handBone = kobold.getHandModel(modelIdx);
            ResourceLocation skinTexture = new ResourceLocation("sexmod", kobold.getHandTexturePath(modelIdx));
            Vec3i handColor = kobold.getHandColor(modelIdx);

            if (handBone == null) return;

            // ── ¡Adiós Reflexión! ──
            // En 1.20.1, el evento ya nos da estos valores directamente:
            float equippedProgress = event.getEquipProgress();
            float swingProgress = event.getSwingProgress();
            float partial = event.getPartialTick();
            int packedLight = event.getPackedLight(); // Luz del entorno real

            AbstractClientPlayer player = mc.player;
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufs = event.getMultiBufferSource();

            if (event.getHand() == InteractionHand.MAIN_HAND) {
                ItemStack mainItem = player.getMainHandItem();
                boolean isMap = mainItem.getItem() instanceof MapItem;

                if (mainItem.isEmpty() || isMap) {
                    event.setCanceled(true);
                    renderMainHand(mc, poseStack, bufs, mainItem, partial, player, equippedProgress, swingProgress, handBone, skinTexture, handColor, packedLight);
                    didCancel = true;
                } else if (didCancel) {
                    event.setCanceled(true);
                    renderMainHand(mc, poseStack, bufs, mainItem, partial, player, equippedProgress, swingProgress, handBone, skinTexture, handColor, packedLight);
                } else {
                    didCancel = false;
                }
            } else {
                ItemStack offItem = player.getOffhandItem();
                if (offItem.getItem() instanceof MapItem) {
                    event.setCanceled(true);
                    renderHandSide(mc, poseStack, bufs, InteractionHand.OFF_HAND, equippedProgress, swingProgress, offItem, handBone, skinTexture, handColor, packedLight);
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Renderizado y Matemáticas de Matriz
    // =========================================================================

    private void renderMainHand(Minecraft mc, PoseStack ps, MultiBufferSource bufs, ItemStack item, float partial, AbstractClientPlayer player, float equip, float swing, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        if (item.getItem() instanceof MapItem) {
            if (player.getOffhandItem().isEmpty()) {
                renderMap(mc, ps, bufs, item, player, swing, partial, equip, handBone, skin, color, light);
            } else {
                renderHandSide(mc, ps, bufs, InteractionHand.MAIN_HAND, equip, swing, item, handBone, skin, color, light);
            }
        } else {
            renderMainHandNoItem(mc, ps, bufs, equip, swing, handBone, skin, color, light);
        }
    }

    private void renderHandSide(Minecraft mc, PoseStack ps, MultiBufferSource bufs, InteractionHand hand, float equip, float swing, ItemStack item, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;

        ps.translate(side * 0.125F, -0.125F, 0.0F);
        if (!mc.player.isInvisible()) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(side * 10.0F));
            applyHandTransform(ps, equip, swing, hand);
            ps.translate(-0.5F, -1.1F, 0.0F);
            if (isRight) ps.translate(0.48F, 0.15F, 0.0F);
            else ps.translate(0.44F, 1.3F, 1.0F);

            renderBone(ps, bufs, handBone, skin, color, light);
            ps.popPose();
        }

        // Animación del Mapa
        ps.pushPose();
        ps.translate(side * 0.51F, -0.08F + equip * -1.2F, -0.75F);
        float swingSin = Mth.sqrt(swing);
        float swingAng = (float) Math.sin(swingSin * Math.PI);
        ps.translate(side * (-0.5F * swingAng), 0.4F * (float) Math.sin(swingSin * Math.PI * 2) - 0.3F * swingAng, -0.3F * (float) Math.sin(swing * Math.PI));
        ps.mulPose(Axis.XP.rotationDegrees(swingAng * -45.0F));
        ps.mulPose(Axis.YP.rotationDegrees(side * swingAng * -30.0F));
        renderMapQuad(mc, ps, bufs, item, light);
        ps.popPose();
    }

    private void renderMap(Minecraft mc, PoseStack ps, MultiBufferSource bufs, ItemStack item, AbstractClientPlayer player, float swing, float partial, float equip, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        float pitch = Mth.lerp(partial, player.xRotO, player.getXRot());
        float swingSin = Mth.sqrt(swing);
        ps.translate(0.0F, -(-(float) Math.sin(swing * Math.PI) * 0.2F) / 2.0F, -(float) Math.sin(swingSin * Math.PI) * 0.4F);

        float bob = mapBob(pitch);
        ps.translate(0.0F, 0.04F + (equip - 1.0F) * -1.2F + bob * -0.5F, -0.72F);
        ps.mulPose(Axis.XP.rotationDegrees(bob * -85.0F));

        renderHandArm(mc, ps, bufs, InteractionHand.MAIN_HAND, handBone, skin, color, light);
        renderHandArm(mc, ps, bufs, InteractionHand.OFF_HAND, handBone, skin, color, light);

        ps.mulPose(Axis.XP.rotationDegrees((float) Math.sin(swingSin * Math.PI) * 20.0F));
        ps.scale(2.0F, 2.0F, 2.0F);
        renderMapQuad(mc, ps, bufs, item, light);
    }

    private void renderMapQuad(Minecraft mc, PoseStack ps, MultiBufferSource bufs, ItemStack item, int light) {
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.scale(0.38F, 0.38F, 0.38F);
        ps.translate(-0.5F, -0.5F, 0.0F);
        ps.scale(0.0078125F, 0.0078125F, 0.0078125F);

        // Renderizado Tesselator moderno 1.20.1
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MAP_BG);
        Tesselator tes = Tesselator.getInstance();
        var buf = tes.getBuilder();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(ps.last().pose(), -7, 135, 0).uv(0, 1).endVertex();
        buf.vertex(ps.last().pose(), 135, 135, 0).uv(1, 1).endVertex();
        buf.vertex(ps.last().pose(), 135, -7, 0).uv(1, 0).endVertex();
        buf.vertex(ps.last().pose(), -7, -7, 0).uv(0, 0).endVertex();
        tes.end();

        if (item.getItem() instanceof MapItem mapItem) {
            var mapData = mapItem.getMapData(item, mc.level);
            if (mapData != null) {
                mc.gameRenderer.getMapRenderer().render(ps, bufs, mapData, false, light);
            }
        }
    }

    private void renderHandArm(Minecraft mc, PoseStack ps, MultiBufferSource bufs, InteractionHand hand, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(92.0F));
        ps.mulPose(Axis.XP.rotationDegrees(45.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        ps.translate(side * 0.3F, -1.1F, 0.45F);
        if (isRight) ps.translate(0.63F, 0.36F, 0.0F);
        else         ps.translate(1.6F, 0.35F, 0.0F);
        renderBone(ps, bufs, handBone, skin, color, light);
        ps.popPose();
    }

    private float mapBob(float pitch) {
        float t = Mth.clamp(1.0F - pitch / 45.0F + 0.1F, 0.0F, 1.0F);
        return -(float) Math.cos(t * Math.PI) * 0.5F + 0.5F;
    }

    private void renderMainHandNoItem(Minecraft mc, PoseStack ps, MultiBufferSource bufs, float equip, float swing, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        ps.pushPose();
        applyHandTransform(ps, equip, swing, InteractionHand.MAIN_HAND);
        renderBone(ps, bufs, handBone, skin, color, light);
        ps.popPose();
    }

    private void applyHandTransform(PoseStack ps, float equip, float swing, InteractionHand hand) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;
        float swingSin = Mth.sqrt(swing);
        ps.translate(side * (-0.3F * (float) Math.sin(swingSin * Math.PI) + 0.64F),
                0.4F * (float) Math.sin(swingSin * Math.PI * 2) - 0.6F + equip * -0.6F,
                -0.4F * (float) Math.sin(swing * Math.PI) - 0.72F);

        ps.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        ps.mulPose(Axis.YP.rotationDegrees(side * (float) Math.sin(swingSin * Math.PI) * 70.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(side * (float) Math.sin(swing * swing * Math.PI) * -20.0F));

        ps.translate(side * -1.0F, 3.6F, 3.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        ps.mulPose(Axis.XP.rotationDegrees(200.0F));
        ps.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        ps.translate(side * 5.6F, 0.0F, 0.0F);
        ps.translate(0.5F, 1.1F, 0.0F);
    }

    private void renderBone(PoseStack ps, MultiBufferSource bufs, IBoneAccessor handBone, ResourceLocation skin, Vec3i color, int light) {
        VertexConsumer consumer = bufs.getBuffer(RenderType.entitySolid(skin));
        ModelPart root = handBone.getBoneRoot();
        // Aplicamos la luz real del entorno (light) en lugar de estar siempre brillando a full
        root.render(ps, consumer, light, OverlayTexture.NO_OVERLAY,
                color.getX() / 255.0F, color.getY() / 255.0F, color.getZ() / 255.0F, 1.0F);
    }
}