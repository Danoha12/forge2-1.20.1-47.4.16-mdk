package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client - Server packet. Toggles the GalathEntity's "rape pounce" flag
 * on the NPC currently owned by the sending player.
 * Obfuscated name: g_
 */
public class GalathRapePouncePacket {

    private final boolean pounce;

    public GalathRapePouncePacket(boolean pounce) {
        this.pounce = pounce;
    }

    // -- Codec -----------------------------------------------------------------

    public static GalathRapePouncePacket decode(FriendlyByteBuf buf) {
        return new GalathRapePouncePacket(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.pounce);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid message @GalathRapePounce :(");
                return;
            }
            BaseNpcEntity npc = BaseNpcEntity.getByOwnerUUID(sender.getUUID());
            if (npc instanceof GalathEntity galath) {
                galath.setPounce(this.pounce);
            }
        });
        ctx.setPacketHandled(true);
    }
}
