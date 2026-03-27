package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.item.ModItems;
import com.trolmastercard.sexmod.tribe.EyeAndKoboldColor;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendEggPacket - ported from z.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER when the player should receive a tribe egg item.
 *
 * The server:
 *  1. Resolves the tribe UUID for the sending player
 *  2. Looks up the tribe's {@link EyeAndKoboldColor} to choose the wool meta
 *  3. Creates a {@link ModItems#TRIBE_EGG} ItemStack with the tribe UUID
 *     stored as an NBT string under the key {@code "tribeID"}
 *  4. Adds it to the player's inventory
 */
public class SendEggPacket {

    private final boolean valid;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public SendEggPacket() {
        this.valid = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static SendEggPacket decode(FriendlyByteBuf buf) {
        buf.readBoolean(); // consumed  packet is valid if it arrived at all
        return new SendEggPacket();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(valid);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                System.out.println("received an invalid Message @SendEgg :(");
                return;
            }

            UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
            if (tribeId == null) return;

            EyeAndKoboldColor color = TribeManager.getTribeColor(tribeId);

            // Build the egg ItemStack - wool color determined by tribe color
            ItemStack egg = new ItemStack(ModItems.TRIBE_EGG.get(), 1);
            net.minecraft.nbt.CompoundTag tag = egg.getOrCreateTag();
            tag.putString("tribeID", tribeId.toString());
            // Store wool color index so the item renderer can tint it
            tag.putInt("woolMeta", color.getWoolMeta());
            egg.setTag(tag);

            player.getInventory().add(egg);
        });
        ctx.setPacketHandled(true);
    }
}
