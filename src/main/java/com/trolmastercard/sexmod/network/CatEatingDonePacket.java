package com.trolmastercard.sexmod.network;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.CatEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CatEatingDonePacket - sent by client when the cat finishes an eating animation.
 * Ported from gk.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original: gk - CatEatingDonePacket
 *   eb.h() - CatEntity.onEatingDone()
 */
public class CatEatingDonePacket {

    private final UUID npcUUID;

    public CatEatingDonePacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // -- Codec -----------------------------------------------------------------

    public static CatEatingDonePacket decode(FriendlyByteBuf buf) {
        return new CatEatingDonePacket(UUID.fromString(buf.readUtf()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcUUID.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                List<BaseNpcEntity> npcs = BaseNpcEntity.getAllByUUIDServer(npcUUID);
                for (BaseNpcEntity npc : npcs) {
                    if (npc.level() instanceof ServerLevel && npc instanceof CatEntity cat) {
                        cat.onEatingDone();
                    }
                }
            } else {
                System.out.println("received an invalid message @CatEatingDone :(");
            }
        });
        context.setPacketHandled(true);
    }
}
