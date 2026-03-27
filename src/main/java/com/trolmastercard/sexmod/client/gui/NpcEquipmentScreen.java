package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.BaseNpcEntity;
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
 * GUI screen for the NPC's equipment / inventory container.
 * Ported from 1.12.2 to 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class NpcEquipmentScreen extends AbstractContainerScreen<NpcEquipmentContainer> {

    public static final ResourceLocation BACKGROUND =
            new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    private final UUID npcUUID;
    private final UUID playerUUID;
    private final BaseNpcEntity npc;

    public NpcEquipmentScreen(NpcEquipmentContainer container,
                              Inventory playerInventory,
                              Component title,
                              BaseNpcEntity npc) {
        super(container, playerInventory, title);
        this.npcUUID    = npc.getUUID();
        this.playerUUID = playerInventory.player.getUUID();
        this.npc        = npc;
    }

    // -- Render ---------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); // Dibuja el fondo oscuro semitransparente
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width  - this.imageWidth)  / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Dibuja el fondo principal del inventario
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Dibuja el panel superpuesto: src (176, 114) size 33x16
        // Argumentos de blit en 1.20.1: Texture, x, y, u, v, width, height
        graphics.blit(BACKGROUND,
                x + this.imageWidth / 2 - 88,
                y + this.imageHeight / 2 - 7 - 24,
                176, 114, 33, 16);
    }

    // -- Close / sync ---------------------------------------------------------

    @Override
    public void onClose() {
        super.onClose();
        syncToServer();
    }

    private void syncToServer() {
        // Build the combined array: 36 player slots + 6 NPC equipment slots = 42
        ItemStack[] combined = new ItemStack[42];

        var playerInv = this.minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            combined[i] = playerInv.items.get(i);
        }

        // NPC equipment slots (indices 36..41 correspond to container slots 0..5)
        for (int i = 0; i < 6; i++) {
            combined[36 + i] = this.menu.getSlot(i).getItem().copy();
        }

        ModNetwork.CHANNEL.sendToServer(
                new SyncInventoryPacket(npcUUID, playerUUID, combined)
        );
    }
}