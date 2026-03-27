package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client - server to toggle the tribe follow mode.
 * Obfuscated name: fj
 */
public class SetTribeFollowModePacket {

    private final boolean followMode;

    public SetTribeFollowModePacket(boolean followMode) {
        this.followMode = followMode;
    }

    // -- Codec -----------------------------------------------------------------

    public static SetTribeFollowModePacket decode(FriendlyByteBuf buf) {
        boolean mode = buf.readBoolean();
        return new SetTribeFollowModePacket(mode);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.followMode);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid message @SetTribeFollowMode :(");
                return;
            }
            UUID tribeUUID = TribeManager.getTribeUUIDForPlayer(sender.getUUID());
            if (tribeUUID == null) return;
            TribeManager.setFollowMode(tribeUUID, this.followMode);
        });
        ctx.setPacketHandled(true);
    }
}
