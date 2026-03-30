package com.trolmastercard.sexmod.client.gui; // Ajusta a tu paquete de GUIs

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.network.ModNetwork; // Asumiendo tu clase de red
import com.trolmastercard.sexmod.network.packet.*; // Asumiendo tus paquetes
// import com.trolmastercard.sexmod.client.renderer.BlockHighlightRenderer; // Descomenta cuando lo tengas
// import com.trolmastercard.sexmod.client.renderer.StaffItemRenderer; // Descomenta cuando lo tengas
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * StaffCommandScreen — Portado a 1.20.1.
 * * Menú radial para el bastón de comandos. Se activa al mantener presionado el ítem.
 * * Utiliza GuiGraphics (nuevo estándar 1.20+) para el renderizado.
 */
@OnlyIn(Dist.CLIENT)
public class StaffCommandScreen extends Screen {

    static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/command.png");

    public static boolean tribeFollowEnabled = false;

    // ── Estado ───────────────────────────────────────────────────────────────

    private float fadeIn = 0.0F;

    // Acumuladores de Hover por cuadrante
    private float tlHover = 0.0F;  // Cama/Cofre
    private float blHover = 0.0F;  // Modo Seguir
    private float trHover = 0.0F;  // Código de Modelo
    private float brHover = 0.0F;  // Talar/Empujar

    private final BlockState lookedAtState;
    private final BlockPos lookedAtPos;
    private final Direction lookedAtFace;

    public StaffCommandScreen() {
        super(Component.empty());
        Minecraft mc = Minecraft.getInstance();

        BlockPos pos = null;
        Direction face = Direction.NORTH;

        if (mc.hitResult instanceof BlockHitResult bhr) {
            pos = bhr.getBlockPos();
            face = bhr.getDirection();
        }

        this.lookedAtPos = pos;
        this.lookedAtFace = face;
        this.lookedAtState = (mc.level != null && pos != null) ? mc.level.getBlockState(pos) : null;
    }

    // ── Screen Overrides ─────────────────────────────────────────────────────

    @Override
    public boolean isPauseScreen() {
        return false; // No pausa el juego al abrirse
    }

    @Override
    public void onClose() {
        // Ejecutar acción del cuadrante más iluminado al soltar el botón/cerrar
        List<Float> vals = Arrays.asList(tlHover, blHover, trHover, brHover);
        float max = Collections.max(vals);

        if (max > 0.0F) {
            if (max == tlHover) highlightBedOrChest();
            else if (max == blHover) toggleFollowMode();
            else if (max == trHover) openModelCode();
            else if (max == brHover) cutTreeOrFurniture();
        }

        super.onClose();
    }

    // 🚨 1.20.1: Usamos GuiGraphics en lugar de PoseStack
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        int cx = this.width / 2;
        int cy = this.height / 2;

        // ── Animación (Fade-in) ──────────────────────────────────────────────
        // Usamos partialTick o delta fijo para consistencia
        this.fadeIn = Math.min(1.0F, this.fadeIn + (mc.getDeltaFrameTime() / 5.0F));
        float ease = (float) easeBackOut(this.fadeIn);
        float dist = (1.0F - ease) * 100.0F;

        // ── Lógica de Hover (Ratón) ──────────────────────────────────────────
        float dt = mc.getDeltaFrameTime();
        // Mapeo original invertido en el código: tl = abajo-derecha, bl = abajo-izquierda, tr = arriba-derecha, br = arriba-izquierda
        // Mantuve tu lógica matemática intacta para no romper tus posiciones
        tlHover = Mth.clamp(tlHover + (mouseX < cx && mouseY > cy ? 1 : -1) * dt, 0, 1);
        blHover = Mth.clamp(blHover + (mouseX < cx && mouseY < cy ? 1 : -1) * dt, 0, 1);
        trHover = Mth.clamp(trHover + (mouseX > cx && mouseY > cy ? 1 : -1) * dt, 0, 1);
        brHover = Mth.clamp(brHover + (mouseX > cx && mouseY < cy ? 1 : -1) * dt, 0, 1);

        // ── Renderizado ──────────────────────────────────────────────────────
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 🚨 1.20.1: GuiGraphics maneja el texturado
        // Dibujamos los 4 cuadrantes
        drawQuadIcon(guiGraphics, cx, cy,
                -62.0F + dist - blHover * 15.0F, -62.0F + dist - blHover * 15.0F,
                0, 0, 64, 64, ease);

        drawQuadIcon(guiGraphics, cx, cy,
                -2.0F - dist + trHover * 15.0F, -62.0F + dist - trHover * 15.0F,
                64, 0, 64, 64, ease);

        drawQuadIcon(guiGraphics, cx, cy,
                -62.0F + dist - tlHover * 15.0F, -2.0F - dist + tlHover * 15.0F,
                0, 64, 64, 64, ease);

        drawQuadIcon(guiGraphics, cx, cy,
                -2.0F - dist + brHover * 15.0F, -2.0F - dist + brHover * 15.0F,
                64, 64, 64, 64, ease);

        RenderSystem.disableBlend();
    }

    private void drawQuadIcon(GuiGraphics guiGraphics, int cx, int cy, float relX, float relY,
                              int uOff, int vOff, int w, int h, float alpha) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        // 🚨 1.20.1: blit de GuiGraphics. Los dos últimos parámetros son el tamaño del atlas (256x256 por defecto)
        guiGraphics.blit(TEXTURE, cx + (int) relX, cy + (int) relY, uOff, vOff, w, h);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Restaurar color
    }

    private double easeBackOut(double t) {
        double s = 1.70158;
        return 1.0 + (s + 1.0) * Math.pow(t - 1.0, 3.0) + s * Math.pow(t - 1.0, 2.0);
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    private void highlightBedOrChest() {
        if (lookedAtState == null || lookedAtPos == null) return;
        Block block = lookedAtState.getBlock();

        if (!(block instanceof BedBlock) && !(block instanceof ChestBlock)) return;

        // Descomentar cuando BlockHighlightRenderer esté porteado
        /*
        boolean alreadyHighlighted = BlockHighlightRenderer.isHighlighted(lookedAtPos);
        if (alreadyHighlighted) {
            ModNetwork.CHANNEL.sendToServer(new CancelTaskPacket(lookedAtPos));
        } else {
            ModNetwork.CHANNEL.sendToServer(new TribeHighlightPacket(lookedAtPos, true));
        }
        */
    }

    private void toggleFollowMode() {
        tribeFollowEnabled = !tribeFollowEnabled;
        ModNetwork.CHANNEL.sendToServer(new SetTribeFollowModePacket(tribeFollowEnabled));
    }

    private void openModelCode() {
        // Descomentar cuando StaffItemRenderer esté porteado
        // StaffItemRenderer.cycleModel();
    }

    private void cutTreeOrFurniture() {
        if (lookedAtState == null || lookedAtPos == null) return;

        // Talar Árboles (1.20.1 usa BlockTags)
        if (lookedAtState.is(BlockTags.LOGS)) {
            // Descomentar lógica de highlight cuando esté lista
            ModNetwork.CHANNEL.sendToServer(new FallTreePacket(lookedAtPos));
            return;
        }

        // Empujar Muebles
        Object[] furnitureData = getFurnitureTarget();
        if (furnitureData != null) {
            BlockPos targetPos = (BlockPos) furnitureData[0];
            // Enviaríamos un FurniturePushPacket en el futuro
            ModNetwork.CHANNEL.sendToServer(new FallTreePacket(targetPos)); // Placeholder temporal
        }
    }

    @Nullable
    private Object[] getFurnitureTarget() {
        if (lookedAtState == null || lookedAtPos == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        // 🚨 1.20.1: Eliminado Material. Usamos Tags para ver si es madera, hojas o tierra/piedra
        boolean isSoft = lookedAtState.is(BlockTags.LOGS) ||
                lookedAtState.is(BlockTags.PLANKS) ||
                lookedAtState.is(BlockTags.LEAVES) ||
                lookedAtState.is(BlockTags.DIRT);

        if (!isSoft) return null;
        if (mc.player.blockPosition().getY() > lookedAtPos.getY()) return null;

        BlockPos cursor = lookedAtPos;
        Direction pushDir = lookedAtFace.getOpposite();

        // Buscar el bloque base
        while (mc.level.isEmptyBlock(cursor.below().relative(pushDir.getOpposite()))) {
            cursor = cursor.below();
        }

        if (lookedAtPos.getY() - cursor.getY() > 3) return null;

        return new Object[]{cursor, lookedAtFace};
    }
}