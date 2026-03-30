package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.world.inventory.NpcInventoryMenu;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncNpcInventoryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * NpcInventoryScreen — Portado a 1.20.1.
 * * Interfaz visual para el inventario de los NPCs.
 * * Maneja el renderizado de la textura 'girlinventory.png' y la sincronización al cerrar.
 */
public class NpcInventoryScreen extends AbstractContainerScreen<NpcInventoryMenu> {

  private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

  private final UUID npcUUID;
  private final UUID playerUUID;

  public NpcInventoryScreen(NpcInventoryMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);

    // Dimensiones de la textura (80, 142, 176, 114 en el atlas)
    this.imageWidth = 176;
    this.imageHeight = 166; // Altura estándar para acomodar inventario de jugador + slots NPC

    this.npcUUID = menu.getNpcUUID();
    this.playerUUID = playerInventory.player.getUUID();
  }

  @Override
  protected void init() {
    super.init();
    // Centrar el título de la GUI
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
  }

  // ── Renderizado ──────────────────────────────────────────────────────────

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTick);
    this.renderTooltip(graphics, mouseX, mouseY);
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    // Dibujamos el fondo desde la región (80, 142) de la textura original
    // El tamaño de la región es 176x114
    int x = this.leftPos;
    int y = this.topPos;

    graphics.blit(TEXTURE, x, y, 80, 142, 176, 114);
  }

  // ── Sincronización al Cerrar ──────────────────────────────────────────────

  @Override
  public void onClose() {
    // En 1.20.1 no necesitamos buscar en listas estáticas,
    // 'this.menu' es la instancia actual vinculada a esta pantalla.

    ItemStack[] snapshot = new ItemStack[43];

    // 1. Snapshot del inventario del jugador (36 slots)
    for (int i = 0; i < 36; i++) {
      snapshot[i] = this.menu.getSlot(i + 7).getItem().copy();
    }

    // 2. Snapshot del inventario del NPC (7 slots: 0-6)
    for (int i = 0; i < 7; i++) {
      snapshot[36 + i] = this.menu.getSlot(i).getItem().copy();
    }

    // Enviar el paquete de sincronización definitivo al servidor
    ModNetwork.CHANNEL.sendToServer(new SyncNpcInventoryPacket(npcUUID, playerUUID, snapshot));

    super.onClose();
  }
}