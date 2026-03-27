package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client - Server packet sent when a player names and claims a Kobold tribe.
 * Obfuscated name: g9
 */
public class ClaimTribePacket {

    private final UUID   tribeUUID;
    private final UUID   playerUUID;
    private final String tribeName;

    public ClaimTribePacket(UUID tribeUUID, UUID playerUUID, String tribeName) {
        this.tribeUUID  = tribeUUID;
        this.playerUUID = playerUUID;
        this.tribeName  = tribeName;
    }

    // -- Codec -----------------------------------------------------------------

    public static ClaimTribePacket decode(FriendlyByteBuf buf) {
        UUID   tribe  = UUID.fromString(buf.readUtf());
        UUID   player = UUID.fromString(buf.readUtf());
        String name   = buf.readUtf();
        return new ClaimTribePacket(tribe, player, name);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.tribeUUID.toString());
        buf.writeUtf(this.playerUUID.toString());
        buf.writeUtf(this.tribeName);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid message @ClaimTribe :(");
                return;
            }

            List<KoboldEntity> members = TribeManager.getKoboldList(this.tribeUUID);
            KoboldColorVariant tribeColor = null;

            for (KoboldEntity kobold : members) {
                if (kobold.isSitting()) continue;
                EntityDataAccessor<String> ownerKey = BaseNpcEntity.DATA_OWNER_UUID;
                kobold.getEntityData().set(ownerKey, this.playerUUID.toString());
                kobold.getEntityData().set(KoboldEntity.DATA_TRIBE_NAME, this.tribeName);
                String colorStr = (String) kobold.getEntityData().get(KoboldEntity.DATA_COLOR);
                tribeColor = KoboldColorVariant.valueOf(colorStr);
            }

            if (tribeColor == null) return;

            // Broadcast tribe formation to all online players
            String senderName = sender.getName().getString();
            String colorFormatting = tribeColor.getTextColor();
            Component msg = Component.literal(String.format(
                    "%s formed the " + colorFormatting + "%s rTribe",
                    senderName, this.tribeName));

            for (Player online : sender.server.getPlayerList().getPlayers()) {
                online.sendSystemMessage(msg);
            }

            TribeManager.setTribeActive(this.tribeUUID, true);
            TribeManager.setTribeOwner(this.tribeUUID, sender.getUUID());
        });
        ctx.setPacketHandled(true);
    }
}
