package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.item.GalathCoinItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SpawnEnergyBallParticlesPacket — Portado a 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Invoca el efecto de partículas de energía en el cliente usando GalathCoinItem.
 */
public class SpawnEnergyBallParticlesPacket {

    private final UUID npcUUID;
    private final UUID targetUUID;

    public SpawnEnergyBallParticlesPacket(UUID npcUUID, UUID targetUUID) {
        this.npcUUID = npcUUID;
        this.targetUUID = targetUUID;
    }

    // ── Codec (Optimizado sin Strings) ───────────────────────────────────────

    public static void encode(SpawnEnergyBallParticlesPacket msg, FriendlyByteBuf buf) {
        // Usamos un boolean como "seguro" por si el UUID es nulo
        buf.writeBoolean(msg.npcUUID != null);
        if (msg.npcUUID != null) buf.writeUUID(msg.npcUUID);

        buf.writeBoolean(msg.targetUUID != null);
        if (msg.targetUUID != null) buf.writeUUID(msg.targetUUID);
    }

    public static SpawnEnergyBallParticlesPacket decode(FriendlyByteBuf buf) {
        UUID npc = buf.readBoolean() ? buf.readUUID() : null;
        UUID target = buf.readBoolean() ? buf.readUUID() : null;
        return new SpawnEnergyBallParticlesPacket(npc, target);
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(SpawnEnergyBallParticlesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Verificamos que realmente estemos en el cliente antes de encolar el trabajo
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> handleClient(msg));
        }
        ctx.setPacketHandled(true);
    }

    /**
     * Lógica aislada para el Cliente.
     * Evita que el Servidor Dedicado crashee al intentar cargar clases de Render/Partículas.
     */
    private static void handleClient(SpawnEnergyBallParticlesPacket msg) {
        if (msg.npcUUID == null) {
            System.out.println("[SexMod] Error: npcUUID vino nulo en el paquete de partículas.");
            return;
        }

        // Resolvemos el NPC en el mundo del cliente
        BaseNpcEntity npc = BaseNpcEntity.getByIdClient(msg.npcUUID);

        if (!(npc instanceof GalathEntity galath)) {
            System.out.println("[SexMod] Error: La entidad no existe o no es Galath.");
            return;
        }

        // Disparamos las partículas
        GalathCoinItem.spawnEnergyBallParticles(msg.targetUUID, galath);
    }
}