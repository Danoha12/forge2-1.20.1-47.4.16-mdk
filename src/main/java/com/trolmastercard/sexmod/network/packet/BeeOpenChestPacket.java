package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.BeeNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent by the client to ask the server to open the Bee NPC's chest GUI.
 * Ported from f3.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Payload: npcOwnerUUID (String) + playerUUID (String).
 */
public class BeeOpenChestPacket {

    private final UUID npcOwnerUUID;
    private final UUID playerUUID;

    public BeeOpenChestPacket(UUID npcOwnerUUID, UUID playerUUID) {
        this.npcOwnerUUID = npcOwnerUUID;
        this.playerUUID   = playerUUID;
    }

    // -- Codec ----------------------------------------------------------------

    public static void encode(BeeOpenChestPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.npcOwnerUUID.toString());
        buf.writeUtf(pkt.playerUUID.toString());
    }

    public static BeeOpenChestPacket decode(FriendlyByteBuf buf) {
        UUID owner  = UUID.fromString(buf.readUtf());
        UUID player = UUID.fromString(buf.readUtf());
        return new BeeOpenChestPacket(owner, player);
    }

    // -- Handler --------------------------------------------------------------

    public static void handle(BeeOpenChestPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Find all NPC entities that belong to the given owner UUID
            ArrayList<BaseNpcEntity> found = BaseNpcEntity.getAllByOwner(pkt.npcOwnerUUID);
            for (BaseNpcEntity npc : found) {
                if (npc.level().isClientSide()) continue;
                if (!(npc instanceof BeeNpcEntity bee)) continue;
                // Only open chest if bee is in "chest mode" (equivalent of fz.M data parameter)
                if (!bee.isChestMode()) continue;

                ServerPlayer target = (ServerPlayer) npc.level().getPlayerByUUID(pkt.playerUUID);
                if (target == null) continue;

                // Open the NPC inventory/chest screen for the player
                target.openMenu(bee.getChestMenuProvider());
                return;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
