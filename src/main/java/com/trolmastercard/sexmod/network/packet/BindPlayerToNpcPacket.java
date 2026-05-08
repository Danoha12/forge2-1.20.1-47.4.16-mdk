package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * BindPlayerToNpcPacket — El "vinculador" de red.
 * Envía la orden del cliente al servidor para iniciar la interacción.
 */
public class BindPlayerToNpcPacket {
    private final UUID npcUUID;
    private final UUID playerUUID;

    public BindPlayerToNpcPacket(UUID npcUUID, UUID playerUUID) {
        this.npcUUID = npcUUID;
        this.playerUUID = playerUUID;
    }

    // Leer del buffer (Decodificador)
    public BindPlayerToNpcPacket(FriendlyByteBuf buf) {
        this.npcUUID = buf.readUUID();
        this.playerUUID = buf.readUUID();
    }

    // Escribir al buffer (Codificador)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.npcUUID);
        buf.writeUUID(this.playerUUID);
    }

    // Lógica del Servidor (Manejador)
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                Entity entity = level.getEntity(this.npcUUID);

                if (entity instanceof BaseNpcEntity npc) {
                    // Aquí llamamos a la lógica de inicio de escena
                    npc.setPartnerUUID(this.playerUUID);
                    // Puedes añadir aquí: npc.startSexScene(player);
                    // o lo que use tu BaseNpcEntity para arrancar.
                }
            }
        });
        return true;
    }
}