package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SyncNpcInventoryPacket — Envía los 43 slots (Jugador + NPC) al servidor al cerrar la GUI.
 */
public class SyncNpcInventoryPacket {

    private final UUID npcUuid;
    private final UUID playerUuid;
    private final ItemStack[] snapshot;

    public SyncNpcInventoryPacket(UUID npcUuid, UUID playerUuid, ItemStack[] snapshot) {
        this.npcUuid = npcUuid;
        this.playerUuid = playerUuid;
        this.snapshot = snapshot;
    }

    public static void encode(SyncNpcInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUuid);
        buf.writeUUID(msg.playerUuid);

        buf.writeVarInt(msg.snapshot.length);
        for (ItemStack stack : msg.snapshot) {
            buf.writeItem(stack);
        }
    }

    public static SyncNpcInventoryPacket decode(FriendlyByteBuf buf) {
        UUID npc = buf.readUUID();
        UUID player = buf.readUUID();

        int size = buf.readVarInt();
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = buf.readItem();
        }

        return new SyncNpcInventoryPacket(npc, player, stacks);
    }

    public static void handle(SyncNpcInventoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Lógica del servidor: Buscar al NPC por UUID y aplicarle los ítems de msg.snapshot
        });
        ctx.get().setPacketHandled(true);
    }
}