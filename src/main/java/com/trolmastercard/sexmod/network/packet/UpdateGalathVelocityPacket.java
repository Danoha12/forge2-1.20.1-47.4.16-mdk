package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * UpdateGalathVelocityPacket - ported from ct.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER.
 *
 * Sent during a Galath sex sequence to update her velocity/position offset on the server.
 * The server resolves the Galath by UUID, then calls {@code galath.d(Vec3d)}
 * (the velocity-update method, renamed {@code updateSexVelocity}) only if the
 * sender is the Galath's current sex partner.
 *
 * Comment in original source: {@code @UpdateVelocity}.
 *
 * Field mapping:
 *   c = valid    (fromBytes guard)
 *   b = pos      (Vec3d - Vec3)
 *   a = npcUUID  (UUID)
 *
 * In 1.12.2:
 *   IMessage/IMessageHandler              - FriendlyByteBuf encode/decode + handle
 *   ByteBufUtils.readUTF8String / write   - FriendlyByteBuf.readUtf / writeUtf
 *   Vec3d.field_72450_a/b/c               - Vec3.x/y/z
 *   em.a(UUID)                            - BaseNpcEntity.getNpcByUUID(UUID)
 *   f_.ab()                               - galath.getSexPartner()
 *   f_.d(Vec3d)                           - galath.updateSexVelocity(Vec3)
 *   FMLCommonHandler.func_152344_a        - ctx.enqueueWork
 */
public class UpdateGalathVelocityPacket {

    private final Vec3 pos;
    private final UUID npcUUID;

    public UpdateGalathVelocityPacket(Vec3 pos, UUID npcUUID) {
        this.pos     = pos;
        this.npcUUID = npcUUID;
    }

    // =========================================================================
    //  Encode / Decode
    // =========================================================================

    public static void encode(UpdateGalathVelocityPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
        buf.writeUtf(msg.npcUUID.toString());
    }

    public static UpdateGalathVelocityPacket decode(FriendlyByteBuf buf) {
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        UUID uuid = UUID.fromString(buf.readUtf());
        return new UpdateGalathVelocityPacket(pos, uuid);
    }

    // =========================================================================
    //  Handle  (SERVER side)
    // =========================================================================

    public static void handle(UpdateGalathVelocityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) {
                System.out.println("received an invalid message @UpdateVelocity :(");
                return;
            }
            BaseNpcEntity npc = BaseNpcEntity.getNpcByUUID(msg.npcUUID);
            if (!(npc instanceof GalathEntity galath)) return;

            // Only allow update from the current sex partner
            if (ctx.getSender().equals(galath.getSexPartner())) {
                galath.updateSexVelocity(msg.pos);
            }
        });
        ctx.setPacketHandled(true);
    }
}
