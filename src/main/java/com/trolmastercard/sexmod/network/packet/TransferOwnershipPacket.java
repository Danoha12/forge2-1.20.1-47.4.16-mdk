package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.TickableCallback; // Asumo que esto existe
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * TransferOwnershipPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Transfiere la propiedad de un grupo de NPCs, detiene sus IA de seguimiento
 * * y opcionalmente actualiza su punto de retorno.
 */
public class TransferOwnershipPacket {

    public final UUID npcUuid;
    public final UUID newOwnerUuid; // Puede ser nulo
    public final boolean setHome;
    public final boolean triggerCallback;

    // ── Constructores ────────────────────────────────────────────────────────

    public TransferOwnershipPacket(UUID npcUuid, UUID newOwnerUuid, boolean setHome, boolean triggerCallback) {
        this.npcUuid = npcUuid;
        this.newOwnerUuid = newOwnerUuid;
        this.setHome = setHome;
        this.triggerCallback = triggerCallback;
    }

    // ── Codec (Optimizado sin Strings) ───────────────────────────────────────

    public static void encode(TransferOwnershipPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUuid);
        buf.writeBoolean(msg.setHome);
        buf.writeBoolean(msg.triggerCallback);

        // Manejo elegante de UUIDs nulos sin usar Strings
        boolean hasNewOwner = msg.newOwnerUuid != null;
        buf.writeBoolean(hasNewOwner);
        if (hasNewOwner) {
            buf.writeUUID(msg.newOwnerUuid);
        }
    }

    public static TransferOwnershipPacket decode(FriendlyByteBuf buf) {
        UUID npcUuid = buf.readUUID();
        boolean setHome = buf.readBoolean();
        boolean triggerCallback = buf.readBoolean();

        UUID newOwnerUuid = buf.readBoolean() ? buf.readUUID() : null;

        return new TransferOwnershipPacket(npcUuid, newOwnerUuid, setHome, triggerCallback);
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(TransferOwnershipPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> applyToEntities(msg));

        ctx.setPacketHandled(true);
    }

    // ── Lógica de Aplicación ─────────────────────────────────────────────────

    private static void applyToEntities(TransferOwnershipPacket msg) {
        // Hacemos una copia segura para evitar ConcurrentModificationException
        List<BaseNpcEntity> targets = new ArrayList<>(BaseNpcEntity.getAllByMasterUUID(msg.npcUuid));

        for (BaseNpcEntity npc : targets) {
            if (npc.level().isClientSide()) continue;

            // Limpiamos las metas de la IA
            // (Asumiendo que followOwnerGoal y wanderGoal son públicos en tu BaseNpcEntity)
            if (npc.followOwnerGoal != null) npc.goalSelector.removeGoal(npc.followOwnerGoal);
            if (npc.wanderGoal != null) npc.goalSelector.removeGoal(npc.wanderGoal);

            // Detener navegación y físicas
            npc.getNavigation().stop();
            npc.setDeltaMovement(0, 0, 0);

            // Asignar nuevo dueño si se proporcionó uno
            if (msg.newOwnerUuid != null) {
                npc.setMasterUUID(msg.newOwnerUuid);
            }

            // 🚨 CORREGIDO: Fijar la casa en la posición actual de la entidad
            if (msg.setHome) {
                npc.setHomePos(npc.position());
            }

            // Refrescar el dueño (Mantuve esto asumiendo que tu setter dispara alguna sincronización de red)
            npc.setMasterUUID(npc.getMasterUUID());

            // Disparar callbacks si es necesario
            if (msg.triggerCallback && npc instanceof TickableCallback cb) {
                cb.onOwnerSet();
            }
        }
    }
}