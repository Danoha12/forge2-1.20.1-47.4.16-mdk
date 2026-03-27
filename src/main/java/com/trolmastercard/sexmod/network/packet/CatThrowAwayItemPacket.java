package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ported from dq.java (1.12.2 - 1.20.1)
 * Packet: client - server - instructs a cat-type NPC (eb subclass) identified by UUID
 * to call its j() method (throwAwayItem / drop held item).
 *
 * 1.12.2 used IMessage/IMessageHandler; 1.20.1 uses FriendlyByteBuf encode/decode + handle().
 */
public class CatThrowAwayItemPacket {

    private final UUID ownerUUID;

    public CatThrowAwayItemPacket(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    // -- Serialization ---------------------------------------------------------

    public static CatThrowAwayItemPacket decode(FriendlyByteBuf buf) {
        return new CatThrowAwayItemPacket(UUID.fromString(buf.readUtf()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(ownerUUID.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Server side: find all BaseNpcEntity instances owned by the UUID
            // and cast to NpcInventoryEntity to call throwAwayItem()
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // Iterate all loaded levels
            server.getAllLevels().forEach(level -> {
                ArrayList<BaseNpcEntity> list = BaseNpcEntity.getAllByOwner(level, ownerUUID);
                for (BaseNpcEntity npc : list) {
                    if (level.isClientSide()) continue;
                    if (npc instanceof NpcInventoryEntity inventoryNpc) {
                        inventoryNpc.throwAwayItem(); // was eb.j()
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
