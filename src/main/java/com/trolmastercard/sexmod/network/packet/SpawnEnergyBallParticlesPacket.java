package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * SpawnEnergyBallParticlesPacket - ported from bv.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * SERVER - CLIENT.
 *
 * Two modes controlled by {@code isExplosion}:
 *   true  - ring of smoke particles around the position ({@link EnergyBallEntity#spawnRingParticles})
 *   false - dragon-breath burst + shatter sound ({@link EnergyBallEntity#spawnDeathParticles})
 *
 * Field mapping:
 *   a = pos          (Vec3d - Vec3)
 *   c = isExplosion  (boolean - ring vs burst)
 *   b = initialized  (fromBytes guard)
 *
 * In 1.12.2:
 *   IMessage / IMessageHandler - FriendlyByteBuf encode/decode + handle(Supplier<NetworkEvent.Context>)
 *   Vec3d.field_72450_a/b/c    - Vec3.x/y/z
 *   param1MessageContext.side  - ctx.get().getDirection().getReceptionSide()
 *   c4.a(vec3d) / c4.c(vec3d) - EnergyBallEntity.spawnRingParticles / spawnDeathParticles
 */
public class SpawnEnergyBallParticlesPacket {

    final Vec3 pos;
    final boolean isExplosion;

    public SpawnEnergyBallParticlesPacket(Vec3 pos, boolean isExplosion) {
        this.pos         = pos;
        this.isExplosion = isExplosion;
    }

    // =========================================================================
    //  Encode / Decode
    // =========================================================================

    public static void encode(SpawnEnergyBallParticlesPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
        buf.writeBoolean(msg.isExplosion);
    }

    public static SpawnEnergyBallParticlesPacket decode(FriendlyByteBuf buf) {
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        boolean explosion = buf.readBoolean();
        return new SpawnEnergyBallParticlesPacket(pos, explosion);
    }

    // =========================================================================
    //  Handle  (CLIENT side)
    // =========================================================================

    public static void handle(SpawnEnergyBallParticlesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (msg.isExplosion) {
                EnergyBallEntity.spawnRingParticles(msg.pos);
            } else {
                EnergyBallEntity.spawnDeathParticles(msg.pos);
            }
        });
        ctx.setPacketHandled(true);
    }
}
