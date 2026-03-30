package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.client.ClientSetup;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * NpcInteractScreen — Portado a 1.20.1.
 * * Menú radial de acción rápida. Aparece al mantener presionada la tecla de Interacción.
 * * Se cierra y ejecuta la acción seleccionada al soltar la tecla.
 */
@OnlyIn(Dist.CLIENT)
public class NpcInteractScreen extends Screen {

    static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/command.png");

    // ── Estado de Animación ──────────────────────────────────────────────────
    private float fadeIn = 0.0F;

    private float left = 0.0F;
    private float right = 0.0F;
    private float upper = 0.0F;
    private float lower = 0.0F;

    private final BaseNpcEntity npc;
    private final boolean isGoblin;

    public NpcInteractScreen(BaseNpcEntity npc) {
        super(Component.empty());
        this.npc = npc;
        this.isGoblin = (npc instanceof GoblinEntity);
    }

    // ── Configuración de Pantalla ────────────────────────────────────────────

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * En 1.20.1, usamos keyReleased para detectar cuándo el jugador suelta la tecla 'G'.
     * Al soltarla, cerramos la pantalla y ejecutamos la acción.
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ClientSetup.KEY_INTERACT_GOBLIN.matches(keyCode, scanCode)) {
            commitAction();
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    // ── Renderizado (GuiGraphics) ────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();

        // 1. Animación de Entrada (Fade In con Efecto Rebote)
        float delta = mc.getDeltaFrameTime() / 5.0F;
        fadeIn = Math.min(1.0F, fadeIn + delta);
        float ease = (float) easeBackOut(fadeIn);
        float offset = (1.0F - ease) * 100.0F;

        // 2. Lógica de Hover (Acumuladores visuales según la posición del ratón)
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Ajustamos la sensibilidad de la selección
        left = Mth.clamp(left + (mouseX < cx - 20 ? 1 : -1) * delta, 0, 1);
        right = Mth.clamp(right + (mouseX > cx + 20 ? 1 : -1) * delta, 0, 1);
        upper = Mth.clamp(upper + (mouseY < cy - 20 ? 1 : -1) * delta, 0, 1);
        lower = Mth.clamp(lower + (mouseY > cy + 20 ? 1 : -1) * delta, 0, 1);

        // 3. Preparación de OpenGL
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(ease, ease, ease);

        // ── DIBUJADO DE BOTONES ──────────────────────────────────────────────

        // Zona Izquierda: Acción Base (Teletransporte / Lanzar Perla)
        graphics.pose().pushPose();
        float scaleL = 1.0F + (left * 0.5F);
        graphics.pose().scale(scaleL, scaleL, 1.0F);
        int lx = (int) (-62.0F + offset - (left * 15.0F));
        int ly = (int) (offset - 32.0F);

        graphics.blit(TEXTURE, lx, ly, 0, 0, 64, 64);      // Fondo del botón
        graphics.blit(TEXTURE, lx, ly, 64, 128, 64, 64);   // Icono del botón
        graphics.pose().popPose();

        // Zonas Derecha (Exclusivo Goblin / Variantes Especiales)
        if (isGoblin) {
            // Animación base del panel derecho
            graphics.pose().pushPose();
            float scaleR = 1.0F - right; // Se encoge cuando el ratón se mueve a la derecha
            graphics.pose().scale(scaleR, scaleR, 1.0F);
            int rx = (int) (-2.0F - offset + (right * 32.0F));
            int ry = (int) (-offset - 32.0F);

            graphics.blit(TEXTURE, rx, ry, 0, 0, 64, 64);      // Fondo
            graphics.blit(TEXTURE, rx, ry, 0, 128, 64, 64);    // Icono
            graphics.pose().popPose();

            // Desplegar sub-opciones cuando el ratón está a la derecha
            if (right > 0.1F) {
                // Sub-Opción Superior (Acción C)
                graphics.pose().pushPose();
                float scaleUp = right + (upper * 0.5F);
                graphics.pose().scale(scaleUp, scaleUp, 1.0F);
                int ux = (int) (-2.0F - offset + (upper * 5.0F));
                int uy = (int) (-offset - 64.0F - (upper * 2.5F));

                graphics.blit(TEXTURE, ux, uy, 0, 0, 64, 64);
                graphics.blit(TEXTURE, ux, uy, 128, 128, 64, 64);
                graphics.pose().popPose();

                // Sub-Opción Inferior (Acción B)
                graphics.pose().pushPose();
                float scaleDn = right + (lower * 0.5F);
                graphics.pose().scale(scaleDn, scaleDn, 1.0F);
                int dx = (int) (-2.0F - offset + (lower * 5.0F));
                int dy = (int) (-offset + (lower * 2.5F));

                graphics.blit(TEXTURE, dx, dy, 0, 0, 64, 64);
                graphics.blit(TEXTURE, dx, dy, 192, 128, 64, 64);
                graphics.pose().popPose();
            }
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    // ── Lógica de Selección ──────────────────────────────────────────────────

    private void commitAction() {
        // Tolerancia: Si el jugador soltó la tecla sin mover mucho el ratón, no hacemos nada.
        if (left < 0.5F && right < 0.5F) return;

        // Selección Izquierda
        if (left >= 0.5F) {
            // Asumiendo que getSexPartner existe en tu BaseNpcEntity
            if (npc.getSexPartner() != null) return;
            // En 1.20.1, si queremos forzar una animación desde el cliente al servidor,
            // usualmente enviamos un paquete. Si setAnimState está sincronizado por NBT, esto bastará.
            npc.setAnimState(AnimState.START_THROWING);
            return;
        }

        // Selección Derecha (Goblin)
        if (isGoblin && right >= 0.5F) {
            GoblinEntity goblin = (GoblinEntity) npc;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            UUID playerId = player.getUUID();

            if (upper > lower) {
                goblin.commitActionC(playerId); // Acción superior
            } else {
                goblin.commitActionB(playerId); // Acción inferior
            }
        }
    }

    // ── Función Matemática ───────────────────────────────────────────────────

    private double easeBackOut(double t) {
        double s = 1.70158D;
        double s2 = s + 1.0D;
        return 1.0D + s2 * Math.pow(t - 1.0D, 3.0D) + s * Math.pow(t - 1.0D, 2.0D);
    }
}