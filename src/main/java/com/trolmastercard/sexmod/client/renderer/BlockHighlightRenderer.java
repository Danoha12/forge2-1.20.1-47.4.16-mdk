package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.ModConstants;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3i;

import java.util.ConcurrentModificationException;
import java.util.HashSet;

import static com.mojang.blaze3d.platform.GlStateManager.*;

/**
 * BlockHighlightRenderer - renders coloured highlight quads over marked blocks.
 * Ported from gm.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Migration notes:
 *   RenderWorldLastEvent - RenderLevelStageEvent.Stage.AFTER_PARTICLES
 *   GlStateManager.func_* - com.mojang.blaze3d.platform.GlStateManager._*
 *   BufferBuilder.func_181662_b/func_187315_a/func_181669_b - .vertex/.uv/.color
 *   Tessellator draw/startDrawingQuads - drawWith VertexFormat.Mode.QUADS
 *   Vec3d player position - Camera.getPosition()
 *   ItemFishingRod/hy.b - StaffItem.INSTANCE (staff triggers highlight rendering)
 *   TickEvent.ClientTickEvent used to update prev camera pos
 */
@OnlyIn(Dist.CLIENT)
public class BlockHighlightRenderer {

    private static final Vector3i COLOR_RED   = new Vector3i(255, 0,   0);
    private static final Vector3i COLOR_GREEN = new Vector3i(0,   255, 0);
    private static final Vector3i COLOR_BLUE  = new Vector3i(0,   0,   255);

    private static final ResourceLocation MARK_TEX =
            new ResourceLocation("sexmod", "textures/mark.png");

    private static final HashSet<BlockPos> markedPositions = new HashSet<>();

    private static final Minecraft MC = Minecraft.getInstance();

    // -- Public API ------------------------------------------------------------

    public static void clearAll() { markedPositions.clear(); }

    public static boolean isMarked(BlockPos pos) { return markedPositions.contains(pos); }

    public static void addPositions(HashSet<BlockPos> positions) {
        markedPositions.addAll(positions);
    }

    public static void removePositions(HashSet<BlockPos> positions) {
        markedPositions.removeAll(positions);
    }

    // -- Rendering -------------------------------------------------------------

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        // Only render when staff is in either hand
        var mainStack = MC.player.getMainHandItem();
        var offStack  = MC.player.getOffhandItem();
        boolean hasStaff = (!mainStack.isEmpty() && mainStack.getItem() instanceof StaffItem)
                || (!offStack.isEmpty()  && offStack.getItem()  instanceof StaffItem);
        if (!hasStaff) return;

        try {
            renderHighlights(event);
        } catch (ConcurrentModificationException ignored) {}
    }

    private static void renderHighlights(RenderLevelStageEvent event) {
        Camera camera = MC.gameRenderer.getMainCamera();

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-camera.getPosition().x,
                     -camera.getPosition().y,
                     -camera.getPosition().z);

        _disableDepthTest();
        _enableBlend();
        _defaultBlendFunc();
        _disableTexture();

        MC.getTextureManager().bindForSetup(MARK_TEX);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        try {
            for (BlockPos pos : markedPositions) {
                Vector3i col = getColor(pos);
                drawBlock(buf, pos, col.x, col.y, col.z);
            }
        } catch (ConcurrentModificationException ignored) {}

        tess.end();

        _enableDepthTest();
        _disableBlend();
        _enableTexture();

        ps.popPose();
    }

    private static Vector3i getColor(BlockPos pos) {
        var block = MC.level.getBlockState(pos).getBlock();
        if (block instanceof BedBlock)   return COLOR_BLUE;
        if (block instanceof ChestBlock) return COLOR_GREEN;
        return COLOR_RED;
    }

    private static void drawBlock(BufferBuilder buf, BlockPos p,
                                  int r, int g, int b) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        // 6 faces, each as 4 vertices (QUADS)
        // Bottom face
        quad(buf, x,   y,   z,   x+1, y,   z,   x+1, y,   z+1, x,   y,   z+1, r,g,b);
        // Top face
        quad(buf, x,   y+1, z,   x+1, y+1, z,   x+1, y+1, z+1, x,   y+1, z+1, r,g,b);
        // North face (z-)
        quad(buf, x,   y,   z,   x+1, y,   z,   x+1, y+1, z,   x,   y+1, z,   r,g,b);
        // South face (z+)
        quad(buf, x,   y,   z+1, x+1, y,   z+1, x+1, y+1, z+1, x,   y+1, z+1, r,g,b);
        // West face (x-)
        quad(buf, x,   y,   z,   x,   y,   z+1, x,   y+1, z+1, x,   y+1, z,   r,g,b);
        // East face (x+)
        quad(buf, x+1, y,   z,   x+1, y,   z+1, x+1, y+1, z+1, x+1, y+1, z,   r,g,b);
    }

    private static void quad(BufferBuilder buf,
                              int x0, int y0, int z0,
                              int x1, int y1, int z1,
                              int x2, int y2, int z2,
                              int x3, int y3, int z3,
                              int r, int g, int b) {
        buf.vertex(x0, y0, z0).uv(0, 0).color(r, g, b, 255).endVertex();
        buf.vertex(x1, y1, z1).uv(1, 0).color(r, g, b, 255).endVertex();
        buf.vertex(x2, y2, z2).uv(1, 1).color(r, g, b, 255).endVertex();
        buf.vertex(x3, y3, z3).uv(0, 1).color(r, g, b, 255).endVertex();
    }

    // -- Tick ------------------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        // player position tracking handled by ModConstants.prevPlayerPos / playerPos
        if (MC.player == null) return;
        ModConstants.PREV_PLAYER_POS = ModConstants.PLAYER_POS;
        ModConstants.PLAYER_POS      = MC.player.position();
    }
}
