package com.trolmastercard.sexmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class ConsentScreen extends Screen {
    private final Screen parent;
    public static boolean hasConsented = false; // Esto debería guardarse en un config.json después

    public ConsentScreen(Screen parent) {
        super(Component.literal("ADVERTENCIA DE CONTENIDO"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 150;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Botón de Aceptar
        this.addRenderableWidget(Button.builder(Component.literal("Soy mayor de 18 y acepto"), (btn) -> {
            hasConsented = true;
            this.minecraft.setScreen(this.parent); // Volver al menú principal
        }).bounds(centerX - buttonWidth - 10, centerY + 40, buttonWidth, 20).build());

        // Botón de Salir
        this.addRenderableWidget(Button.builder(Component.literal("Salir del juego"), (btn) -> {
            this.minecraft.stop();
        }).bounds(centerX + 10, centerY + 40, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics); // Fondo oscuro

        // Dibujar el título en rojo
        guiGraphics.drawCenteredString(this.font, "§4§lADVERTENCIA: CONTENIDO ADULTO", this.width / 2, this.height / 2 - 50, 0xFFFFFF);

        // Dibujar el mensaje de advertencia
        guiGraphics.drawCenteredString(this.font, "Este mod contiene animaciones y contenido explícito.", this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Al continuar, confirmas que tienes la edad legal para ver este contenido.", this.width / 2, this.height / 2 - 5, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // No pueden saltarse la pantalla con ESC
    }
}