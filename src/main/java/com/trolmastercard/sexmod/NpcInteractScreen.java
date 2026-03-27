package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * NpcInteractScreen - ported from ea.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A radial overlay GUI that appears when the player interacts with an NPC.
 * Three zones exist along the horizontal / vertical mouse axes:
 *
 *   Left  (g  > 0) - throw-pearl / teleport-to-player action
 *   Right (e  > 0) - only visible for e3 (GoblinEntity): two additional choices
 *     Right-Upper (d > m) - GoblinEntity.c(playerUUID)  (sex from above)
 *     Right-Lower (d < m) - GoblinEntity.b(playerUUID)  (sex from below)
 *
 * The screen fades in with a back-ease function and does NOT pause the game.
 *
 * The screen opens when mapped key[0] ("Interact with your goblin") is pressed while
 * looking at the NPC. Pressing the same key while open closes the screen.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - GuiScreen - Screen
 *   - func_73863_a - render(PoseStack, mouseX, mouseY, partialTick)
 *   - func_146281_b - onClose()
 *   - func_146282_l / func_146283_a - keyPressed / keyReleased overrides
 *   - func_175174_a(x,y,u,v,w,h) - blit(ps, x, y, u, v, w, h)
 *   - GlStateManager.func_179094_E / func_179121_F - ps.pushPose / ps.popPose
 *   - GlStateManager.func_179152_a - ps.scale
 *   - GlStateManager.func_179109_b - ps.translate
 *   - OpenGlHelper.func_148821_a - RenderSystem.blendFuncSeparate
 *   - GL11.glEnable(3042) - RenderSystem.enableBlend()
 *   - GL11.glBlendFunc(770,771) - RenderSystem.defaultBlendFunc()
 *   - field_146297_k.func_193989_ak() - mc.getDeltaFrameTime()
 *   - field_146294_l / field_146295_m - width / height
 *   - Minecraft.func_71410_x().field_71439_g - Minecraft.getInstance().player
 *   - e3 - GoblinEntity; ei - PlayerKoboldEntity; fp - AnimState
 *   - ClientProxy.keyBindings[0].func_151463_i() - ClientProxy.keyBindings[0].getKey().getValue()
 *   - player.func_71053_j() - mc.player.closeContainer()
 *   - e3.b / e3.c - goblin.commitActionB / commitActionC (UUID variant)
 *   - em.b(fp.START_THROWING) - npc.setAnimState(AnimState.START_THROWING)
 *   - em.ae() - npc.getSexPartner()
 */
@OnlyIn(Dist.CLIENT)
public class NpcInteractScreen extends Screen {

    // -- Texture ----------------------------------------------------------------
    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/command.png");

    // -- State ------------------------------------------------------------------

    /** Fade-in (0-1). */
    float fadeIn = 0.0F;

    /** Hover accumulators. Range [0, 1]. */
    float left  = 0.0F;   // g: left zone (throw pearl)
    float right = 0.0F;   // e: right zone (only for GoblinEntity)
    float upper = 0.0F;   // d: upper-right sub-zone
    float lower = 0.0F;   // m: lower-right sub-zone

    /** The NPC this screen was opened for. */
    final BaseNpcEntity npc;
    /** Whether the NPC is a GoblinEntity (shows extra right-zone actions). */
    final boolean isGoblin;

    public NpcInteractScreen(BaseNpcEntity npc) {
        super(Component.empty());
        this.npc     = npc;
        this.isGoblin = (npc instanceof GoblinEntity);
    }

    // -- Screen contract --------------------------------------------------------

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        super.onClose();
        commitAction();
    }

    /** Close screen when the interact key is released. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == ClientProxy.keyBindings[0].getKey().getValue()) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // -- Render -----------------------------------------------------------------

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        super.render(ps, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();

        // Fade in
        fadeIn = Math.min(1.0F, fadeIn + mc.getDeltaFrameTime() / 5.0F);
        float ease  = (float) easeBackOut(fadeIn);
        float f2    = (1.0F - ease) * 100.0F;

        // Hover update
        float dt = mc.getDeltaFrameTime();
        left  = Mth.clamp(left  + (mouseX < width / 2  ?  1 : -1) * dt, 0, 1);
        right = Mth.clamp(right + (mouseX > width / 2  ?  1 : -1) * dt, 0, 1);
        upper = Mth.clamp(upper + (mouseY < height / 2 - 1 ?  1 : -1) * dt, 0, 1);
        lower = Mth.clamp(lower + (mouseY > height / 2 ?  1 : -1) * dt, 0, 1);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int cx = width / 2;
        int cy = height / 2;

        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.scale(ease, ease, ease);

        // Left action icon (pearl/teleport)
        ps.pushPose();
        ps.scale(1.0F + left * 0.5F, 1.0F + left * 0.5F, 1.0F);
        blit(ps, (int)(-62.0F + f2 - left * 15.0F), (int)(f2 - 32.0F), 0, 0, 64, 64);
        blit(ps, (int)(-62.0F + f2 - left * 15.0F), (int)(f2 - 32.0F), 64, 128, 64, 64);
        ps.popPose();

        if (isGoblin) {
            // Right actions (only for GoblinEntity)
            ps.pushPose();
            ps.scale(1.0F - right, 1.0F - right, 1.0F);
            blit(ps, (int)(-2.0F - f2 + right * 32.0F), (int)(-f2 - 32.0F), 0, 0, 64, 64);
            blit(ps, (int)(-2.0F - f2 + right * 32.0F), (int)(-f2 - 32.0F), 0, 128, 64, 64);
            ps.popPose();

            if (right > 0.0F) {
                // Upper-right sub-action
                ps.pushPose();
                float scaleU = -1.0F + right + 1.0F + upper * 0.5F;
                ps.scale(scaleU, scaleU, 1.0F);
                blit(ps, (int)(-2.0F - f2 + upper * 5.0F), (int)(-f2 - 64.0F - upper * 2.5F), 0, 0, 64, 64);
                blit(ps, (int)(-2.0F - f2 + upper * 5.0F), (int)(-f2 - 64.0F - upper * 2.5F), 128, 128, 64, 64);
                ps.popPose();

                // Lower-right sub-action
                ps.pushPose();
                float scaleL = -1.0F + right + 1.0F + lower * 0.5F;
                ps.scale(scaleL, scaleL, 1.0F);
                blit(ps, (int)(-2.0F - f2 + lower * 5.0F), (int)(-f2 + lower * 2.5F), 0, 0, 64, 64);
                blit(ps, (int)(-2.0F - f2 + lower * 5.0F), (int)(-f2 + lower * 2.5F), 192, 128, 64, 64);
                ps.popPose();
            }
        }

        ps.popPose();
        RenderSystem.disableBlend();
    }

    // -- Action commit ----------------------------------------------------------

    private void commitAction() {
        if (left == 0.0F && upper == 0.0F && lower == 0.0F && right == 0.0F) return;

        if (left > 0.0F) {
            // Throw pearl / teleport-to-player
            if (npc.getSexPartner() != null) return;
            npc.setAnimState(AnimState.START_THROWING);
            return;
        }

        if (!isGoblin) return;
        GoblinEntity goblin = (GoblinEntity) npc;
        java.util.UUID playerId = Minecraft.getInstance().player.getGameProfile().getId();

        if (upper > lower) {
            goblin.commitActionC(playerId);
        } else {
            goblin.commitActionB(playerId);
        }
    }

    // -- Back-ease easing -------------------------------------------------------
    private double easeBackOut(double t) {
        double s  = 1.70158D;
        double s2 = s + 1.0D;
        return 1.0D + s2 * Math.pow(t - 1.0D, 3.0D) + s * Math.pow(t - 1.0D, 2.0D);
    }
}
