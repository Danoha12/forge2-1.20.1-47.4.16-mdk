package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.item.StaffItem;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockHighlightRenderer — Portado a 1.20.1 y optimizado.
 * Renderiza resaltados de colores sobre bloques marcados cuando se sostiene el Bastón.
 */
@OnlyIn(Dist.CLIENT)
public class BlockHighlightRenderer {

    private static final Vector3i COLOR_RED   = new Vector3i(255, 0, 0);
    private static final Vector3i COLOR_GREEN = new Vector3i(0, 255, 0);
    private static final Vector3i COLOR_BLUE  = new Vector3i(0, 0, 255);

    private static final ResourceLocation MARK_TEX = new ResourceLocation("sexmod", "textures/mark.png");

    // Set seguro para subprocesos para evitar crasheos por modificación concurrente
    private static final Set<BlockPos> markedPositions = ConcurrentHashMap.newKeySet();

    private static final Minecraft MC = Minecraft.getInstance();

    // ── API Pública ───────────────────────────────────────────────────────────

    public static void clearAll() { markedPositions.clear(); }
    public static boolean isMarked(BlockPos pos) { return markedPositions.contains(pos); }
    public static void addPositions(Set<BlockPos> positions) { markedPositions.addAll(positions); }
    public static void removePositions(Set<BlockPos> positions) { markedPositions.removeAll(positions); }

    // ── Renderizado ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        // En 1.20.1, este es el stage ideal para overlays del mundo
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (MC.player == null) return;

        // Verificar si el jugador sostiene el Bastón
        var mainStack = MC.player.getMainHandItem();
        var offStack  = MC.player.getOffhandItem();
        boolean hasStaff = mainStack.getItem() instanceof StaffItem || offStack.getItem() instanceof StaffItem;

        if (!hasStaff) return;

        renderHighlights(event);
    }

    private static void renderHighlights(RenderLevelStageEvent event) {
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack ps = event.getPoseStack();

        ps.pushPose();
        // Trasladar al espacio del mundo relativo a la cámara
        ps.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = ps.last().pose();

        // Configuración del sistema de renderizado moderno
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, MARK_TEX);
        // Transparencia al 50% para que sea visualmente agradable
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (BlockPos pos : markedPositions) {
            Vector3i col = getColor(pos);
            drawBlock(matrix, buf, pos, col.x, col.y, col.z);
        }

        tess.end();

        // Limpiar estado
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ps.popPose();
    }

    private static Vector3i getColor(BlockPos pos) {
        if (MC.level == null) return COLOR_RED;
        var block = MC.level.getBlockState(pos).getBlock();
        if (block instanceof BedBlock)   return COLOR_BLUE;
        if (block instanceof ChestBlock) return COLOR_GREEN;
        return COLOR_RED;
    }

    private static void drawBlock(Matrix4f matrix, BufferBuilder buf, BlockPos p, int r, int g, int b) {
        float x = p.getX(), y = p.getY(), z = p.getZ();
        float size = 1.01f; // Ligeramente más grande para evitar Z-fighting
        float o = -0.005f; // Offset de centrado

        // 6 Caras del bloque
        quad(matrix, buf, x+o, y+o, z+o, x+size, y+o, z+o, x+size, y+o, z+size, x+o, y+o, z+size, r, g, b); // Abajo
        quad(matrix, buf, x+o, y+size, z+o, x+size, y+size, z+o, x+size, y+size, z+size, x+o, y+size, z+size, r, g, b); // Arriba
        quad(matrix, buf, x+o, y+o, z+o, x+size, y+o, z+o, x+size, y+size, z+o, x+o, y+size, z+o, r, g, b); // Norte
        quad(matrix, buf, x+o, y+o, z+size, x+size, y+o, z+size, x+size, y+size, z+size, x+o, y+size, z+size, r, g, b); // Sur
        quad(matrix, buf, x+o, y+o, z+o, x+o, y+o, z+size, x+o, y+size, z+size, x+o, y+size, z+o, r, g, b); // Oeste
        quad(matrix, buf, x+size, y+o, z+o, x+size, y+o, z+size, x+size, y+size, z+size, x+size, y+size, z+o, r, g, b); // Este
    }

    private static void quad(Matrix4f matrix, BufferBuilder buf,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int b) {
        buf.vertex(matrix, x0, y0, z0).uv(0, 0).color(r, g, b, 255).endVertex();
        buf.vertex(matrix, x1, y1, z1).uv(1, 0).color(r, g, b, 255).endVertex();
        buf.vertex(matrix, x2, y2, z2).uv(1, 1).color(r, g, b, 255).endVertex();
        buf.vertex(matrix, x3, y3, z3).uv(0, 1).color(r, g, b, 255).endVertex();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START || MC.player == null) return;
        // Seguimiento de posición del jugador para otros sistemas
        ModConstants.PREV_PLAYER_POS = ModConstants.PLAYER_POS;
        ModConstants.PLAYER_POS      = MC.player.position();
    }
}