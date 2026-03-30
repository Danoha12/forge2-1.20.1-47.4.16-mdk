package com.trolmastercard.sexmod.client.gui; // Ajusta al paquete correcto

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.inventory.NpcEquipmentContainer;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncInventoryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * NpcEquipmentScreen — Portado a 1.20.1.
 * * Pantalla del inventario y equipamiento del NPC.
 * * Al cerrarse, envía el inventario combinado al servidor para sincronizar.
 */
@OnlyIn(Dist.CLIENT)
public class NpcEquipmentScreen extends AbstractContainerScreen<NpcEquipmentContainer> {

    public static final ResourceLocation BACKGROUND =
            new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    private final UUID npcUUID;
    private final UUID playerUUID;
    private final BaseNpcEntity npc;

    public NpcEquipmentScreen(NpcEquipmentContainer container, Inventory playerInventory, Component title) {
        // En 1.20.1, el constructor registrado recibe estos 3 parámetros.
        // Extraemos las entidades directamente del container para mantener la firma limpia.
        super(container, playerInventory, title);
        this.npc = container.npc;
        this.npcUUID = container.npc.getUUID();
        this.playerUUID = playerInventory.player.getUUID();
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); // Oscurece el fondo
        super.render(graphics, mouseX, mouseY, partialTick); // Dibuja la UI y los slots
        this.renderTooltip(graphics, mouseX, mouseY); // Dibuja la información del ítem flotante
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. Dibuja el fondo principal. GuiGraphics gestiona el RenderSystem por ti.
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. Dibuja el panel superpuesto.
        // ¡Orden corregido! -> X, Y, U (176), V (114), Ancho (33), Alto (16)
        graphics.blit(BACKGROUND,
                x + this.imageWidth / 2 - 88,
                y + this.imageHeight / 2 - 7 - 24,
                176, 114, 33, 16);
    }

    // ── Sincronización al Cerrar ─────────────────────────────────────────────

    @Override
    public void onClose() {
        syncToServer(); // Sincronizamos antes de destruir la pantalla
        super.onClose();
    }

    private void syncToServer() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        // Combinamos: 36 slots del jugador + 6 slots del NPC = 42
        ItemStack[] combined = new ItemStack[42];
        var playerInv = this.minecraft.player.getInventory();

        for (int i = 0; i < 36; i++) {
            combined[i] = playerInv.items.get(i).copy(); // .copy() por seguridad
        }

        // Los slots del equipo del NPC corresponden a los índices 0 al 5 del contenedor
        for (int i = 0; i < 6; i++) {
            combined[36 + i] = this.menu.getSlot(i).getItem().copy();
        }

        ModNetwork.CHANNEL.sendToServer(
                new SyncInventoryPacket(this.npcUUID, this.playerUUID, combined)
        );
    }
}