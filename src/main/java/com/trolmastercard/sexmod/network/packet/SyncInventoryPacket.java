package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SyncInventoryPacket - ported from b1.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER. Uploads the full combined inventory state to the server:
 *  - 36 player inventory slots (hotbar + main)
 *  - Up to 7 NPC clothing slots (from {@code e2.Q} / {@code eb.Q})
 *  - Up to 27 kobold-entity chest slots (from {@code fo.L})
 *
 * Field mapping:
 *   a = masterUUID  (UUID of the NPC group)
 *   c = playerUUID  (UUID of the player whose inventory to sync)
 *   d = stacks      (ItemStack array, total size: 36 + up to 43 NPC slots)
 *
 * Slot layout (indices):
 *   [0..35]   - player inventory (36 slots: hotbar 0-8, main 9-35)
 *   [36..42]  - NPC clothing slots (7 for eb-type, 6 for e2-type)
 *   [36..62]  - KoboldEntity chest (27 for fo-type; overlaps with clothing indices)
 *
 * In 1.12.2:
 *   - {@code ByteBufUtils.readItemStack/writeItemStack} - {@code buf.readItem()/writeItem()}
 *   - {@code ByteBufUtils.readUTF8String/writeUTF8String} - {@code buf.readUtf()/writeUtf()}
 *   - {@code entityPlayer.field_71071_by} - {@code player.getInventory()}
 *   - {@code inventoryPlayer.func_70299_a(slot, stack)} - {@code inv.setItem(slot, stack)}
 *   - {@code IMessage/IMessageHandler} - FriendlyByteBuf + handle()
 */
public class SyncInventoryPacket {

    public static final int PLAYER_SLOTS   = 36;
    public static final int CLOTHING_SLOTS = 7;

    private final UUID        masterUUID;
    private final UUID        playerUUID;
    private final ItemStack[] stacks;
    private final boolean     valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public SyncInventoryPacket(UUID masterUUID, UUID playerUUID, ItemStack[] stacks) {
        this.masterUUID = masterUUID;
        this.playerUUID = playerUUID;
        this.stacks     = stacks;
        this.valid      = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
        UUID master = UUID.fromString(buf.readUtf());
        UUID player = UUID.fromString(buf.readUtf());
        int count   = buf.readInt();
        ItemStack[] items = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            items[i] = buf.readItem();
        }
        var pkt = new SyncInventoryPacket(master, player, items);
        return pkt;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
        buf.writeUtf(playerUUID.toString());
        buf.writeInt(stacks.length);
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @UploadInventoryToServer :(");
                return;
            }

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                ArrayList<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(masterUUID);

                for (BaseNpcEntity npc : npcs) {
                    if (npc.level().isClientSide()) continue;

                    Player player = npc.level().getPlayerByUUID(playerUUID);
                    if (player == null) return;

                    // Sync player inventory (slots 0-35)
                    Inventory inv = player.getInventory();
                    for (int i = 0; i < PLAYER_SLOTS; i++) {
                        inv.setItem(i, stacks[i]);
                    }

                    // Sync NPC clothing slots (slots 36-42)
                    if (npc instanceof com.trolmastercard.sexmod.entity.NpcInventoryEntity invNpc) {
                        var handler = invNpc.getClothingHandler();
                        int slotCount = handler.getSlots();
                        for (int i = 0; i < slotCount && (36 + i) < stacks.length; i++) {
                            handler.setStackInSlot(i, stacks[36 + i]);
                        }
                    }

                    // Sync KoboldEntity chest (slots 36-62)
                    if (npc instanceof com.trolmastercard.sexmod.entity.KoboldEntity kob) {
                        var chest = kob.getChestHandler();
                        for (int i = 0; i < 27 && (36 + i) < stacks.length; i++) {
                            chest.setStackInSlot(i, stacks[36 + i]);
                        }
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
