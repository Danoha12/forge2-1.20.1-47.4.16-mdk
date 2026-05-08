package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * IsGirlPacket - syncs to the client whether the local player is currently
 * transformed into a girl NPC (kobold/allie/etc).
 */
public class IsGirlPacket {

    private final boolean isGirl;

    public IsGirlPacket(boolean isGirl) {
        this.isGirl = isGirl;
    }

    public IsGirlPacket(FriendlyByteBuf buf) {
        this.isGirl = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isGirl);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: update local player girl state
            com.trolmastercard.sexmod.client.handler.ClientStateManager.setAllieActive(isGirl);
        });
        ctx.get().setPacketHandled(true);
    }

    public boolean isGirl() { return isGirl; }
}