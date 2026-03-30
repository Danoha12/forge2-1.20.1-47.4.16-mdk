package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.tribe.TribeTask;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MineBlocksPacket — Portado a 1.20.1.
 * * Variante/Clon de MineAreaPacket (e6.class).
 */
public class MineBlocksPacket {

    final BlockPos pos;
    final Direction facing;

    public MineBlocksPacket(BlockPos pos, Direction facing) {
        this.pos = pos;
        this.facing = facing;
    }

    // ── Serialización ────────────────────────────────────────────────────────

    public static void encode(MineBlocksPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.facing);
    }

    public static MineBlocksPacket decode(FriendlyByteBuf buf) {
        return new MineBlocksPacket(buf.readBlockPos(), buf.readEnum(Direction.class));
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(MineBlocksPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (!ctx.getDirection().getReceptionSide().isServer()) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            UUID tribeId = TribeManager.getTribeOf(sender.getUUID()); // Asumo que usas getTribeOf aquí
            if (tribeId == null) return;

            int members = TribeManager.getMemberCount(tribeId);
            int beds = TribeManager.getBedList(tribeId).size() / 2;

            if (members > beds) {
                sender.sendSystemMessage(Component.literal("Tribe will only work for you if every one of them has a bed")
                        .withStyle(ChatFormatting.RED));
                sender.sendSystemMessage(Component.literal(beds + "/" + members + " Beds")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }

            HashSet<BlockPos> blocks = collectTunnelBlocks(msg.pos, msg.facing);

            for (BlockPos bp : blocks) {
                BlockState state = sender.level().getBlockState(bp);
                if (state.getDestroySpeed(sender.level(), bp) < 0.0F) {
                    sender.displayClientMessage(
                            Component.literal("This area contains Bedrock and cannot be mined").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }

            TribeTask task = new TribeTask(msg.pos, TribeTask.Type.MINE, blocks, msg.facing);
            TribeManager.assignTask(tribeId, task);

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new TribeHighlightPacket(blocks, true));
        });
        ctx.setPacketHandled(true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static HashSet<BlockPos> collectTunnelBlocks(BlockPos origin, Direction facing) {
        HashSet<BlockPos> set = new HashSet<>();
        BlockPos cursor = origin;
        Direction perp = facing.getClockWise(); // Perpendicular

        for (int i = 0; i < 30; i++) {
            BlockPos inward = cursor.relative(perp.getOpposite());
            BlockPos outward = cursor.relative(perp);

            for (BlockPos col : new BlockPos[]{ cursor, inward, outward }) {
                set.add(col);
                set.add(col.above());
                set.add(col.above(2)); // Más limpio que col.above().above()
            }

            // Avanzamos el cursor hacia el frente de forma recta y segura
            cursor = cursor.relative(facing);
        }
        return set;
    }
}