package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.GalathOwnershipData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * OwnershipSyncPacket - informs the client whether the local player owns an NPC.
 * Ported from gf.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original: gf - OwnershipSyncPacket
 *   gf.b  - isOwner (payload boolean)
 *   v.f   - GalathOwnershipData.isLocalPlayerOwner (static field set on client)
 */
public class OwnershipSyncPacket {

    private final boolean isOwner;

    public OwnershipSyncPacket(boolean isOwner) {
        this.isOwner = isOwner;
    }

    // -- Codec -----------------------------------------------------------------

    public static OwnershipSyncPacket decode(FriendlyByteBuf buf) {
        return new OwnershipSyncPacket(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isOwner);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClient();
            } else {
                System.out.println("received an invalid message @OwnershipSync :(");
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        GalathOwnershipData.setLocalPlayerIsOwner(isOwner);
    }
}
