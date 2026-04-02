package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenInteractionInventoryPacket {
    private final UUID npcUuid;

    public OpenInteractionInventoryPacket(UUID npcUuid, UUID playerUuid) {
        this.npcUuid = npcUuid;
    }

    public static void encode(OpenInteractionInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUuid);
    }

    public static OpenInteractionInventoryPacket decode(FriendlyByteBuf buf) {
        return new OpenInteractionInventoryPacket(buf.readUUID(), null);
    }

    public static void handle(OpenInteractionInventoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Aquí se abriría el Container (Menú de Inventario)
                // player.openMenu(...);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}