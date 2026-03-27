package com.trolmastercard.sexmod.network.packet;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * SetPlayerCamPacket - ported from aq.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent SERVER - CLIENT. Forces the local player's camera pitch, yaw, and
 * perspective mode to specific values.
 *
 * Used by the sex-animation system to lock the camera in first-person and
 * snap the view to the NPC's position.
 *
 * Fields:
 *   a = pitch       (float - {@code XRot})
 *   b = yaw         (float - {@code YRot})
 *   c = perspective (int   - 0=first, 1=third-back, 2=third-front)
 *
 * In 1.12.2:
 *   - The {@code GameSettings.thirdPersonView} field ({@code field_74320_O}) was
 *     set directly. In 1.20.1 use {@code mc.options.setCameraType(CameraType)}.
 *   - All rotation fields ({@code field_70177_z}, etc.) are now unified:
 *     {@code setYRot}, {@code setXRot}, {@code yRotO}, {@code xRotO},
 *     {@code yHeadRot}, {@code yBodyRot}.
 */
public class SetPlayerCamPacket {

    private final float pitch;
    private final float yaw;
    private final int   perspective;  // 0=first, 1=third-back, 2=third-front
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public SetPlayerCamPacket(float pitch, float yaw, int perspective) {
        this.pitch       = pitch;
        this.yaw         = yaw;
        this.perspective = perspective;
        this.valid       = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static SetPlayerCamPacket decode(FriendlyByteBuf buf) {
        float pitch = buf.readFloat();
        float yaw   = buf.readFloat();
        int   persp = buf.readInt();
        return new SetPlayerCamPacket(pitch, yaw, persp);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(pitch);
        buf.writeFloat(yaw);
        buf.writeInt(perspective);
    }

    // =========================================================================
    //  Handler (CLIENT side)
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getSender() != null) {
            // This packet is server-client only
            System.out.println("received an invalid message @SetPlayerCam :(");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @SetPlayerCam :(");
                return;
            }
            System.out.println(Thread.currentThread().getName());
            applyOnClient(pitch, yaw, perspective);
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyOnClient(float pitch, float yaw, int perspectiveOrdinal) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            // Set perspective
            CameraType[] types = CameraType.values();
            if (perspectiveOrdinal >= 0 && perspectiveOrdinal < types.length) {
                mc.options.setCameraType(types[perspectiveOrdinal]);
            }

            // Snap all yaw/pitch fields
            LocalPlayer player = mc.player;
            if (player == null) return;

            player.setYRot(yaw);
            player.yRotO       = yaw;
            player.yHeadRotO   = yaw;
            player.yHeadRot    = yaw;
            player.yBodyRot    = yaw;
            player.setXRot(pitch);
            player.xRotO       = pitch;
        });
    }
}
