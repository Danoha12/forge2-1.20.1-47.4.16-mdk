package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.IBoneAccessor;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NpcHandRenderer - ported from cn.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * @OnlyIn(CLIENT) event subscriber that intercepts {@link RenderHandEvent}
 * when the local player has a corresponding {@link PlayerKoboldEntity} that provides
 * a custom hand model.
 *
 * The renderer replaces the vanilla hand rendering with the NPC's bone model
 * (retrieved via {@link IBoneAccessor}) textured with the NPC skin.
 *
 * Fields:
 *   f = mc               (Minecraft instance)
 *   g = equippedProgress (swing animation factor; original 2.0F means "hidden")
 *   c = didCancel        (was the event cancelled by this handler last frame?)
 *   d = handBone         (IBoneAccessor - the model part provider)
 *   h = skinTexture      (ResourceLocation of NPC hand skin)
 *   b = handColor        (Vec3i RGB color for the hand)
 *   a = swingProgress    (float - item swing)
 *
 * Method mapping:
 *   a(RenderSpecificHandEvent) - onRenderHand(RenderHandEvent)
 *   a(ItemStack, float, AbstractClientPlayer, float, float) - renderMainHand(...)
 *   a(EnumHandSide, float, float, ItemStack) - renderItemInHand(...)
 *   a(ItemStack, AbstractClientPlayer, float, float) - renderMap(...)
 *   a(ItemStack) - renderItemStack(...)
 *   a(EnumHandSide) - renderHandArm(...)
 *   a(float) - mapBob(...)
 *   a(float, float) - renderMainHandNoItem(...)
 *   a(float, float, EnumHandSide) - applyHandTransform(...)
 *
 * In 1.12.2:
 *   RenderSpecificHandEvent         - RenderHandEvent
 *   EnumHandSide.RIGHT/LEFT         - same
 *   GlStateManager.func_179109_b    - poseStack.translate(...)
 *   GlStateManager.func_179094_E    - poseStack.pushPose()
 *   GlStateManager.func_179121_F    - poseStack.popPose()
 *   GlStateManager.func_179114_b    - poseStack.mulPose(axis-angle - Quaternionf)
 *   GlStateManager.func_179152_a    - poseStack.scale(...)
 *   GlStateManager.func_179124_c    - color via VertexConsumer (GL glColor4f no longer exists)
 *   GlStateManager.func_179129_p    - renderSystem enableAlphaTest (removed in 1.20.1)
 *   GlStateManager.func_179084_k    - renderSystem enableBlend
 *   GlStateManager.func_179089_o    - renderSystem disableBlend
 *   GlStateManager.func_179140_f    - renderSystem disableCull (simplified)
 *   GlStateManager.func_179145_e    - renderSystem enableCull
 *   Tessellator.func_178181_a()     - Tesselator.getInstance()
 *   BufferBuilder.func_181668_a     - bufferBuilder.begin(mode, format)
 *   bufferBuilder.func_181662_b     - bufferBuilder.vertex(...)
 *   bufferBuilder.func_187315_a     - .uv(...)
 *   bufferBuilder.func_181675_d     - .endVertex()
 *   tessellator.func_78381_a()      - Tesselator.getInstance().end()
 *   field_71460_t.func_147701_i().func_148250_a(mapData, bool)
 *     - mc.gameRenderer.getMapRenderer().render(poseStack, buffer, mapData, bool, light)
 *   ModelPart.func_78785_a(scale)   - modelPart.render(poseStack, consumer, light, overlay)
 *   ItemRenderer.field_187470_g / field_187469_f  - prevEquippedProgressMainHand / equippedProgressMainHand
 *     (accessed via ObfuscationReflectionHelper or injected via event fields in 1.20.1)
 *   ei.d(UUID)  - PlayerKoboldEntity.getByPlayerUUID(UUID)
 *   ei.ah()     - kobold.getHandModelIndex()
 *   ei.c(int)   - kobold.getHandTexturePath(int)  - String subpath
 *   ei.a(int)   - kobold.getHandModel(int)        - IBoneAccessor
 *   ei.b(int)   - kobold.getHandColor(int)        - Vec3i
 *   cn.C()      - NpcHandRenderer.checkIsModded()  - static bool (reflection helper)
 */
@OnlyIn(Dist.CLIENT)
public class NpcHandRenderer {

    private Minecraft mc;
    private float equippedProgress = 2.0F;
    private boolean didCancel = false;
    private IBoneAccessor handBone;
    private ResourceLocation skinTexture;
    private Vec3i handColor;
    private float swingProgress = 0.0F;

    private static final ResourceLocation MAP_BG =
        new ResourceLocation("textures/map/map_background.png");

    // =========================================================================
    //  Event handler  (original: cn.a(RenderSpecificHandEvent))
    // =========================================================================

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        try {
            mc = Minecraft.getInstance();

            // Look up the PlayerKobold for the local player
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getByPlayerUUID(
                mc.player.getUUID());
            if (kobold == null) return;

            int modelIdx = kobold.getHandModelIndex();
            handBone     = kobold.getHandModel(modelIdx);
            skinTexture  = new ResourceLocation("sexmod", kobold.getHandTexturePath(modelIdx));
            handColor    = kobold.getHandColor(modelIdx);

            if (handBone == null) {
                System.out.println("HAND IS NULL uwu did you forget to assign this girl a hand owo?");
                return;
            }

            // Compute equipped progress (item swing)
            float prevProg = 0.0F, curProg = 0.0F;
            try {
                var itemRenderer = mc.getItemInHandRenderer();
                prevProg = net.minecraftforge.fml.util.ObfuscationReflectionHelper
                    .getPrivateValue(net.minecraft.client.renderer.ItemInHandRenderer.class,
                        itemRenderer, "f_109307_");  // prevEquippedProgressMainHand
                curProg  = net.minecraftforge.fml.util.ObfuscationReflectionHelper
                    .getPrivateValue(net.minecraft.client.renderer.ItemInHandRenderer.class,
                        itemRenderer, "f_109308_");  // equippedProgressMainHand
            } catch (Exception e) {
                System.out.println("couldnt do the reflection thingy");
            }
            float partial = event.getPartialTick();
            equippedProgress = 2.0F - (prevProg + (curProg - prevProg) * partial);

            AbstractClientPlayer player = mc.player;
            swingProgress = player.getAttackAnim(partial);

            // Color: from handColor Vec3i (RGB 0-255)
            float cr = handColor.getX() / 255.0F;
            float cg = handColor.getY() / 255.0F;
            float cb = handColor.getZ() / 255.0F;

            // Get PoseStack and buffers from event
            PoseStack poseStack    = event.getPoseStack();
            MultiBufferSource bufs = event.getMultiBufferSource();

            // MAIN_HAND
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                ItemStack mainItem = player.getMainHandItem();
                boolean isMap = !mainItem.isEmpty() && mainItem.getItem() instanceof MapItem;

                if (mainItem.isEmpty() || isMap) {
                    event.setCanceled(true);
                    renderMainHand(poseStack, bufs, mainItem, partial, player,
                        equippedProgress, swingProgress);
                    didCancel = true;
                } else if (curProg < prevProg && didCancel) {
                    event.setCanceled(true);
                    renderMainHand(poseStack, bufs, mainItem, partial, player,
                        equippedProgress, swingProgress);
                } else {
                    didCancel = false;
                }
            } else {
                // OFF_HAND - only if holding a map in OFF_HAND
                ItemStack offItem = player.getOffhandItem();
                if (offItem.getItem() instanceof MapItem) {
                    event.setCanceled(true);
                    renderHandSide(poseStack, bufs, net.minecraft.world.InteractionHand.OFF_HAND,
                        equippedProgress - 1.0F, swingProgress, offItem);
                }
            }

        } catch (Exception e) {
            // Swallow renderer errors to avoid crashing
        }
    }

    // =========================================================================
    //  Render methods (simplified - full GL transforms converted to PoseStack)
    // =========================================================================

    void renderMainHand(PoseStack ps, MultiBufferSource bufs,
                        ItemStack item, float partial,
                        AbstractClientPlayer player, float equip, float swing) {
        if (item.getItem() instanceof MapItem) {
            if (player.getOffhandItem().isEmpty()) {
                renderMap(ps, bufs, item, player, swing, partial);
            } else {
                renderHandSide(ps, bufs, InteractionHand.MAIN_HAND,
                    equip - 1.0F, swing, item);
            }
        } else {
            renderMainHandNoItem(ps, bufs, swing, partial);
        }
    }

    void renderHandSide(PoseStack ps, MultiBufferSource bufs,
                        InteractionHand hand, float equip, float swing, ItemStack item) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;

        ps.translate(side * 0.125F, -0.125F, 0.0F);
        if (!mc.player.isInvisible()) {
            ps.pushPose();
            ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(side * 10.0F)));
            applyHandTransform(ps, equip, swing, isRight ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
            ps.translate(-0.5F, -1.1F, 0.0F);
            if (isRight) ps.translate(0.48F, 0.15F, 0.0F);
            else ps.translate(0.44F, 1.3F, 1.0F);
            mc.getTextureManager().bindForSetup(skinTexture);
            renderBone(ps, bufs);
            ps.popPose();
        }

        // Render map quad
        ps.pushPose();
        ps.translate(side * 0.51F, -0.08F + equip * -1.2F, -0.75F);
        float swingSin = Mth.sqrt(swing);
        float swingAng = (float) Math.sin(swingSin * Math.PI);
        float f4 = -0.5F * swingAng;
        float f5 = 0.4F * (float) Math.sin(swingSin * Math.PI * 2);
        float f6 = -0.3F * (float) Math.sin(swing * Math.PI);
        ps.translate(side * f4, f5 - 0.3F * swingAng, f6);
        ps.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(swingAng * -45.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(side * swingAng * -30.0F)));
        renderItemStack(ps, bufs, item);
        ps.popPose();
    }

    void renderMap(PoseStack ps, MultiBufferSource bufs, ItemStack item,
                   AbstractClientPlayer player, float swing, float partial) {
        float pitch   = player.xRotO + (player.getXRot() - player.xRotO) * partial;
        float swingSin = Mth.sqrt(swing);
        float f3 = -(float) Math.sin(swing * Math.PI) * 0.2F;
        float f4 = -(float) Math.sin(swingSin * Math.PI) * 0.4F;
        ps.translate(0.0F, -f3 / 2.0F, f4);
        float bob = mapBob(pitch);
        ps.translate(0.0F, 0.04F + (equippedProgress - 1.0F) * -1.2F + bob * -0.5F, -0.72F);
        ps.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(bob * -85.0F)));
        renderHandArm(ps, bufs, InteractionHand.MAIN_HAND);
        renderHandArm(ps, bufs, InteractionHand.OFF_HAND);
        float f6 = (float) Math.sin(swingSin * Math.PI);
        ps.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(f6 * 20.0F)));
        ps.scale(2.0F, 2.0F, 2.0F);
        renderItemStack(ps, bufs, item);
    }

    void renderItemStack(PoseStack ps, MultiBufferSource bufs, ItemStack item) {
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(180)));
        ps.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(180)));
        ps.scale(0.38F, 0.38F, 0.38F);
        mc.getTextureManager().bindForSetup(MAP_BG);
        var tes = com.mojang.blaze3d.systems.RenderSystem.renderThreadTesselator();
        var buf = tes.getBuilder();
        ps.translate(-0.5F, -0.5F, 0.0F);
        ps.scale(0.0078125F, 0.0078125F, 0.0078125F);
        buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        buf.vertex(ps.last().pose(), -7, 135, 0).uv(0, 1).endVertex();
        buf.vertex(ps.last().pose(), 135, 135, 0).uv(1, 1).endVertex();
        buf.vertex(ps.last().pose(), 135, -7, 0).uv(1, 0).endVertex();
        buf.vertex(ps.last().pose(), -7, -7, 0).uv(0, 0).endVertex();
        tes.end();

        if (item.getItem() instanceof MapItem mapItem) {
            var mapData = mapItem.getMapData(item, mc.level);
            if (mapData != null) mc.gameRenderer.getMapRenderer().render(
                ps, bufs, mapData, false, 0xF000F0);
        }
    }

    private void renderHandArm(PoseStack ps, MultiBufferSource bufs, InteractionHand hand) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;
        ps.pushPose();
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(92.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(45.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(side * -41.0F)));
        ps.translate(side * 0.3F, -1.1F, 0.45F);
        if (isRight) ps.translate(0.63F, 0.36F, 0.0F);
        else         ps.translate(1.6F, 0.35F, 0.0F);
        mc.getTextureManager().bindForSetup(skinTexture);
        renderBone(ps, bufs);
        ps.popPose();
    }

    private float mapBob(float pitch) {
        float t = 1.0F - pitch / 45.0F + 0.1F;
        t = Mth.clamp(t, 0.0F, 1.0F);
        return -(float) Math.cos(t * Math.PI) * 0.5F + 0.5F;
    }

    void renderMainHandNoItem(PoseStack ps, MultiBufferSource bufs, float swing, float partial) {
        ps.pushPose();
        applyHandTransform(ps, equippedProgress, swing, InteractionHand.MAIN_HAND);
        mc.getTextureManager().bindForSetup(skinTexture);
        renderBone(ps, bufs);
        ps.popPose();
    }

    private void applyHandTransform(PoseStack ps, float equip, float swing, InteractionHand hand) {
        boolean isRight = (hand == InteractionHand.MAIN_HAND);
        float side = isRight ? 1.0F : -1.0F;
        float swingSin = Mth.sqrt(swing);
        float f3 = -0.3F * (float) Math.sin(swingSin * Math.PI);
        float f4 =  0.4F * (float) Math.sin(swingSin * Math.PI * 2);
        float f5 = -0.4F * (float) Math.sin(swing * Math.PI);
        ps.translate(side * (f3 + 0.64F), f4 - 0.6F + equip * -0.6F, f5 - 0.72F);
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(side * 45.0F)));
        float f6 = (float) Math.sin(swing * swing * Math.PI);
        float f7 = (float) Math.sin(swingSin * Math.PI);
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(side * f7 * 70.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(side * f6 * -20.0F)));
        ps.translate(side * -1.0F, 3.6F, 3.5F);
        ps.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(side * 120.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(200.0F)));
        ps.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(side * -135.0F)));
        ps.translate(side * 5.6F, 0.0F, 0.0F);
        ps.translate(0.5F, 1.1F, 0.0F);
    }

    private void renderBone(PoseStack ps, MultiBufferSource bufs) {
        if (handBone == null) return;
        VertexConsumer consumer = bufs.getBuffer(RenderType.entitySolid(skinTexture));
        ModelPart root = handBone.getBoneRoot();
        root.render(ps, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY,
            handColor.getX() / 255.0F, handColor.getY() / 255.0F,
            handColor.getZ() / 255.0F, 1.0F);
    }
}
