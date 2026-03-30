package com.trolmastercard.sexmod.client.gui; // O el paquete donde guardes tus Screens

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ClaimTribePacket;
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
 * NameTribeScreen — Portado a 1.20.1.
 * * Interfaz gráfica sencilla para que el jugador introduzca el nombre de su tribu.
 * * Al presionar "set", envía un ClaimTribePacket al servidor.
 */
@OnlyIn(Dist.CLIENT)
public class NameTribeScreen extends Screen {

    private static final int NAME_WIDTH = 100;
    private static final int NAME_HEIGHT = 20;
    private static final int MAX_LENGTH = 15;

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
        super.init(); // Siempre es buena práctica llamar al super.init() en 1.20.1

        int cx = this.width / 2;
        int cy = this.height / 2;

        // 1. Inicializamos el campo de texto
        this.nameField = new EditBox(
                this.font,
                cx - NAME_WIDTH / 2, cy - NAME_HEIGHT / 2,
                NAME_WIDTH, NAME_HEIGHT,
                Component.empty()
        );

        // 2. Magia de 1.20.1: Configuramos el límite de caracteres de forma nativa
        this.nameField.setMaxLength(MAX_LENGTH);
        this.nameField.setFocused(true); // Cambiado de setFocus a setFocused

        // Al añadirlo como RenderableWidget, Minecraft maneja las teclas automáticamente
        this.addRenderableWidget(this.nameField);

        // 3. Añadimos el botón de confirmación
        this.addRenderableWidget(Button.builder(Component.literal("Set"), btn -> confirm())
                .bounds(cx - 25, cy + 20, 50, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // En 1.20.1, renderBackground pinta el oscurecimiento por defecto
        this.renderBackground(gfx);

        // Dibujamos el título encima del campo de texto
        gfx.drawString(this.font, "Name Tribe", this.width / 2 - 39, this.height / 2 - 25, 0xFFFFFF);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    // ¡Adiós a keyPressed, charTyped y tick! El EditBox moderno hace todo eso solo.

    private void confirm() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Enviamos el nombre al servidor
        ModNetwork.CHANNEL.sendToServer(
                new ClaimTribePacket(this.tribeUUID, mc.player.getUUID(), name)
        );

        // Para las Screens normales de cliente, onClose es suficiente.
        this.onClose();
    }
}