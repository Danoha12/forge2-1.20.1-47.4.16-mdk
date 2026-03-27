package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * DespawnClothingPacket (cz) - CLIENT-SERVER.
 * Removes all ClothingOverlayEntity instances whose owner UUID matches the
 * provided UUID.  Silently ignores client-side worlds.
 */
public class DespawnClothingPacket {

    private final UUID ownerUuid;
    public final boolean isFirstSummon;

    /** Called from AllieEntity with isFirstSummon flag. */
    public DespawnClothingPacket(boolean isFirstSummon) {
        this.ownerUuid = null;
        this.isFirstSummon = isFirstSummon;
    }

    public DespawnClothingPacket(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    // -- Codec -----------------------------------------------------------------

    public static DespawnClothingPacket decode(FriendlyByteBuf buf) {
        return new DespawnClothingPacket(UUID.fromString(buf.readUtf()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(ownerUuid.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            ArrayList<BaseNpcEntity> targets = BaseNpcEntity.getAllByMasterUUID(ownerUuid);
            for (BaseNpcEntity npc : targets) {
                if (npc.level().isClientSide()) continue;
                npc.level().removeEntity(npc, Entity.RemovalReason.DISCARDED);
            }
        });
        context.setPacketHandled(true);
    }
}
