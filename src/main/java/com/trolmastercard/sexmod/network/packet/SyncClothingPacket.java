package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SyncClothingPacket — Envía la ropa seleccionada desde la GUI al Servidor.
 */
public class SyncClothingPacket {
    private final UUID npcUuid;
    private final List<String> clothingList;

    public SyncClothingPacket(UUID npcUuid, List<String> clothingList) {
        this.npcUuid = npcUuid;
        this.clothingList = clothingList;
    }

    public static void encode(SyncClothingPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUuid);
        buf.writeVarInt(msg.clothingList.size());
        for (String clothing : msg.clothingList) {
            buf.writeUtf(clothing); // Escribimos cada prenda
        }
    }

    public static SyncClothingPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf()); // Leemos cada prenda
        }
        return new SyncClothingPacket(id, list);
    }

    public static void handle(SyncClothingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Aquí el servidor buscará al NPC y le aplicará el msg.clothingList
        });
        ctx.get().setPacketHandled(true);
    }
}