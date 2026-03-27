package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * GuiHandler - ported from et.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * In 1.12.2 this implemented IGuiHandler to map GUI IDs to server and client
 * container/screen objects. In 1.20.1 the IGuiHandler system is gone; containers
 * are opened via NetworkHooks.openScreen() with a MenuProvider. This class is
 * retained as a static factory that creates the appropriate AbstractContainerMenu
 * for each registered GUI ID, and also provides the static helper that was used
 * to trigger container creation from the test code in the original.
 *
 * GUI IDs:
 *   0 - NpcChestContainer (cat NPC inventory) or NpcEquipmentContainer (generic NPC)
 *   1 - NpcEquipmentContainer (generic NPC equipment slots)
 *
 * 1.12.2 - 1.20.1:
 *   - IGuiHandler - AbstractContainerMenu factory + NetworkHooks.openScreen()
 *   - GuiContainer - AbstractContainerScreen (handled by ClientSetup)
 *   - EntityPlayer - Player
 *   - World - Level
 *   - em.ad() - BaseNpcEntity.getAllNpcs()
 *   - em.func_180425_c() - npc.blockPosition()
 *   - new ca(eb, inv, uuid) - new NpcChestContainer(luna, inv, uuid)
 *   - new d4(em, inv, uuid) - new NpcEquipmentContainer(npc, inv, uuid)
 *   - new bx(inv, iinv, player, uuid) - new NpcEquipmentContainer(npc, inv, uuid)
 *   - new az / fb / ek - client-only screen classes (handled by ClientSetup)
 *   - ConcurrentModificationException catch - iterate over a copy
 */
public class GuiHandler {

    /** GUI ID constants matching the original int params. */
    public static final int GUI_NPC_CHEST      = 0;
    public static final int GUI_NPC_EQUIPMENT  = 1;

    /**
     * Returns the server-side AbstractContainerMenu for the given GUI ID and block position.
     * Called from packets that need to open a container on the server.
     */
    public static AbstractContainerMenu getServerContainer(int guiId, Player player,
                                                            Level level, int bx, int by, int bz) {
        for (BaseNpcEntity npc : java.util.List.copyOf(BaseNpcEntity.getAllNpcs())) {
            if (npc.level.isClientSide()) continue;
            var pos = npc.blockPosition();
            if (pos.getX() != bx || pos.getY() != by || pos.getZ() != bz) continue;

            if (guiId == GUI_NPC_CHEST) {
                if (npc instanceof LunaEntity luna)
                    return new NpcChestContainer(luna, player.getInventory(), UUID.randomUUID());
                return new NpcEquipmentContainer(npc, player.getInventory(), UUID.randomUUID());
            }
            if (guiId == GUI_NPC_EQUIPMENT) {
                if (npc instanceof NpcInventoryEntity inv)
                    return new NpcEquipmentContainer(npc, player.getInventory(), UUID.randomUUID());
            }
        }
        return null;
    }

    /**
     * Opens the appropriate screen for a given NPC on the client.
     * Called by the networking system after the server has approved the open.
     *
     * @param guiId  GUI_NPC_CHEST or GUI_NPC_EQUIPMENT
     * @param npc    The NPC whose inventory to show
     * @param player The local player
     */
    public static void openClientGui(int guiId, BaseNpcEntity npc, Player player) {
        if (guiId == GUI_NPC_CHEST) {
            UUID uuid = UUID.randomUUID();
            if (npc instanceof LunaEntity luna)
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new NpcChestScreen(new NpcChestContainer(luna, player.getInventory(), uuid),
                                       player.getInventory(),
                                       net.minecraft.network.chat.Component.literal(npc.getName().getString()),
                                       luna, uuid));
            else
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new NpcEquipmentScreen(new NpcEquipmentContainer(npc, player.getInventory(), uuid),
                                           player.getInventory(),
                                           net.minecraft.network.chat.Component.literal(npc.getName().getString()),
                                           npc, uuid));
        } else if (guiId == GUI_NPC_EQUIPMENT) {
            UUID uuid = UUID.randomUUID();
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new NpcInventoryGuiScreen(new NpcEquipmentContainer(npc, player.getInventory(), uuid),
                                          player.getInventory(),
                                          net.minecraft.network.chat.Component.literal(npc.getName().getString()),
                                          npc, uuid));
        }
    }
}
