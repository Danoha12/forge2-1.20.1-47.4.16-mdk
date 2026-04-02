package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * NpcEquipmentSyncPacket — Envía el estado del inventario modificado al servidor.
 */
public class NpcEquipmentSyncPacket {

    private final UUID npcUuid;
    private final UUID playerUuid;
    private final ItemStack[] snapshot;

    public NpcEquipmentSyncPacket(UUID npcUuid, UUID playerUuid, ItemStack[] snapshot) {
        this.npcUuid = npcUuid;
        this.playerUuid = playerUuid;
        this.snapshot = snapshot;
    }

    public static void encode(NpcEquipmentSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUuid);
        buf.writeUUID(msg.playerUuid);

        // Escribimos cuántos ítems van en el paquete
        buf.writeVarInt(msg.snapshot.length);

        // Escribimos cada ítem individualmente (1.20.1 usa writeItem)
        for (ItemStack stack : msg.snapshot) {
            buf.writeItem(stack);
        }
    }

    public static NpcEquipmentSyncPacket decode(FriendlyByteBuf buf) {
        UUID npc = buf.readUUID();
        UUID player = buf.readUUID();

        int size = buf.readVarInt();
        ItemStack[] stacks = new ItemStack[size];

        // Leemos cada ítem (1.20.1 usa readItem)
        for (int i = 0; i < size; i++) {
            stacks[i] = buf.readItem();
        }

        return new NpcEquipmentSyncPacket(npc, player, stacks);
    }

    public static void handle(NpcEquipmentSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Aquí irá la lógica del servidor para guardar los ítems en el NPC real
        });
        ctx.get().setPacketHandled(true);
    }
}