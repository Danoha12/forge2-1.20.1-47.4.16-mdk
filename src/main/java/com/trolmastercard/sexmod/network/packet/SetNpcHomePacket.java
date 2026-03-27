package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SetNpcHomePacket - ported from a6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER to update the home position ({@code em.l}) of all
 * NPCs that share the given master UUID.
 *
 * The Y-coordinate is floored before being stored (the NPC's home is always
 * on the block surface).
 */
public class SetNpcHomePacket {

    private final UUID    masterUUID;
    private final Vec3    homePos;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public SetNpcHomePacket(UUID masterUUID, Vec3 homePos) {
        this.masterUUID = masterUUID;
        this.homePos    = homePos;
        this.valid      = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static SetNpcHomePacket decode(FriendlyByteBuf buf) {
        UUID uuid = UUID.fromString(buf.readUtf());
        Vec3 pos  = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new SetNpcHomePacket(uuid, pos);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
        buf.writeDouble(homePos.x);
        buf.writeDouble(homePos.y);
        buf.writeDouble(homePos.z);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @SetNewHome :(");
                return;
            }

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                ArrayList<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(masterUUID);
                if (npcs.isEmpty()) return;

                Vec3 snapped = new Vec3(homePos.x, Math.floor(homePos.y), homePos.z);
                for (BaseNpcEntity npc : npcs) {
                    npc.setHomePosition(snapped);   // em.l = snapped
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
