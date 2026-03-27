package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * StartSexAnimationPacket - ported from eu.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER. Triggers a sex animation on a PlayerKoboldEntity (ei) by
 * action name + partner UUID. Finds the ei instance by npc UUID, then calls
 * ei.b(action, playerId) - onActionSelected(action, playerId).
 *
 * Fields: a=npcUUID, b=playerUUID, d=actionName
 *
 * 1.12.2 - 1.20.1:
 *  - IMessage/IMessageHandler - FriendlyByteBuf + handle(Supplier)
 *  - ByteBufUtils.readUTF8String - buf.readUtf()
 *  - ei.d(uuid) - PlayerKoboldEntity.getByUUID(uuid) [client-side lookup]
 *  - FMLCommonHandler.getMinecraftServerInstance().func_152344_a - ctx.enqueueWork
 *  - FMLCommonHandler.instance().getMinecraftServerInstance().func_71262_S() - server.isSingleplayer()
 *  - em.ad() loop - BaseNpcEntity.getAllNpcs()
 */
public class StartSexAnimationPacket {

    private final UUID   npcUUID;
    private final UUID   playerUUID;
    private final String actionName;

    public StartSexAnimationPacket(UUID npcUUID, UUID playerUUID, String actionName) {
        this.npcUUID    = npcUUID;
        this.playerUUID = playerUUID;
        this.actionName = actionName;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(StartSexAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcUUID.toString());
        buf.writeUtf(msg.playerUUID.toString());
        buf.writeUtf(msg.actionName);
    }

    public static StartSexAnimationPacket decode(FriendlyByteBuf buf) {
        return new StartSexAnimationPacket(
                UUID.fromString(buf.readUtf()),
                UUID.fromString(buf.readUtf()),
                buf.readUtf());
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(StartSexAnimationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            System.out.println("received an invalid message @StartSexAnimation :(");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            // Find the PlayerKoboldEntity by its NPC UUID
            PlayerKoboldEntity target = null;

            // Try direct lookup first
            target = PlayerKoboldEntity.getByNpcUUID(msg.npcUUID);

            // Fallback: scan all NPCs (needed in singleplayer where client entities differ)
            if (target == null) {
                for (BaseNpcEntity npc : new ArrayList<>(BaseNpcEntity.getAllNpcs())) {
                    if (npc instanceof PlayerKoboldEntity pk && !npc.level.isClientSide()) {
                        if (pk.getNpcUUID().equals(msg.npcUUID)) {
                            target = pk;
                            break;
                        }
                    }
                }
            }

            if (target == null) return;

            target.onActionSelected(msg.actionName, msg.playerUUID);
        });

        ctx.setPacketHandled(true);
    }
}
