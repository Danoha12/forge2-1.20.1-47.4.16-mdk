package com.trolmastercard.sexmod.network;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendCompanionHomePacket - teleports an NPC to its home position.
 * Ported from gg.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original obfuscation:
 *   gg - SendCompanionHomePacket
 *   fp.THROW_PEARL - AnimState.THROW_PEARL
 *   em.G - BaseNpcEntity.DATA_IS_CASTING (synched boolean for pearl-cast anim)
 *   em.l - npc.homePos (Vec3 home position)
 *   em.x() - npc.onArrivedHome()
 *   ho  - ThrownEnderpearl
 */
public class SendCompanionHomePacket {

    private final UUID npcUUID;

    public SendCompanionHomePacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // -- Codec -----------------------------------------------------------------

    public static SendCompanionHomePacket decode(FriendlyByteBuf buf) {
        return new SendCompanionHomePacket(UUID.fromString(buf.readUtf()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcUUID.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                System.out.println("received an invalid message @SendCompanionHome :(");
                return;
            }
            List<BaseNpcEntity> npcs = BaseNpcEntity.getAllByUUIDServer(npcUUID);
            for (BaseNpcEntity npc : npcs) {
                if (!(npc.level() instanceof ServerLevel serverLevel)) continue;

                if (npc.getAnimState() != AnimState.THROW_PEARL) {
                    // Start pearl-throw animation and face home
                    npc.setAnimState(AnimState.THROW_PEARL);
                    double dx = npc.getX() - npc.getHomePos().x;
                    double dz = npc.getZ() - npc.getHomePos().z;
                    npc.setYRot((float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) + 90.0f);
                    npc.faceHomeDirection();
                    npc.setIsCasting(true);
                    npc.setCurrentPathTarget(null);
                    continue;
                }

                if (npc.getPendingPearl() == null) {
                    // Launch ender pearl toward home
                    float dist = (float) npc.position().distanceTo(npc.getHomePos());
                    ThrownEnderpearl pearl = new ThrownEnderpearl(serverLevel, npc);
                    double dx = npc.getHomePos().x - npc.getX();
                    double dy = npc.getHomePos().y - npc.getY();
                    double dz = npc.getHomePos().z - npc.getZ();
                    float speed = Math.min(4.0f, dist * 0.1f);
                    pearl.shoot(dx, dy, dz, speed, 0.0f);
                    serverLevel.addFreshEntity(pearl);
                    npc.setPendingPearl(pearl);
                    continue;
                }

                // Pearl already thrown - teleport with portal particles
                for (int i = 0; i < 32; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.PORTAL,
                            npc.getX(), npc.getY() + serverLevel.random.nextDouble() * 2.0, npc.getZ(),
                            1, 0.2, 0.2, 0.2, serverLevel.random.nextGaussian()
                    );
                }
                npc.teleportTo(npc.getHomePos().x, npc.getHomePos().y, npc.getHomePos().z);
                npc.setPendingPearl(null);
                npc.setAnimState(AnimState.NULL);
                npc.setIsCasting(false);
                npc.onArrivedHome();
            }
        });
        context.setPacketHandled(true);
    }
}
