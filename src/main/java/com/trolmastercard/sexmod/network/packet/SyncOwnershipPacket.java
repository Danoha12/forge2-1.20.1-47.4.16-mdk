package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SyncOwnershipPacket - syncs Galath ownership data from server to client.
 */
public class SyncOwnershipPacket {

    private final Map<UUID, UUID> ownershipMap; // galathUUID -> ownerUUID
    private final boolean reset;

    /** Send a full ownership snapshot. */
    public SyncOwnershipPacket(Map<UUID, UUID> ownershipMap) {
        this.ownershipMap = ownershipMap != null ? ownershipMap : new HashMap<>();
        this.reset = false;
    }

    /** Send a reset packet (clears client-side ownership). */
    public SyncOwnershipPacket(boolean reset) {
        this.ownershipMap = new HashMap<>();
        this.reset = reset;
    }

    public SyncOwnershipPacket(FriendlyByteBuf buf) {
        this.reset = buf.readBoolean();
        int size = buf.readInt();
        this.ownershipMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            UUID galath = buf.readUUID();
            UUID owner  = buf.readUUID();
            ownershipMap.put(galath, owner);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(reset);
        buf.writeInt(ownershipMap.size());
        ownershipMap.forEach((g, o) -> { buf.writeUUID(g); buf.writeUUID(o); });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: apply ownership data
        });
        ctx.get().setPacketHandled(true);
    }

    public Map<UUID, UUID> getOwnershipMap() { return ownershipMap; }
    public boolean isReset() { return reset; }
}