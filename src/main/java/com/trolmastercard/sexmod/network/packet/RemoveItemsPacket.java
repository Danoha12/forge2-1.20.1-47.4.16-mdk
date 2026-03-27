package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RemoveItemsPacket - ported from t.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER to remove a specific count of a specific item type
 * from the target player's inventory.
 *
 * The server searches for the first slot that contains an item matching
 * {@code itemStack}'s type and shrinks it by the stack's count.
 */
public class RemoveItemsPacket {

    private final UUID      playerUUID;
    private final ItemStack itemStack;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public RemoveItemsPacket(UUID playerUUID, ItemStack itemStack) {
        this.playerUUID = playerUUID;
        this.itemStack  = itemStack;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static RemoveItemsPacket decode(FriendlyByteBuf buf) {
        UUID      uuid  = UUID.fromString(buf.readUtf());
        ItemStack stack = buf.readItem();
        return new RemoveItemsPacket(uuid, stack);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerUUID.toString());
        buf.writeItem(itemStack);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(playerUUID);
            if (player == null) {
                System.out.println("received an invalid message @RemoveItems :(");
                return;
            }

            // Find first slot with a matching item type and shrink it
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot.is(itemStack.getItem())) {
                    slot.shrink(itemStack.getCount());
                    break;
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
