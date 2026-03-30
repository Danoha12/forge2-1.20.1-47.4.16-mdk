package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GoblinEntity;
// import com.trolmastercard.sexmod.network.ModNetwork;
// import com.trolmastercard.sexmod.network.packet.GoblinCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * GoblinContextMenuScreen — Portado a 1.20.1.
 * * Menú radial de comandos rápidos para interactuar con NPCs.
 */
@OnlyIn(Dist.CLIENT)
public class GoblinContextMenuScreen extends Screen {

    static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/command.png");

    final BaseNpcEntity npc;
    final boolean isGoblin;

    // ── Animación ────────────────────────────────────────────────────────────
    float fadeIn = 0.0F;
    float leftHover = 0.0F;
    float rightHover = 0.0F;
    float topHover = 0.0F;
    float bottomHover = 0.0F;

    public GoblinContextMenuScreen(BaseNpcEntity npc) {
        super(Component.empty());
        this.npc = npc;
        this.isGoblin = (npc instanceof GoblinEntity);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        commitAction();
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // En 1.20.1, keyBindings está dentro de mc.options
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyUse.getKey().getValue() == keyCode) { // Asumiendo que usas click derecho
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Renderizado (El nuevo rey GuiGraphics) ───────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // En 1.20.1, el fondo por defecto (si lo necesitas) se dibuja con gfx.renderBackground(...)
        super.render(gfx, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Simulamos el delta de tiempo (aprox 0.1 por frame para velocidad consistente)
        float dt = 0.1F;

        fadeIn = Math.min(1.0F, fadeIn + dt);
        float scale = (float) easeBackOut(fadeIn);
        float dist = (1.0F - scale) * 100.0F;

        // Update hover
        leftHover   = Mth.clamp(leftHover   + (mouseX < cx ? 1 : -1) * dt, 0, 1);
        rightHover  = Mth.clamp(rightHover  + (mouseX > cx ? 1 : -1) * dt, 0, 1);
        topHover    = Mth.clamp(topHover    + (mouseY < cy ? 1 : -1) * dt, 0, 1);
        bottomHover = Mth.clamp(bottomHover + (mouseY > cy ? 1 : -1) * dt, 0, 1);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack ps = gfx.pose(); // Extraemos la matriz del GuiGraphics

        // ── Push outer scale ──
        ps.pushPose();
        ps.translate(cx, cy, 0.0F);
        ps.scale(scale, scale, scale);

        // Left icon
        ps.pushPose();
        ps.scale(1.0F + leftHover * 0.5F, 1.0F + leftHover * 0.5F, 1.0F);
        // gfx.blit(Textura, X, Y, U, V, Ancho, Alto)
        gfx.blit(TEXTURE, (int)(-62.0F + dist - leftHover * 15.0F), (int)(dist - 32.0F), 0, 0, 64, 64);
        gfx.blit(TEXTURE, (int)(-62.0F + dist - leftHover * 15.0F), (int)(dist - 32.0F), 64, 128, 64, 64);
        ps.popPose();

        if (isGoblin) {
            // Right icon
            ps.pushPose();
            ps.scale(1.0F - rightHover, 1.0F - rightHover, 1.0F);
            gfx.blit(TEXTURE, (int)(-2.0F - dist + rightHover * 32.0F), (int)(-dist - 32.0F), 0, 0, 64, 64);
            gfx.blit(TEXTURE, (int)(-2.0F - dist + rightHover * 32.0F), (int)(-dist - 32.0F), 0, 128, 64, 64);
            ps.popPose();

            if (rightHover > 0.0F) {
                // Top-right icon
                ps.pushPose();
                ps.scale(-1.0F + rightHover + 1.0F + topHover * 0.5F, -1.0F + rightHover + 1.0F + topHover * 0.5F, 1.0F);
                gfx.blit(TEXTURE, (int)(-2.0F - dist + topHover * 5.0F), (int)(-dist - 64.0F - topHover * 5.0F / 2.0F), 0, 0, 64, 64);
                gfx.blit(TEXTURE, (int)(-2.0F - dist + topHover * 5.0F), (int)(-dist - 64.0F - topHover * 5.0F / 2.0F), 128, 128, 64, 64);
                ps.popPose();

                // Bottom-right icon
                ps.pushPose();
                ps.scale(-1.0F + rightHover + 1.0F + bottomHover * 0.5F, -1.0F + rightHover + 1.0F + bottomHover * 0.5F, 1.0F);
                gfx.blit(TEXTURE, (int)(-2.0F - dist + bottomHover * 5.0F), (int)(-dist + bottomHover * 5.0F / 2.0F), 0, 0, 64, 64);
                gfx.blit(TEXTURE, (int)(-2.0F - dist + bottomHover * 5.0F), (int)(-dist + bottomHover * 5.0F / 2.0F), 192, 128, 64, 64);
                ps.popPose();
            }
        }

        ps.popPose();
        RenderSystem.disableBlend();
    }

    // ── Commit on close (Networking needed!) ─────────────────────────────────

    // ── Commit on close (Networking Conectado) ───────────────────────────────

    private void commitAction() {
        if (leftHover == 0.0F && rightHover == 0.0F && topHover == 0.0F && bottomHover == 0.0F) return;

        float maxH = Math.max(Math.max(leftHover, rightHover), Math.max(topHover, bottomHover));

        // Analizamos qué botón se pulsó y enviamos el paquete al servidor
        if (leftHover == maxH) {
            com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.sendToServer(
                    new com.trolmastercard.sexmod.network.packet.NpcCommandPacket(npc.getId(), com.trolmastercard.sexmod.network.packet.NpcCommandPacket.Command.START_THROWING)
            );
            return;
        }

        if (!isGoblin) return;

        if (rightHover == maxH && topHover > bottomHover) {
            com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.sendToServer(
                    new com.trolmastercard.sexmod.network.packet.NpcCommandPacket(npc.getId(), com.trolmastercard.sexmod.network.packet.NpcCommandPacket.Command.FOLLOW)
            );
        } else if (rightHover == maxH && bottomHover >= topHover) {
            com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.sendToServer(
                    new com.trolmastercard.sexmod.network.packet.NpcCommandPacket(npc.getId(), com.trolmastercard.sexmod.network.packet.NpcCommandPacket.Command.STOP_FOLLOW)
            );
        }
    }

    // ── Helper Matemático ────────────────────────────────────────────────────
    private double easeBackOut(double t) {
        double s = 1.70158D;
        double s2 = s + 1.0D;
        return 1.0D + s2 * Math.pow(t - 1.0D, 3.0D) + s * Math.pow(t - 1.0D, 2.0D);
    }
}