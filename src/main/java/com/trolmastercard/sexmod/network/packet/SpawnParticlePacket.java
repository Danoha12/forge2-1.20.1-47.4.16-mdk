package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SpawnParticlePacket - ported from en.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * SERVER - CLIENT. Tells the client to spawn N copies of the named particle at
 * every entity whose NPC UUID matches the given UUID.
 *
 * 1.12.2 - 1.20.1:
 *   - IMessage/IMessageHandler - FriendlyByteBuf + handle(Supplier)
 *   - ByteBufUtils - buf.readUtf / buf.writeUtf
 *   - EnumParticleTypes.func_186831_a(name) - ForgeRegistries.PARTICLE_TYPES.getValue(rl)
 *   - em.a(particle, em) - npc.spawnParticle(type)
 *   - em.ad() - BaseNpcEntity.getAllNpcs()
 *   - field_72995_K - level.isClientSide()
 */
public class SpawnParticlePacket {

    private final UUID   npcUUID;
    private final String particleId;
    private final int    count;

    public SpawnParticlePacket(UUID npcUUID, String particleId) {
        this(npcUUID, particleId, 1);
    }

    public SpawnParticlePacket(UUID npcUUID, String particleId, int count) {
        this.npcUUID    = npcUUID;
        this.particleId = particleId;
        this.count      = count;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(SpawnParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcUUID.toString());
        buf.writeUtf(msg.particleId);
        buf.writeInt(msg.count);
    }

    public static SpawnParticlePacket decode(FriendlyByteBuf buf) {
        UUID   id    = UUID.fromString(buf.readUtf());
        String pId   = buf.readUtf();
        int    count = buf.readInt();
        return new SpawnParticlePacket(id, pId, count);
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(SpawnParticlePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isClient()) {
            System.out.println("received an invalid message @SpawnParticle :(");
            ctx.setPacketHandled(true);
            return;
        }
        ctx.enqueueWork(() -> {
            var particleType = ForgeRegistries.PARTICLE_TYPES.getValue(
                    new net.minecraft.resources.ResourceLocation(msg.particleId));
            if (particleType == null) return;

            for (BaseNpcEntity npc : new ArrayList<>(BaseNpcEntity.getAllNpcs())) {
                if (!npc.level.isClientSide()) continue;
                if (!npc.getNpcUUID().equals(msg.npcUUID)) continue;
                for (int i = 0; i < msg.count; i++) {
                    npc.spawnParticleAt(particleType);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
