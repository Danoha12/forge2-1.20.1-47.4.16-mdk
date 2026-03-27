package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SetPlayerForNpcPacket - ported from b0.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER. Assigns a player UUID ({@code playerUUID}) as the sex
 * partner for all NPCs that share the given master UUID ({@code masterUUID}).
 *
 * If the NPC is an instance of an "extended" sex entity ({@code ex} - likely
 * {@code KoboldSexEntity}), it additionally sets its {@code af} flag to true
 * (which triggers the approach sequence).
 *
 * Field mapping:
 *   c = masterUUID  (UUID of the NPC group)
 *   b = playerUUID  (UUID of the player to assign as sex partner)
 *
 * In 1.12.2:
 *   - {@code ByteBufUtils.readUTF8String/writeUTF8String} - {@code buf.readUtf()/writeUtf()}
 *   - {@code FMLCommonHandler...getMinecraftServerInstance()} - {@code ServerLifecycleHooks.getCurrentServer()}
 *   - {@code playerList.func_177451_a(uuid).func_70005_c_()} - {@code playerList.getPlayer(uuid).getName()}
 *   - {@code IMessage/IMessageHandler} - FriendlyByteBuf + handle()
 */
public class SetPlayerForNpcPacket {

    private final UUID    masterUUID;
    private final UUID    playerUUID;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public SetPlayerForNpcPacket(UUID masterUUID, UUID playerUUID) {
        this.masterUUID = masterUUID;
        this.playerUUID = playerUUID;
        this.valid      = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static SetPlayerForNpcPacket decode(FriendlyByteBuf buf) {
        UUID master = UUID.fromString(buf.readUtf());
        UUID player = UUID.fromString(buf.readUtf());
        return new SetPlayerForNpcPacket(master, player);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
        buf.writeUtf(playerUUID.toString());
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @SetPlayerForGirl :(");
                return;
            }

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

                // Verify player exists
                ServerPlayer target = playerList.getPlayer(playerUUID);
                if (target == null) {
                    System.out.println("couldn't find player with UUID: " + playerUUID);
                    System.out.println("could only find players with these UUID's:");
                    for (ServerPlayer p : playerList.getPlayers()) {
                        System.out.println(p.getName().getString() + " " + p.getUUID());
                    }
                    return;
                }

                ArrayList<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(masterUUID);
                for (BaseNpcEntity npc : npcs) {
                    // Enable approach mode on sex-capable subtypes
                    if (npc instanceof com.trolmastercard.sexmod.entity.KoboldSexEntity sexNpc) {
                        sexNpc.setApproachEnabled(true);  // af = true
                    }
                    npc.setSexPartnerUUID(playerUUID);    // em.e(playerUUID)
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
