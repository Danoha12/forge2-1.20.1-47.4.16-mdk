package com.trolmastercard.sexmod.network;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendChatMessagePacket - broadcasts an NPC chat message to nearby players.
 * Ported from gh.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Bidirectional:
 *   Server-bound: player requests broadcast from NPC position.
 *   Client-bound: client receives the message string and displays it.
 */
public class SendChatMessagePacket {

    private final String message;
    private final int    dimensionId;
    private final UUID   npcUUID;
    private final boolean valid;

    public SendChatMessagePacket(String message, int dimensionId, UUID npcUUID) {
        this.message     = message;
        this.dimensionId = dimensionId;
        this.npcUUID     = npcUUID;
        this.valid       = true;
    }

    private SendChatMessagePacket() {
        this.message     = "";
        this.dimensionId = 0;
        this.npcUUID     = null;
        this.valid       = false;
    }

    // -- Codec -----------------------------------------------------------------

    public static SendChatMessagePacket decode(FriendlyByteBuf buf) {
        try {
            int len         = buf.readInt();
            byte[] msgBytes = new byte[len];
            buf.readBytes(msgBytes);
            String msg = new String(msgBytes, StandardCharsets.UTF_8);
            int    dim = buf.readInt();
            UUID   uid = UUID.fromString(buf.readUtf());
            return new SendChatMessagePacket(msg, dim, uid);
        } catch (Exception e) {
            System.out.println("couldn't read bytes @SendChatMessage :(");
            return new SendChatMessagePacket();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        buf.writeInt(dimensionId);
        buf.writeUtf(npcUUID.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!valid) {
            System.out.println("received an invalid message @SendChatMessage :(");
            context.setPacketHandled(true);
            return;
        }

        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(this::handleClient);
        } else {
            context.enqueueWork(this::handleServer);
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private void handleServer() {
        // Find NPC and broadcast to nearby players
        List<BaseNpcEntity> npcs = BaseNpcEntity.getAllByUUIDServer(npcUUID);
        if (npcs.isEmpty()) return;
        BaseNpcEntity npc = npcs.get(0);
        Vec3 pos = npc.position();
        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.x, pos.y, pos.z, 40.0, npc.level().dimension())),
                new SendChatMessagePacket(message, dimensionId, npcUUID)
        );
    }
}
