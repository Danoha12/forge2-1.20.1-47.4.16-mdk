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
                // 🚨 CONEXIÓN: Buscamos al bicho exacto en el mundo usando su UUID
                net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(msg.npcUuid);

                // Si encontramos al bicho y es un NPC de nuestro mod...
                if (entity instanceof com.trolmastercard.sexmod.entity.BaseNpcEntity) {

                    // ... ¡Le abrimos tu mochila!
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (containerId, playerInventory, p) -> {
                                return new com.trolmastercard.sexmod.world.inventory.NpcInventoryMenu(containerId, playerInventory);
                            },
                            net.minecraft.network.chat.Component.literal("Mochila del Súbdito")
                    ));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}