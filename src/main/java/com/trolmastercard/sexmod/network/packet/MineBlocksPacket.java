package com.trolmastercard.sexmod.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MineBlocksPacket - ported from e6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER when the player uses the staff to order tribe members to
 * mine a 3-wide - 30-deep tunnel in the given direction.
 *
 * Server logic:
 *   1. Resolves the tribe by sender UUID.
 *   2. Verifies tribe has enough beds (- member count / 2).
 *   3. Collects a 3-30 column of block positions.
 *   4. Rejects if any block is indestructible (hardness < 0).
 *   5. Creates a {@link TribeTask} of type MINE and registers it.
 *   6. Echoes a {@link TribeHighlightPacket} back so the client highlights the area.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - IMessage/IMessageHandler - FriendlyByteBuf encode/decode + handle
 *   - ByteBufUtils.readUTF8String - buf.readUtf()
 *   - EnumFacing.func_176739_a - Direction.byName
 *   - EnumFacing.func_176730_m - direction.getAxis()
 *   - blockPos.func_177973_b / func_177971_a / func_177984_a - offset / above
 *   - IBlockState.func_177230_c().func_176195_g - Block.defaultDestroyTime
 *   - FMLCommonHandler.getMinecraftServerInstance().func_152344_a - server.execute
 *   - entityPlayerMP.func_145747_a - sendSystemMessage
 *   - EntityPlayerMP.getPersistentID - getUUID
 *   - ge.b.sendTo - ModNetwork.CHANNEL.send(PacketDistributor.PLAYER)
 *   - bs - TribeTask; bs.a.MINE - TribeTask.Type.MINE
 *   - ax.a(uuid) - TribeManager.getTribeOf(uuid)
 *   - ax.h(uuid) - TribeManager.getMemberCount(uuid)
 *   - ax.j(uuid) - TribeManager.getBedList(uuid)
 *   - ax.b(uuid, task) - TribeManager.assignTask(uuid, task)
 *   - h6 - TribeHighlightPacket
 */
public class MineBlocksPacket {

    final BlockPos pos;
    final Direction facing;

    public MineBlocksPacket(BlockPos pos, Direction facing) {
        this.pos    = pos;
        this.facing = facing;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(MineBlocksPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.pos.getX());
        buf.writeInt(msg.pos.getY());
        buf.writeInt(msg.pos.getZ());
        buf.writeUtf(msg.facing.getName());
    }

    public static MineBlocksPacket decode(FriendlyByteBuf buf) {
        BlockPos   pos    = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        Direction  facing = Direction.byName(buf.readUtf());
        if (facing == null) facing = Direction.NORTH;
        return new MineBlocksPacket(pos, facing);
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(MineBlocksPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            UUID tribeId = TribeManager.getTribeOf(sender.getUUID());
            if (tribeId == null) return;

            // Bed count gate: each member needs a bed
            int members = TribeManager.getMemberCount(tribeId);
            int beds    = TribeManager.getBedList(tribeId).size() / 2;
            if (members > beds) {
                sender.sendSystemMessage(Component.literal(
                        String.format("cTribe will only work for you if feveryc one of them has a fbed")));
                sender.sendSystemMessage(Component.literal(
                        String.format("e%d/%d Beds", beds, members)));
                return;
            }

            // Collect block positions (3-wide - 30-deep tunnel)
            HashSet<BlockPos> blocks = collectTunnelBlocks(msg.pos, msg.facing);

            // Reject if any block is indestructible
            for (BlockPos bp : blocks) {
                BlockState state = sender.level.getBlockState(bp);
                if (state.getDestroySpeed(sender.level, bp) < 0.0F) {
                    sender.displayClientMessage(
                            Component.literal("This area contains Bedrock and cannot be mined"), true);
                    return;
                }
            }

            // Register mine task
            TribeTask task = new TribeTask(msg.pos, TribeTask.Type.MINE, blocks, msg.facing);
            TribeManager.assignTask(tribeId, task);

            // Echo highlight back to client
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                    new TribeHighlightPacket(blocks, true));
        });
        ctx.setPacketHandled(true);
    }

    // -- Helpers ----------------------------------------------------------------

    /**
     * Collects a 3-wide - 3-tall - 30-deep column of blocks in the given direction.
     * Original logic from e6.a.a(BlockPos, EnumFacing):
     *   - centre column
     *   - offset +1 and -1 perpendicular to facing
     *   - three layers high (pos, pos.above, pos.above.above)
     */
    static HashSet<BlockPos> collectTunnelBlocks(BlockPos origin, Direction facing) {
        HashSet<BlockPos> set     = new HashSet<>();
        BlockPos          cursor  = origin;
        Direction         perp    = perpendicularOf(facing);

        for (int i = 0; i < 30; i++) {
            BlockPos inward = cursor.relative(perp.getOpposite());
            BlockPos outward = cursor.relative(perp);

            for (BlockPos col : new BlockPos[]{ cursor, inward, outward }) {
                set.add(col);
                set.add(col.above());
                set.add(col.above().above());
            }
            cursor = cursor.relative(facing.getClockWise().getAxis().getPositiveDirection().equals(facing)
                    ? facing : Direction.NORTH);
            // Advance along facing's perpendicular axis (mirrors original Vec3i rotation)
            cursor = origin.offset(0, 0, 0).relative(
                    facing.getCounterClockWise(), i + 1);
        }
        return set;
    }

    /**
     * Returns a direction perpendicular to {@code facing} (rotated 90- on the Y axis).
     * Mirrors original: vec3i(facing.axis) rotated (z--x, x-z).
     */
    static Direction perpendicularOf(Direction facing) {
        return facing.getClockWise();
    }
}
