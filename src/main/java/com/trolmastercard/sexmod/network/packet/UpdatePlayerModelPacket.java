package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Constructor;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * UpdatePlayerModelPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Maneja la transformación del jugador en uno de los tipos de NPC.
 * * Spawnea el "Avatar" 69 bloques arriba del jugador como área de staging.
 */
public class UpdatePlayerModelPacket {

    private final NpcType npcType;
    private final boolean valid;

    public UpdatePlayerModelPacket(NpcType npcType) {
        this.npcType = npcType;
        this.valid = true;
    }

    // ── Codec (Optimizado) ───────────────────────────────────────────────────

    public static void encode(UpdatePlayerModelPacket msg, FriendlyByteBuf buf) {
        // Usamos boolean + Enum para ahorrar bits en la red
        buf.writeBoolean(msg.npcType != null);
        if (msg.npcType != null) {
            buf.writeEnum(msg.npcType);
        }
    }

    public static UpdatePlayerModelPacket decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return new UpdatePlayerModelPacket(null); // Caso "player" (reset)
        }
        return new UpdatePlayerModelPacket(buf.readEnum(NpcType.class));
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(UpdatePlayerModelPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!msg.valid) return;

            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            ServerLevel level = sender.serverLevel();
            UUID playerUUID = sender.getUUID();

            // 1. Eliminar avatar previo si existe
            PlayerKoboldEntity existing = PlayerKoboldEntity.getByPlayerUUID(playerUUID);
            if (existing != null) {
                // Limpieza segura de overlays y entidades relacionadas
                BaseNpcEntity.getAllActive().removeIf(npc -> {
                    if (npc.getMasterUUID().equals(existing.getUUID())) {
                        npc.discard(); // Borrar del mundo
                        return true;
                    }
                    return false;
                });

                existing.dropAllContents(); // Soltar inventario si tiene
                existing.discard();
                PlayerKoboldEntity.removeFromRegistry(playerUUID);
            }

            // 2. Si el tipo es null, volvemos a ser humanos y terminamos
            if (msg.npcType == null) return;

            // 3. Crear el nuevo avatar
            try {
                // Buscamos el constructor: (Level, UUID del jugador)
                Constructor<? extends PlayerKoboldEntity> ctor = msg.npcType.playerClass.getConstructor(
                        net.minecraft.world.level.Level.class,
                        UUID.class
                );

                PlayerKoboldEntity newAvatar = ctor.newInstance(level, playerUUID);

                // Configuración de la entidad staging
                newAvatar.setPersistenceRequired();
                newAvatar.noPhysics = true;
                newAvatar.setDeltaMovement(0, 0, 0);

                // Posicionamiento 69 bloques arriba (clásico del mod)
                newAvatar.setPos(sender.getX(), sender.getY() + 69.0, sender.getZ());

                // Registrar y añadir al mundo
                level.addFreshEntity(newAvatar);
                PlayerKoboldEntity.register(playerUUID, newAvatar);

                // Inicializar pose por defecto para animaciones
                newAvatar.initDefaultState();

            } catch (Exception e) {
                System.err.println("[SexMod] Error al instanciar avatar para: " + msg.npcType.name());
                e.printStackTrace();
            }
        });
        ctx.setPacketHandled(true);
    }
}