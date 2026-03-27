package com.trolmastercard.sexmod.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * CameraControlPacket - ported from gz.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent SERVER - CLIENT to lock or unlock the player's movement/camera
 * during a sex scene involving the player.
 *
 * When locked=true:
 *   - Stops player velocity
 *   - Shows HornyMeterOverlay (ds.c() - HornyMeterOverlay.show())
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - IMessage/IMessageHandler - FriendlyByteBuf + handle(Supplier&lt;NetworkEvent.Context&gt;)
 *   - Minecraft.func_71410_x() - Minecraft.getInstance()
 *   - entity.func_70016_h(0,0,0) - entity.setDeltaMovement(0,0,0)
 *   - ClientStateManager.a(bool) - ClientStateManager.setPlayerLocked(bool)
 *   - ds.c() - HornyMeterOverlay.show()
 */
public class CameraControlPacket {

    /** Whether the player should be locked (true) or unlocked (false). */
    private final boolean locked;

    public CameraControlPacket(boolean locked) {
        this.locked = locked;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(CameraControlPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.locked);
    }

    public static CameraControlPacket decode(FriendlyByteBuf buf) {
        return new CameraControlPacket(buf.readBoolean());
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(CameraControlPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                ClientStateManager.setPlayerLocked(msg.locked);
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    }
                } catch (Exception ignored) {}
                try {
                    if (msg.locked) {
                        HornyMeterOverlay.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("received an invalid message @CameraControlPacket :(");
        }
        ctx.setPacketHandled(true);
    }

    // -- Accessors --------------------------------------------------------------

    public boolean isLocked() {
        return locked;
    }
}
