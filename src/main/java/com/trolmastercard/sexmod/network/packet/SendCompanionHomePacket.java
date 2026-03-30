package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState; // Ajusta al paquete correcto
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendCompanionHomePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Inicia la secuencia de teletransporte (con animación de Perla de Ender)
 * * para enviar al NPC de vuelta a su punto de origen.
 */
public class SendCompanionHomePacket {

    private final UUID npcUUID;

    public SendCompanionHomePacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // ── Codec (Nativo 1.20.1) ────────────────────────────────────────────────

    public static void encode(SendCompanionHomePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUUID); // Más eficiente que convertir a String
    }

    public static SendCompanionHomePacket decode(FriendlyByteBuf buf) {
        return new SendCompanionHomePacket(buf.readUUID());
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(SendCompanionHomePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {

            // Buscar al NPC en el servidor
            List<BaseNpcEntity> npcs = BaseNpcEntity.getAllByUUIDServer(msg.npcUUID);

            for (BaseNpcEntity npc : npcs) {
                if (!(npc.level() instanceof ServerLevel serverLevel)) continue;

                // FASE 1: Iniciar animación y rotar hacia casa
                if (npc.getAnimState() != AnimState.THROW_PEARL) {
                    npc.setAnimState(AnimState.THROW_PEARL);

                    double dx = npc.getHomePos().x - npc.getX();
                    double dz = npc.getHomePos().z - npc.getZ();

                    // Rotación matemática de Minecraft (el +90.0f ajusta el desfase de los ejes)
                    npc.setYRot((float)(Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
                    npc.faceHomeDirection();
                    npc.setIsCasting(true);
                    npc.setCurrentPathTarget(null);
                    continue;
                }

                // FASE 2: Instanciar y lanzar la perla
                if (npc.getPendingPearl() == null) {
                    float dist = (float) npc.position().distanceTo(npc.getHomePos());

                    // En 1.20.1 el constructor toma Level y la entidad disparadora
                    ThrownEnderpearl pearl = new ThrownEnderpearl(serverLevel, npc);

                    double dx = npc.getHomePos().x - npc.getX();
                    double dy = npc.getHomePos().y - npc.getY();
                    double dz = npc.getHomePos().z - npc.getZ();

                    // Limitar la velocidad máxima
                    float speed = Math.min(4.0F, dist * 0.1F);
                    pearl.shoot(dx, dy, dz, speed, 0.0F);

                    serverLevel.addFreshEntity(pearl);
                    npc.setPendingPearl(pearl);
                    continue;
                }

                // FASE 3: Partículas y Teletransporte real
                for (int i = 0; i < 32; i++) {
                    // 1.20.1: particleType, x, y, z, count, dx, dy, dz, speed
                    serverLevel.sendParticles(
                            ParticleTypes.PORTAL,
                            npc.getX(),
                            npc.getY() + serverLevel.random.nextDouble() * 2.0D,
                            npc.getZ(),
                            1,
                            0.2D, 0.2D, 0.2D,
                            serverLevel.random.nextGaussian()
                    );
                }

                npc.teleportTo(npc.getHomePos().x, npc.getHomePos().y, npc.getHomePos().z);
                npc.setPendingPearl(null);
                npc.setAnimState(AnimState.NULL);
                npc.setIsCasting(false);
                npc.onArrivedHome();
            }
        });
        ctx.setPacketHandled(true);
    }
}