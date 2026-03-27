package com.trolmastercard.sexmod.client.screen;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BeeEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ChangeDataParameterPacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

/**
 * BeeQuickAccessScreen - ported from ch.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A small animated pop-up screen (triggered by right-clicking a Bee NPC) with three buttons:
 *   0 = Follow/Stop-following toggle
 *   1 = Go home
 *   2 = Set home (icon button)
 *
 * Also renders the Bee NPC's portrait icon depending on whether she is pregnant.
 *
 * Field mapping:
 *   c = beeEntity (fo - BeeEntity)
 *   a = player    (EntityPlayer - Player)
 *   e = isFollowing (boolean - derived from master DataParameter)
 *   b = GUI texture ResourceLocation
 *   d = animProgress (double, slides from 0 - 1 over ~5 ticks)
 *
 * In 1.12.2:
 *   GuiScreen / GuiButton   - Screen / Button
 *   ScaledResolution.func_78326_a() - width (from Screen)
 *   func_73863_a             - render(GuiGraphics, int, int, float)
 *   func_73864_a             - mouseClicked(double, double, int)
 *   func_146284_a(GuiButton) - onPress(Button)
 *   field_146292_n.add       - addRenderableWidget
 *   field_146292_n.clear     - removed on each render call (rebuilt)
 *   func_73729_b(x,y, u,v, w,h) - guiGraphics.blit(...)
 *   I18n.func_135052_a      - Component.translatable(key)
 *   ge.b.sendToServer        - ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), ...)
 *   em.v                     - BaseNpcEntity.MASTER_UUID
 *   fo.K                     - BeeEntity.IS_PREGNANT  (boolean DataParameter)
 *   new f3(npcUUID, playerUUID) - SetPlayerForNpcPacket(...)
 *   new n(npcUUID, "master", "") - ChangeDataParameterPacket(uuid, "master", "")
 *   new gg(npcUUID)          - SetNpcHomePacket(uuid)
 *   new a6(npcUUID, Vec3d)   - SetNpcHomePacket(uuid, pos)
 *   func_71053_j()           - minecraft.setScreen(null)
 *   player.func_145747_a     - player.displayClientMessage(...)
 *   foK                      - BeeEntity.IS_PREGNANT DataParameter key
 */
@OnlyIn(Dist.CLIENT)
public class BeeQuickAccessScreen extends Screen {

    private final BeeEntity beeEntity;
    private final Player player;
    private boolean isFollowing;
    private double animProgress = 0.0D;

    private static final ResourceLocation GUI_TEXTURE =
        new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    public BeeQuickAccessScreen(BeeEntity bee, Player player) {
        super(Component.empty());
        this.beeEntity   = bee;
        this.player      = player;
        // Derive isFollowing from MASTER_UUID DataParameter
        String masterUUID = bee.getEntityData().get(
            com.trolmastercard.sexmod.entity.BaseNpcEntity.MASTER_UUID);
        this.isFollowing = !"".equals(masterUUID);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // =========================================================================
    //  render  (original: ch.func_73863_a)
    // =========================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Rebuild buttons each frame so their widths animate with animProgress
        clearWidgets();

        // Advance animation
        animProgress = Math.min(1.0D, animProgress + (partialTick / 5.0F));

        int centerX = width / 2;
        int animWidth = (int)(animProgress * 100.0D);
        int animOffsetX = (int)(100.0D - 100.0D * animProgress);

        // Button 0: Follow / Stop following
        addRenderableWidget(Button.builder(
            Component.translatable(isFollowing
                ? "action.names.stopfollowme"
                : "action.names.followme"),
            btn -> onFollowButton())
            .pos(centerX - 119 + animOffsetX, 30)
            .size(animWidth, 20)
            .build());

        // Button 1: Go home
        addRenderableWidget(Button.builder(
            Component.translatable("action.names.gohome"),
            btn -> onGoHomeButton())
            .pos(centerX + 19, 30)
            .size(animWidth, 20)
            .build());

        // Draw GUI texture
        minecraft.getTextureManager().bindForSetup(GUI_TEXTURE);
        int iconY = 61 - (int)(15.0D - animProgress * 15.0D);

        // Home icon (small button icon)
        graphics.blit(GUI_TEXTURE, centerX - 7, iconY, 32, 0, 15, 15);

        // Button 2: Set home (invisible area over icon)
        addRenderableWidget(Button.builder(Component.empty(),
            btn -> onSetHomeButton())
            .pos(centerX - 10, iconY - 2)
            .size(20, 20)
            .build());

        // Portrait: pregnant vs normal
        boolean isPregnant = beeEntity.getEntityData().get(BeeEntity.IS_PREGNANT);
        graphics.blit(GUI_TEXTURE,
            centerX - 20, 20,
            isPregnant ? 0 : 40, 130,
            40, 40);
    }

    // =========================================================================
    //  Mouse click (portrait click to initiate player-npc binding)
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        boolean isPregnant = beeEntity.getEntityData().get(BeeEntity.IS_PREGNANT);
        if (isPregnant) {
            if (mouseX >= centerX - 20 && mouseX <= centerX + 20
                && mouseY >= 20 && mouseY <= 60) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                    new com.trolmastercard.sexmod.network.packet.SetPlayerForNpcPacket(
                        beeEntity.getUUID(), player.getUUID()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // =========================================================================
    //  Button handlers
    // =========================================================================

    private void onFollowButton() {
        if (isFollowing) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new ChangeDataParameterPacket(beeEntity.getUUID(), "master", ""));
            player.displayClientMessage(
                Component.translatable("bee.dialogue.sad"), true);
        } else {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new ChangeDataParameterPacket(beeEntity.getUUID(), "master",
                    player.getStringUUID()));
            player.displayClientMessage(
                Component.translatable("bee.dialogue.exited"), true);
        }
        isFollowing = !isFollowing;
        onClose();
    }

    private void onGoHomeButton() {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
            new SetNpcHomePacket(beeEntity.getUUID(), null));
        onClose();
    }

    private void onSetHomeButton() {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
            new SetNpcHomePacket(beeEntity.getUUID(),
                new Vec3(beeEntity.getX(), beeEntity.getY(), beeEntity.getZ())));
        onClose();
        player.displayClientMessage(
            Component.translatable("bee.dialogue.home"), true);
    }
}
