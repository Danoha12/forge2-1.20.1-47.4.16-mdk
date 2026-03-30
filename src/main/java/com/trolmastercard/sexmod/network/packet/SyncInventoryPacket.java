package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SyncInventoryPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Sincroniza el inventario completo tras cerrar la GUI.
 * * Soporta: Jugador (36 slots), Ropa NPC (7 slots) y Cofre Kobold (27 slots).
 */
public class SyncInventoryPacket {

    public static final int PLAYER_SLOTS = 36;
    private final UUID masterUUID;
    private final UUID playerUUID;
    private final List<ItemStack> stacks;

    public SyncInventoryPacket(UUID masterUUID, UUID playerUUID, List<ItemStack> stacks) {
        this.masterUUID = masterUUID;
        this.playerUUID = playerUUID;
        this.stacks = stacks;
    }

    /** Constructor alternativo para arrays (usado por la Screen). */
    public SyncInventoryPacket(UUID masterUUID, UUID playerUUID, ItemStack[] stacksArray) {
        this(masterUUID, playerUUID, List.of(stacksArray));
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(SyncInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.masterUUID);
        buf.writeUUID(msg.playerUUID);
        // writeCollection maneja el tamaño y los items automáticamente
        buf.writeCollection(msg.stacks, FriendlyByteBuf::writeItem);
    }

    public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
        return new SyncInventoryPacket(
                buf.readUUID(),
                buf.readUUID(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readItem)
        );
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(SyncInventoryPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (!ctx.getDirection().getReceptionSide().isServer()) return;

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.getUUID().equals(msg.playerUUID)) return;

            // 1. Sincronizar Inventario del Jugador (Slots 0-35)
            Inventory inv = player.getInventory();
            for (int i = 0; i < PLAYER_SLOTS && i < msg.stacks.size(); i++) {
                inv.setItem(i, msg.stacks.get(i).copy());
            }

            // 2. Buscar NPCs vinculados al MasterUUID
            List<BaseNpcEntity> targets = BaseNpcEntity.getAllWithMaster(msg.masterUUID);

            for (BaseNpcEntity npc : targets) {
                if (npc.level().isClientSide()) continue;

                // Caso A: Ropa y Equipamiento (NpcInventoryEntity)
                if (npc instanceof NpcInventoryEntity invNpc) {
                    var handler = invNpc.getInventory();
                    // Los slots de ropa empiezan en el índice 36 del paquete
                    for (int i = 0; i < 7 && (36 + i) < msg.stacks.size(); i++) {
                        handler.setStackInSlot(i, msg.stacks.get(36 + i).copy());
                    }
                }

                // Caso B: Cofre de Almacenamiento (KoboldEntity)
                if (npc instanceof KoboldEntity kobold) {
                    // Los Kobolds usan un inventario extendido (usualmente 27 slots)
                    var chest = kobold.getInventory();
                    // Si el paquete es grande, llenamos el cofre desde el índice 36
                    for (int i = 0; i < chest.getSlots() && (36 + i) < msg.stacks.size(); i++) {
                        chest.setStackInSlot(i, msg.stacks.get(36 + i).copy());
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}