package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * OpenNpcInventoryPacket - ported from bo.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT-SERVER. Opens the NPC inventory container (GUI 0) for the given player
 * at the NPC's block position.
 *
 * Field mapping:
 *   a = masterUUID, b = playerUUID
 *
 * In 1.12.2:
 *   player.openGui(Main.instance, 0, world, x, y, z) - player.openMenu(provider)
 *   ByteBufUtils.readUTF8String - buf.readUtf()
 *   FMLCommonHandler-getMinecraftServerInstance() - ServerLifecycleHooks.getCurrentServer()
 */
public class OpenNpcInventoryPacket {

    private final UUID    masterUUID;
    private final UUID    playerUUID;
    private final boolean valid;

    public OpenNpcInventoryPacket(UUID masterUUID, UUID playerUUID) {
        this.masterUUID = masterUUID;
        this.playerUUID = playerUUID;
        this.valid      = true;
    }

    public static OpenNpcInventoryPacket decode(FriendlyByteBuf buf) {
        return new OpenNpcInventoryPacket(
            UUID.fromString(buf.readUtf()),
            UUID.fromString(buf.readUtf()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
        buf.writeUtf(playerUUID.toString());
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) { System.out.println("received an invalid message @OpenNpcInventory :("); return; }
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                BaseNpcEntity npc = null;
                for (BaseNpcEntity c : BaseNpcEntity.getAllActive()) {
                    if (!c.isRemoved() && c.getMasterUUID().equals(masterUUID)) { npc = c; break; }
                }
                if (npc == null) return;
                net.minecraft.server.level.ServerPlayer player =
                    ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
                if (player == null) return;
                final BaseNpcEntity finalNpc = npc;
                player.openMenu(new com.trolmastercard.sexmod.menu.NpcInventoryMenuProvider(finalNpc));
            });
        });
        ctx.setPacketHandled(true);
    }
}
