package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SpawnParticlePacket — Portado a 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Ordena al cliente generar N cantidad de partículas en un NPC específico.
 */
public class SpawnParticlePacket {

    public final UUID npcUUID;
    public final ResourceLocation particleId;
    public final int count;

    // ── Constructores ────────────────────────────────────────────────────────

    public SpawnParticlePacket(UUID npcUUID, String particleId) {
        this(npcUUID, new ResourceLocation(particleId), 1);
    }

    public SpawnParticlePacket(UUID npcUUID, ResourceLocation particleId, int count) {
        this.npcUUID = npcUUID;
        this.particleId = particleId;
        this.count = count;
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(SpawnParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUUID); // 128 bits crudos, ¡no más Strings!
        buf.writeResourceLocation(msg.particleId); // Nativo de Forge/Minecraft
        buf.writeInt(msg.count);
    }

    public static SpawnParticlePacket decode(FriendlyByteBuf buf) {
        return new SpawnParticlePacket(
                buf.readUUID(),
                buf.readResourceLocation(),
                buf.readInt()
        );
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(SpawnParticlePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                // 🛡️ Aislamiento total para que el servidor dedicado jamás lea esto por error
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            });
        } else {
            System.out.println("[SexMod] Error: El servidor recibió un @SpawnParticle :(");
        }

        ctx.setPacketHandled(true);
    }

    // ── Lógica del Cliente ───────────────────────────────────────────────────

    private static void handleClient(SpawnParticlePacket msg) {
        // Obtenemos el tipo de partícula de los registros de Forge
        var particleType = ForgeRegistries.PARTICLE_TYPES.getValue(msg.particleId);
        if (particleType == null) return;

        // Copiamos la lista para evitar ConcurrentModificationException
        List<BaseNpcEntity> allNpcs = new ArrayList<>(BaseNpcEntity.getAllNpcs());

        for (BaseNpcEntity npc : allNpcs) {
            // 🚨 1.20.1: level() es un método ahora
            if (!npc.level().isClientSide()) continue;

            if (!npc.getNpcUUID().equals(msg.npcUUID)) continue;

            for (int i = 0; i < msg.count; i++) {
                npc.spawnParticleAt(particleType);
            }
        }
    }
}