package com.trolmastercard.sexmod.client.gui; // Ajusta al paquete correcto

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.inventory.NpcChestContainer;
import com.trolmastercard.sexmod.inventory.NpcEquipmentContainer;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.NpcEquipmentSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.UUID;

/**
 * NpcInventoryGuiScreen — Portado a 1.20.1.
 * * UI para el inventario extendido del NPC usando GuiGraphics.
 * * NOTA: Revisa si el genérico debería ser NpcChestContainer en lugar de NpcEquipmentContainer.
 */
@OnlyIn(Dist.CLIENT)
public class NpcInventoryGuiScreen extends AbstractContainerScreen<NpcEquipmentContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    private final BaseNpcEntity npcEntity;
    private final UUID npcUUID;
    private final UUID playerUUID;
    private final int rows;

    public NpcInventoryGuiScreen(NpcEquipmentContainer container, Inventory playerInv, Component title) {
        super(container, playerInv, title);

        // Extraemos todo del contenedor para mantener la compatibilidad con MenuScreens.register
        this.npcEntity = container.npc;
        this.npcUUID = container.npc.getUUID();
        this.playerUUID = playerInv.player.getUUID(); // getUUID() directo en 1.20.1

        // Usamos Capabilities para saber cuántos slots tiene (por defecto 27 = 3 filas)
        int totalSlots = this.npcEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(h -> h.getSlots())
                .orElse(27);
        this.rows = totalSlots / 9;
        this.imageHeight = 114 + this.rows * 18;
    }

    // ── Renderizado con GuiGraphics (1.20.1) ─────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx); // Oscurece el fondo
        super.render(gfx, mouseX, mouseY, partialTick); // Llama a renderBg y renderLabels
        this.renderTooltip(gfx, mouseX, mouseY); // Dibuja la info del ítem
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // GuiGraphics maneja el bindTexture y el RenderSystem por ti
        gfx.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.rows * 18 + 17);
        gfx.blit(TEXTURE, x, y + this.rows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // En 1.20.1 usamos drawString(fuente, texto, x, y, color, sombra)
        // False = sin sombra, como el texto de los cofres vanilla
        gfx.drawString(this.font, this.npcEntity.getName(), 8, 6, 0x404040, false);
        gfx.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    // ── Sincronización al Cerrar ─────────────────────────────────────────────

    @Override
    public void onClose() {
        syncToServer();
        super.onClose();
    }

    private void syncToServer() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        // 🚨 OJO: Iterar NpcChestContainer desde una screen de NpcEquipmentContainer es peligroso.
        // Lo mantengo como lo pediste, pero si tu mod tiene un NpcChestScreen, esto debería ir allá.
        for (NpcChestContainer chest : NpcChestContainer.openContainers) {
            if (!chest.getNpcUUID().equals(this.npcUUID)) continue;

            // Snapshot del inventario del jugador (36 slots) + 27 del NPC = 63 slots
            ItemStack[] snapshot = new ItemStack[63];
            var playerItems = this.minecraft.player.getInventory().items;

            for (int i = 0; i < playerItems.size() && i < 36; i++) {
                snapshot[i] = playerItems.get(i).copy();
            }

            // Los slots 36-62 son los 27 slots del NPC
            for (int i = 0; i < 27; i++) {
                var slot = chest.getSlot(i);
                if (slot != null && slot.hasItem()) {
                    snapshot[i + 36] = slot.getItem().copy();
                } else {
                    snapshot[i + 36] = ItemStack.EMPTY;
                }
            }

            ModNetwork.CHANNEL.sendToServer(
                    new NpcEquipmentSyncPacket(this.npcUUID, this.playerUUID, snapshot)
            );
            break; // Salimos del loop una vez que encontramos el cofre correcto
        }
    }
}