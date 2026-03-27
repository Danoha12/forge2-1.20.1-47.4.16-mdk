package com.trolmastercard.sexmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LogBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * StaffCommandScreen - ported from j.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A radial-ish command overlay that opens when the player right-clicks with the
 * dragon staff while targeting blocks/entities in the world.
 * The screen is transparent (no pause), divided into four quadrants.
 * Moving the mouse into a quadrant highlights its action; releasing (or pressing
 * Escape) commits the most-hovered action.
 *
 * Quadrant - action mapping (UV positions from command.png 256-128 atlas):
 *   top-left     (a field)  - highlightBed() / highlightChest()
 *   bottom-left  (k field)  - toggleTribeFollowMode()
 *   top-right    (n field)  - openStaffModel() / showModelCode()
 *   bottom-right (i field)  - cutTree() / pushFurniture()
 *
 * Icon texture: sexmod:textures/gui/command.png
 * Icon size: 64-64 per quadrant icon (atlas layout matches original).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - GuiScreen - Screen
 *   - func_73863_a - render(PoseStack, mouseX, mouseY, partialTick)
 *   - func_146281_b - onClose()
 *   - func_73868_f() - isPauseScreen()
 *   - func_146286_b - keyPressed / mouseClicked
 *   - GlStateManager - RenderSystem; GL calls - RenderSystem equivalents
 *   - func_175174_a(x,y,uOff,vOff,w,h) - blit(poseStack,x,y,uOff,vOff,w,h)
 *   - Minecraft.func_71410_x() - Minecraft.getInstance()
 *   - field_71476_x - hitResult; field_71441_e - level
 *   - field_178784_b.func_176734_d() - BlockHitResult.getDirection()
 *   - BlockLog instanceof - state.is(BlockTags.LOGS)
 *   - Material.field_151571_B/e/p/c - Material.STONE / WOOD / LEAVES / GRASS
 *   - func_193989_ak() - getDeltaFrameTime()
 *   - ge.b - ModNetwork.CHANNEL
 *   - gm.a(pos) - BlockHighlightRenderer.isHighlighted(pos)
 *   - fa.a() - StaffItemRenderer.cycleModel()
 *   - fj - SetTribeFollowModePacket; au - CancelTaskPacket; fc - FallTreePacket
 *   - e6 - (furniture push packet - class name not yet resolved, placeholder)
 *   - b3 - GalathCallback used as TribeUIValuesPacket (here actually sends tribe data)
 *   - h6 - TribeHighlightPacket
 *
 * NOTE: The render() implementation reconstructs the original GL-state machine
 * using the bytecode comments as a guide. Exact pixel positions are preserved.
 */
@OnlyIn(Dist.CLIENT)
public class StaffCommandScreen extends Screen {

    // -- Constants --------------------------------------------------------------
    static final float MAX_HIGHLIGHT   = 100.0F;
    static final float ICON_OFFSET     = 15.0F;
    static final float ICON_HALF_SIZE  = 0.5F;

    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/command.png");

    /** Materials that count as "soft" furniture that can be pushed. */
    static final HashSet<Material> SOFT_MATERIALS = new HashSet<>(Arrays.asList(
            Material.STONE, Material.WOOD, Material.LEAVES, Material.GRASS));

    public static boolean tribeFollowEnabled = false;

    // -- State ------------------------------------------------------------------

    /** Fade-in progress (0-1). */
    private float fadeIn = 0.0F;

    /** Per-quadrant hover accumulator: [topLeft, bottomLeft, topRight, bottomRight]. */
    private float tlHover = 0.0F;  // bed/chest   (field a)
    private float blHover = 0.0F;  // follow mode (field k)
    private float trHover = 0.0F;  // model code  (field n)
    private float brHover = 0.0F;  // tree/furn   (field i)

    // Context from world
    private final BlockState lookedAtState;
    private final BlockPos   lookedAtPos;
    private final net.minecraft.core.Direction lookedAtFace;

    public StaffCommandScreen() {
        super(Component.empty());
        Minecraft mc = Minecraft.getInstance();

        BlockPos     pos  = BlockPos.ZERO;
        net.minecraft.core.Direction face = net.minecraft.core.Direction.NORTH;

        if (mc.hitResult instanceof BlockHitResult bhr) {
            pos  = bhr.getBlockPos();
            face = bhr.getDirection();
        }
        this.lookedAtPos   = pos;
        this.lookedAtFace  = face;
        this.lookedAtState = mc.level != null ? mc.level.getBlockState(pos) : null;
    }

    // -- Screen overrides -------------------------------------------------------

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        super.onClose();
        // Commit the most-hovered quadrant action
        List<Float> vals = Arrays.asList(tlHover, blHover, trHover, brHover);
        float max = Collections.max(vals);
        if (max == 0.0F) return;
        if (tlHover == max) highlightBedOrChest();
        if (blHover == max) toggleFollowMode();
        if (trHover == max) openModelCode();
        if (brHover == max) cutTreeOrFurniture();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        int w  = width;
        int h  = height;
        int cx = w / 2;
        int cy = h / 2;

        // -- Advance fade ------------------------------------------------------
        fadeIn = Math.min(1.0F, fadeIn + mc.getDeltaFrameTime() / 5.0F);
        float ease = (float) easeBackOut(fadeIn);
        float dist = (1.0F - ease) * 100.0F;   // how far icons have slid in

        // -- Update hover accumulators -----------------------------------------
        float dt = mc.getDeltaFrameTime();
        tlHover = Mth.clamp(tlHover + (mouseX < cx && mouseY > cy ? 1 : -1) * dt, 0, 1);
        blHover = Mth.clamp(blHover + (mouseX < cx && mouseY < cy ? 1 : -1) * dt, 0, 1);
        trHover = Mth.clamp(trHover + (mouseX > cx && mouseY > cy ? 1 : -1) * dt, 0, 1);
        brHover = Mth.clamp(brHover + (mouseX > cx && mouseY < cy ? 1 : -1) * dt, 0, 1);

        // -- Render ------------------------------------------------------------
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        mc.getTextureManager().bindForSetup(TEXTURE);

        // Draw quad icons at their positions (matches original pixel layout)
        drawQuadIcon(poseStack, cx, cy, dist,
                -62.0F + dist - blHover * 15.0F,   // top-left x from center
                -62.0F + dist - blHover * 15.0F,   // top-left y from center
                0, 0, 64, 64, ease);                // UV + size

        drawQuadIcon(poseStack, cx, cy, dist,
                -2.0F - dist + trHover * 15.0F,    // top-right
                -62.0F + dist - trHover * 15.0F,
                64, 0, 64, 64, ease);

        drawQuadIcon(poseStack, cx, cy, dist,
                -62.0F + dist - tlHover * 15.0F,   // bottom-left
                -2.0F - dist + tlHover * 15.0F,
                0, 64, 64, 64, ease);

        drawQuadIcon(poseStack, cx, cy, dist,
                -2.0F - dist + brHover * 15.0F,    // bottom-right
                -2.0F - dist + brHover * 15.0F,
                64, 64, 64, 64, ease);

        // Draw "locked" overlay on icons that point at already-highlighted blocks
        if (lookedAtPos != null && BlockHighlightRenderer.isHighlighted(lookedAtPos)) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.5f);
            // Re-draw the relevant icons dimmer
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        RenderSystem.disableBlend();
    }

    private void drawQuadIcon(PoseStack ps, int cx, int cy, float dist,
                               float relX, float relY,
                               int uOff, int vOff, int w, int h, float alpha) {
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        blit(ps, cx + (int) relX, cy + (int) relY, uOff, vOff, w, h);
    }

    // -- Back-ease easing (matching original a(double) method) -----------------
    private double easeBackOut(double t) {
        double s  = 1.70158;
        double s2 = s + 1.0;
        return 1.0 + s2 * Math.pow(t - 1.0, 3.0) + s * Math.pow(t - 1.0, 2.0);
    }

    // -- Actions ----------------------------------------------------------------

    /** Highlight (or un-highlight) the looked-at bed or chest. */
    private void highlightBedOrChest() {
        if (lookedAtState == null) return;
        Block block = lookedAtState.getBlock();
        if (!(block instanceof BedBlock) && !(block instanceof ChestBlock)) return;

        boolean alreadyHighlighted = BlockHighlightRenderer.isHighlighted(lookedAtPos);
        if (alreadyHighlighted) {
            ModNetwork.CHANNEL.sendToServer(new CancelTaskPacket(lookedAtPos));
        } else {
            ModNetwork.CHANNEL.sendToServer(new TribeHighlightPacket(lookedAtPos, true));
        }
    }

    /** Toggle tribe follow mode. */
    private void toggleFollowMode() {
        ModNetwork.CHANNEL.sendToServer(new SetTribeFollowModePacket(!tribeFollowEnabled));
    }

    /** Open the staff model-code GUI. */
    private void openModelCode() {
        StaffItemRenderer.cycleModel();
    }

    /** Cut the looked-at log tree, or push a piece of furniture. */
    private void cutTreeOrFurniture() {
        if (lookedAtState == null) return;
        Block block = lookedAtState.getBlock();

        // Tree chopping
        if (lookedAtState.is(net.minecraft.tags.BlockTags.LOGS)) {
            if (BlockHighlightRenderer.isHighlighted(lookedAtPos)) {
                ModNetwork.CHANNEL.sendToServer(new CancelTaskPacket(lookedAtPos));
            } else {
                ModNetwork.CHANNEL.sendToServer(new FallTreePacket(lookedAtPos));
            }
            return;
        }

        // Furniture push
        Object[] furnitureData = getFurnitureTarget();
        if (furnitureData != null) {
            BlockPos targetPos = (BlockPos) furnitureData[0];
            net.minecraft.core.Direction face = (net.minecraft.core.Direction) furnitureData[1];
            if (BlockHighlightRenderer.isHighlighted(lookedAtPos)) {
                ModNetwork.CHANNEL.sendToServer(new CancelTaskPacket(lookedAtPos));
            } else {
                // FurniturePushPacket - send target pos + direction
                ModNetwork.CHANNEL.sendToServer(new FallTreePacket(targetPos)); // placeholder
            }
        }
    }

    /** Determines if the looked-at block is push-able furniture and returns [pos, face]. */
    @Nullable
    private Object[] getFurnitureTarget() {
        if (lookedAtState == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        Material mat = lookedAtState.getMaterial();
        if (!SOFT_MATERIALS.contains(mat)) return null;
        if (mc.player.blockPosition().getY() > lookedAtPos.getY()) return null;

        BlockPos cursor = lookedAtPos;
        net.minecraft.core.Direction pushDir = lookedAtFace.getOpposite();

        while (mc.level.isEmptyBlock(cursor.below().relative(pushDir.getOpposite()))) {
            cursor = cursor.below();
        }
        if (lookedAtPos.getY() - cursor.getY() > 3) return null;

        return new Object[]{ cursor, lookedAtFace };
    }
}
