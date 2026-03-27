package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncInventoryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/**
 * NpcInventoryScreen - ported to 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class NpcInventoryScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    // =========================================================================
    //  Constants
    // =========================================================================

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    private static final int NPC_SLOT_COUNT = 7;

    // =========================================================================
    //  State
    // =========================================================================

    private final UUID npcUUID;
    private final KoboldEntity npc;
    private final UUID playerUUID;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public NpcInventoryScreen(AbstractContainerMenu menu,
                              Inventory playerInventory,
                              Component title,
                              KoboldEntity npc,
                              UUID npcUUID) {
        super(menu, playerInventory, title);
        this.npcUUID    = npcUUID;
        this.npc        = npc;
        this.playerUUID = playerInventory.player.getUUID();

        // Ajustamos el tamaño de la interfaz al de tu textura
        this.imageWidth = 176;
        this.imageHeight = 114;
    }

    // =========================================================================
    //  Render
    // =========================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // En 1.20.1, GuiGraphics se encarga del color y los shaders automáticamente
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Dibujamos el trozo de la textura desde u=80, v=142
        graphics.blit(TEXTURE, x, y, 80, 142, this.imageWidth, this.imageHeight);
    }

    // =========================================================================
    //  Close - sync inventory back to server
    // =========================================================================

    @Override
    public void onClose() {
        super.onClose();

        // Creamos un array combinado: jugador [0-35] + NPC [36-42]
        ItemStack[] snapshot = new ItemStack[43];

        // Inventario del jugador (36 slots)
        List<ItemStack> playerItems = this.minecraft.player.getInventory().items;
        for (int i = 0; i < 36; i++) {
            snapshot[i] = i < playerItems.size() ? playerItems.get(i).copy() : ItemStack.EMPTY;
        }

        // Inventario del NPC (7 slots, guardados en los índices 36-42)
        // Usamos 'this.menu' para acceder al menú que acabamos de cerrar
        for (int i = 0; i < NPC_SLOT_COUNT; i++) {
            snapshot[36 + i] = this.menu.getSlot(i).getItem().copy();
        }

        // Enviamos el paquete al servidor
        ModNetwork.CHANNEL.sendToServer(
                new SyncInventoryPacket(npc.getUUID(), playerUUID, snapshot)
        );
    }
}