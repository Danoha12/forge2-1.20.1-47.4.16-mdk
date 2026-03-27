package com.trolmastercard.sexmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Simple GUI screen that lets a player enter a name for their tribe.
 * Pressing "set" sends a {@link ClaimTribePacket} to the server.
 *
 * Obfuscated name: g7
 */
@OnlyIn(Dist.CLIENT)
public class NameTribeScreen extends Screen {

    private static final int NAME_WIDTH  = 100;
    private static final int NAME_HEIGHT = 20;
    private static final int MAX_LENGTH  = 15;

    private final UUID tribeUUID;
    private EditBox nameField;

    public NameTribeScreen(UUID tribeUUID) {
        super(Component.literal("Name Tribe"));
        this.tribeUUID = tribeUUID;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        this.nameField = new EditBox(
                this.font,
                cx - NAME_WIDTH / 2, cy - NAME_HEIGHT / 2,
                NAME_WIDTH, NAME_HEIGHT,
                Component.empty());
        this.nameField.setFocus(true);
        this.addRenderableWidget(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("set"), btn -> confirm())
                .bounds(cx - 25, cy + 20, 50, 20)
                .build());
    }

    @Override
    public void tick() {
        this.nameField.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        gfx.drawString(this.font, "Name Tribe",
                this.width / 2 - 39, this.height / 2 - 10, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
            String text = this.nameField.getValue();
            if (text.length() > MAX_LENGTH) {
                this.nameField.setValue(text.substring(0, MAX_LENGTH));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        boolean consumed = this.nameField.charTyped(ch, modifiers);
        String text = this.nameField.getValue();
        if (text.length() > MAX_LENGTH) {
            this.nameField.setValue(text.substring(0, MAX_LENGTH));
        }
        return consumed;
    }

    private void confirm() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ModNetwork.CHANNEL.sendToServer(
                new ClaimTribePacket(this.tribeUUID, mc.player.getUUID(), name));
        mc.player.closeContainer();
        this.onClose();
    }
}
