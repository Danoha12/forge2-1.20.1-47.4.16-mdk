package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * GalathBackOffPacket - ported from cd.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER.
 *
 * Sent when the player wants the Galath to "back off" (stop the current assault /
 * approach sequence).  On the server side, looks up the GalathEntity that has the
 * sender as its owner and calls {@code galath.w()} (the "back off" method).
 *
 * Comment in original source: {@code @GalathBackOffRape}.
 *
 * Field mapping:
 *   a = valid flag (fromBytes guard)
 *
 * In 1.12.2:
 *   IMessage/IMessageHandler          - FriendlyByteBuf encode/decode + handle(Supplier)
 *   param1MessageContext.side.SERVER  - ctx.getDirection().getReceptionSide() == SERVER
 *   FMLCommonHandler.getMinecraftServerInstance().func_152344_a(runnable)
 *     - ctx.enqueueWork(runnable)
 *   em.a(UUID, Boolean.TRUE)          - BaseNpcEntity.getNpcByOwner(playerUUID)
 *   (f_)em                            - (GalathEntity)em
 *   galath.w()                        - galath.backOff()
 */
public class GalathBackOffPacket {

    private final boolean valid;

    public GalathBackOffPacket() {
        this.valid = true;
    }

    // =========================================================================
    //  Encode / Decode
    // =========================================================================

    public static void encode(GalathBackOffPacket msg, FriendlyByteBuf buf) {
        // No payload - presence of packet is sufficient
    }

    public static GalathBackOffPacket decode(FriendlyByteBuf buf) {
        return new GalathBackOffPacket();
    }

    // =========================================================================
    //  Handle  (SERVER side)
    // =========================================================================

    public static void handle(GalathBackOffPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) {
                System.out.println("received an invalid Message @GalathBackOffRape :(");
                return;
            }
            BaseNpcEntity npc = BaseNpcEntity.getNpcByOwner(
                ctx.getSender().getUUID(), true);
            if (npc instanceof GalathEntity galath) {
                galath.backOff();
            }
        });
        ctx.setPacketHandled(true);
    }
}
