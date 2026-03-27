package com.trolmastercard.sexmod;
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

/**
 * GoblinContextMenuScreen - ported from ea.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A radial quick-command overlay shown when the player right-clicks a goblin or
 * NPC. The screen shows up to four quadrant icons:
 *   top-left   (g field)  - START_THROWING (if no sex partner)
 *   top-right  (e field)  - [unused/blank] (right side)
 *   bottom-left (d field) - GoblinEntity.c(playerUUID) - follow
 *   bottom-right (m field)- GoblinEntity.b(playerUUID) - stop follow
 *
 * For GoblinEntity targets ({@code h = true}) two extra buttons appear.
 *
 * The screen does not pause the game.
 *
 * Animation: a single fade-in value drives both scale and icon offset.
 * Icons slide in from off-screen over ~5 frames, eased with a back-overshoot curve.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - GuiScreen - Screen
 *   - func_73863_a(int,int,float) - render(PoseStack,mouseX,mouseY,partialTick)
 *   - func_73868_f() - isPauseScreen()
 *   - func_146281_b() - onClose()
 *   - func_146282_l() - keyPressed(int,int,int)
 *   - GlStateManager - RenderSystem / GL11 - RenderSystem
 *   - OpenGlHelper.func_148821_a(770,771,1,0) - RenderSystem.blendFuncSeparate
 *   - func_193989_ak() - getDeltaFrameTime()
 *   - func_175174_a(x,y,u,v,w,h) - blit(ps,x,y,u,v,w,h)
 *   - field_146294_l/m - width/height
 *   - ClientProxy.keyBindings[0].func_151463_i() - KeyMapping.getKey().getValue()
 *   - Keyboard.getEventKey/getEventKeyState - forwarded through keyPressed
 *   - be.b(val,min,max) - Mth.clamp
 *   - em.ae() - npc.getSexPartner()
 *   - em - BaseNpcEntity; e3 - GoblinEntity
 *   - SENTINEL value 1.2345679F passed to func_73868_f to detect close
 *   - a(double) back-easing - same formula as in StaffCommandScreen
 */
@OnlyIn(Dist.CLIENT)
public class GoblinContextMenuScreen extends Screen {

    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/command.png");

    /** The NPC this menu was opened for. */
    final BaseNpcEntity npc;

    /** True if the NPC is a GoblinEntity (extra buttons enabled). */
    final boolean isGoblin;

    // -- Animation --------------------------------------------------------------
    float fadeIn = 0.0F;

    /** Per-quadrant hover accumulator [0]=left, [1]=right, [2]=top, [3]=bottom. */
    float leftHover   = 0.0F;
    float rightHover  = 0.0F;
    float topHover    = 0.0F;
    float bottomHover = 0.0F;

    public GoblinContextMenuScreen(BaseNpcEntity npc) {
        super(Component.empty());
        this.npc      = npc;
        this.isGoblin = (npc instanceof GoblinEntity);
    }

    // -- Screen overrides -------------------------------------------------------

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        super.onClose();
        commitAction();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on the interact-with-goblin keybinding
        if (ClientProxy.keyBindings != null && ClientProxy.keyBindings.length > 0) {
            int bindKey = ClientProxy.keyBindings[0].getKey().getValue();
            if (keyCode == bindKey) {
                Minecraft.getInstance().player.closeContainer();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // -- Render -----------------------------------------------------------------

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        super.render(ps, mouseX, mouseY, partialTick);

        Minecraft mc  = Minecraft.getInstance();
        int cx = width / 2;
        int cy = height / 2;

        // Advance fade
        fadeIn = Math.min(1.0F, fadeIn + mc.getDeltaFrameTime() / 5.0F);
        float scale = (float) easeBackOut(fadeIn);
        float dist  = (1.0F - scale) * 100.0F;

        // Update hover
        float dt = mc.getDeltaFrameTime();
        leftHover   = Mth.clamp(leftHover   + (mouseX < cx ? 1 : -1) * dt, 0, 1);
        rightHover  = Mth.clamp(rightHover  + (mouseX > cx ? 1 : -1) * dt, 0, 1);
        topHover    = Mth.clamp(topHover    + (mouseY < cy ? 1 : -1) * dt, 0, 1);
        bottomHover = Mth.clamp(bottomHover + (mouseY > cy ? 1 : -1) * dt, 0, 1);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                RenderSystem.SourceFactor.SRC_ALPHA,
                RenderSystem.DestFactor.ONE_MINUS_SRC_ALPHA,
                RenderSystem.SourceFactor.ONE,
                RenderSystem.DestFactor.ZERO);
        mc.getTextureManager().bindForSetup(TEXTURE);

        // -- Push outer scale --------------------------------------------------
        ps.pushPose();
        ps.translate(cx, cy, 0.0F);
        ps.scale(scale, scale, scale);

        // Left icon (scale grows when hovered)
        ps.pushPose();
        ps.scale(1.0F + leftHover * 0.5F, 1.0F + leftHover * 0.5F, 1.0F);
        blit(ps, (int)(-62.0F + dist - leftHover * 15.0F), (int)(dist - 32.0F), 0, 0, 64, 64);
        blit(ps, (int)(-62.0F + dist - leftHover * 15.0F), (int)(dist - 32.0F), 64, 128, 64, 64);
        ps.popPose();

        if (isGoblin) {
            // Right icon
            ps.pushPose();
            ps.scale(1.0F - rightHover, 1.0F - rightHover, 1.0F);
            blit(ps, (int)(-2.0F - dist + rightHover * 32.0F), (int)(-dist - 32.0F), 0, 0, 64, 64);
            blit(ps, (int)(-2.0F - dist + rightHover * 32.0F), (int)(-dist - 32.0F), 0, 128, 64, 64);
            ps.popPose();

            if (rightHover > 0.0F) {
                // Top-right icon
                ps.pushPose();
                ps.scale(-1.0F + rightHover + 1.0F + topHover * 0.5F,
                         -1.0F + rightHover + 1.0F + topHover * 0.5F, 1.0F);
                blit(ps, (int)(-2.0F - dist + topHover * 5.0F),
                         (int)(-dist - 64.0F - topHover * 5.0F / 2.0F), 0, 0, 64, 64);
                blit(ps, (int)(-2.0F - dist + topHover * 5.0F),
                         (int)(-dist - 64.0F - topHover * 5.0F / 2.0F), 128, 128, 64, 64);
                ps.popPose();

                // Bottom-right icon
                ps.pushPose();
                ps.scale(-1.0F + rightHover + 1.0F + bottomHover * 0.5F,
                         -1.0F + rightHover + 1.0F + bottomHover * 0.5F, 1.0F);
                blit(ps, (int)(-2.0F - dist + bottomHover * 5.0F),
                         (int)(-dist + bottomHover * 5.0F / 2.0F), 0, 0, 64, 64);
                blit(ps, (int)(-2.0F - dist + bottomHover * 5.0F),
                         (int)(-dist + bottomHover * 5.0F / 2.0F), 192, 128, 64, 64);
                ps.popPose();
            }
        }

        ps.popPose();
        RenderSystem.disableBlend();
    }

    // -- Commit on close --------------------------------------------------------

    private void commitAction() {
        if (leftHover == 0.0F && rightHover == 0.0F &&
            topHover  == 0.0F && bottomHover == 0.0F) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        float maxH = Math.max(Math.max(leftHover, rightHover),
                              Math.max(topHover, bottomHover));

        if (leftHover == maxH) {
            // Throw / start action
            if (npc.getSexPartner() == null) {
                npc.setAnimState(AnimState.START_THROWING);
            }
            return;
        }

        if (!isGoblin) return;

        if (rightHover == maxH && topHover > bottomHover) {
            // Follow
            ((GoblinEntity) npc).startFollowing(player.getUUID());
        } else if (rightHover == maxH && bottomHover >= topHover) {
            // Stop follow
            ((GoblinEntity) npc).stopFollowing(player.getUUID());
        }
    }

    // -- Back-ease --------------------------------------------------------------

    private double easeBackOut(double t) {
        double s  = 1.70158D;
        double s2 = s + 1.0D;
        return 1.0D + s2 * Math.pow(t - 1.0D, 3.0D) + s * Math.pow(t - 1.0D, 2.0D);
    }
}
