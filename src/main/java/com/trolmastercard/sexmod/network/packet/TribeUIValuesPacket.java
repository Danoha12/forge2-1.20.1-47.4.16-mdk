package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.KoboldEntity;

import com.trolmastercard.sexmod.client.screen.TribeScreen;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec4;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.Supplier;

/**
 * TribeUIValuesPacket - ported from b3.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Bidirectional packet used to drive the tribe minimap UI ({@code j.d} / {@link TribeScreen}).
 *
 * CLIENT - SERVER (empty packet): requests updated tribe data for the sending player.
 * SERVER - CLIENT (data packet): delivers member positions and "active tribe" flag.
 *
 * Payload:
 *   b = hasTribe  (boolean - true if the player's tribe exists)
 *   c = positions (List<double[4]> - x, y, z, colorIndex for each tribe member)
 *
 * Each position entry maps to the original {@code Vector4d(x, y, z, woolMeta)}.
 *
 * If the player has no tribe, an empty packet with {@code hasTribe=false} is sent back.
 *
 * In 1.12.2:
 *   - {@code javax.vecmath.Vector4d} - plain {@code double[4]} array (no external dep)
 *   - {@code ge.b.sendTo(packet, player)} - {@code ModNetwork.CHANNEL.send(...)}
 *   - {@code ff.aY} - {@code KoboldEntity.tribeHighlightPositions} (static client list)
 *   - {@code ax.*} - {@code TribeManager.*}
 *   - {@code EyeAndKoboldColor.safeValueOf(...).getWoolMeta()} - {@code KoboldColor.getWoolIndex(...)}
 *   - {@code IMessage/IMessageHandler} - FriendlyByteBuf + handle()
 */
public class TribeUIValuesPacket {

    private final boolean      hasTribe;
    private final List<double[]> positions;  // each: [x, y, z, colorIndex]
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public TribeUIValuesPacket(boolean hasTribe, List<double[]> positions) {
        this.hasTribe  = hasTribe;
        this.positions = positions;
        this.valid     = true;
    }

    /** Empty request packet (CLIENT - SERVER). */
    public static TribeUIValuesPacket request() {
        return new TribeUIValuesPacket(false, new ArrayList<>());
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static TribeUIValuesPacket decode(FriendlyByteBuf buf) {
        boolean hasTribe = buf.readBoolean();
        int count = buf.readInt();
        List<double[]> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(new double[]{
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
            });
        }
        return new TribeUIValuesPacket(hasTribe, positions);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasTribe);
        buf.writeInt(positions.size());
        for (double[] p : positions) {
            buf.writeInt((int) p[0]);
            buf.writeInt((int) p[1]);
            buf.writeInt((int) p[2]);
            buf.writeInt((int) p[3]);
        }
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @GetTribeUIValues :(");
                return;
            }

            ServerPlayer sender = ctx.getSender();

            if (sender == null) {
                // ---- CLIENT SIDE: store data for the HUD ----
                TribeScreen.hasTribe = hasTribe;
                KoboldEntity.setTribeHighlightPositions(positions);
                return;
            }

            // ---- SERVER SIDE: build and send response ----
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                UUID playerUUID = sender.getUUID();
                UUID tribeId    = TribeManager.getTribeIdForMaster(playerUUID);

                if (tribeId == null) {
                    com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        TribeUIValuesPacket.request());
                    return;
                }

                boolean hasTribeResult = TribeManager.isActive(tribeId);
                Map<UUID, BlockPos> savedPositions = TribeManager.getSavedPositions(tribeId, sender.level());
                List<KoboldEntity> loaded = TribeManager.getMembersLoaded(tribeId);

                List<double[]> result = new ArrayList<>();
                int baseColor = TribeManager.getTribeColor(tribeId).getWoolMeta();
                Set<UUID> seen = new HashSet<>();

                // Add loaded (live) member positions
                for (KoboldEntity kob : loaded) {
                    if (kob.isRemoved()) continue;
                    UUID kid = kob.getKoboldUUID();
                    if (seen.contains(kid)) continue;

                    int color = baseColor;
                    if (kob.isLeader()) {
                        color = com.trolmastercard.sexmod.entity.KoboldColor.safeValueOf(
                            kob.getLeaderColorName()).getWoolMeta();
                    }
                    result.add(new double[]{kob.getX(), kob.getY(), kob.getZ(), color});
                    seen.add(kid);
                }

                // Add saved (unloaded) positions
                for (Map.Entry<UUID, BlockPos> entry : savedPositions.entrySet()) {
                    if (seen.contains(entry.getKey())) continue;
                    BlockPos bp = entry.getValue();
                    result.add(new double[]{bp.getX(), bp.getY(), bp.getZ(), baseColor});
                }

                com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new TribeUIValuesPacket(hasTribeResult, result));
            });
        });
        ctx.setPacketHandled(true);
    }
}
