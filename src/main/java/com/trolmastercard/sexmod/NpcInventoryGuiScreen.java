package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.UUID;

/**
 * NpcInventoryGuiScreen - ported from ek.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Client-side GUI for the NPC equipment inventory (slots: bow, sword, bra, lower, shoes).
 *
 * 1.12.2 - 1.20.1:
 *   - GuiContainer - AbstractContainerScreen
 *   - func_73863_a - render
 *   - func_146979_b - renderLabels
 *   - func_146976_a - renderBg
 *   - func_146281_b - onClose
 *   - d4/bx containers - NpcChestContainer/NpcEquipmentContainer
 *   - ge.b.sendToServer - ModNetwork.CHANNEL.sendToServer
 *   - b1 packet - NpcEquipmentSyncPacket
 */
@OnlyIn(Dist.CLIENT)
public class NpcInventoryGuiScreen extends AbstractContainerScreen<NpcEquipmentContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    final BaseNpcEntity npcEntity;
    final UUID          npcUUID;
    final UUID          playerUUID;
    final int           rows;

    public NpcInventoryGuiScreen(NpcEquipmentContainer container, Inventory playerInv,
                                  Component title, BaseNpcEntity npc, UUID npcUUID) {
        super(container, playerInv, title);
        this.npcEntity  = npc;
        this.npcUUID    = npcUUID;
        this.playerUUID = playerInv.player.getGameProfile().getId();
        this.rows       = npc.getInventory().getSlots() / 9;
        this.imageHeight = 114 + rows * 18;
    }

    @Override
    protected void renderBg(com.mojang.blaze3d.vertex.PoseStack ps, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;
        blit(ps, x, y, 0, 0, imageWidth, rows * 18 + 17);
        blit(ps, x, y + rows * 18 + 17, 0, 126, imageWidth, 96);
    }

    @Override
    protected void renderLabels(com.mojang.blaze3d.vertex.PoseStack ps, int mx, int my) {
        font.draw(ps, npcEntity.getName(), 8, 6, 0x404040);
        font.draw(ps, inventory.getDisplayName(), 8, imageHeight - 96 + 2, 0x404040);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack ps, int mx, int my, float pt) {
        renderBackground(ps);
        super.render(ps, mx, my, pt);
        renderTooltip(ps, mx, my);
    }

    /** On close: collect player inventory items and send equipment sync packet. */
    @Override
    public void onClose() {
        super.onClose();

        // Sync all NpcChestContainer entries matching this NPC
        for (NpcChestContainer chest : NpcChestContainer.openContainers) {
            if (!chest.getNpcUUID().equals(npcUUID)) continue;

            // Build snapshot of player inventory
            ItemStack[] snapshot = new ItemStack[63];
            var items = minecraft.player.getInventory().items;
            for (int i = 0; i < items.size() && i < snapshot.length; i++) {
                snapshot[i] = items.get(i).copy();
            }
            // Slot 36-62 are the NPC's 27 slots
            for (int i = 0; i < 27; i++) {
                var slot = chest.getSlot(i);
                if (slot != null) snapshot[i + 36] = slot.getItem().copy();
            }

            ModNetwork.CHANNEL.sendToServer(
                    new NpcEquipmentSyncPacket(npcUUID, playerUUID, snapshot));
        }
    }
}
