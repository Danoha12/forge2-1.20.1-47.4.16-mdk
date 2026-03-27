package com.trolmastercard.sexmod.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MineAreaPacket - ported from e6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER. Requests that the player's tribe begin mining a 3-30 column
 * of blocks in the direction the player is facing when they right-click with the
 * staff/wand on a block.
 *
 * Server handler:
 *   1. Verifies the sender has an owned tribe via TribeManager.
 *   2. Checks that enough beds exist for the tribe size (i.e. beds - tribe_size/2).
 *   3. Builds the mining block set (3 wide - 30 deep - 3 tall).
 *   4. Validates none of the blocks have negative hardness (bedrock).
 *   5. Creates a {@link TribeTask} with mode MINE and registers it.
 *   6. Sends a {@link TribeHighlightPacket} back to the client to highlight
 *      the mined column.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - IMessage/IMessageHandler - FriendlyByteBuf + handle(Supplier<NetworkEvent.Context>)
 *   - ByteBufUtils.readUTF8String - buf.readUtf
 *   - EnumFacing.func_176739_a(str) - Direction.byName(str)
 *   - EnumFacing.func_176730_m() - direction.getAxis()
 *   - blockPos.func_177973_b - blockPos.relative(dir)
 *   - blockPos.func_177984_a - blockPos.above()
 *   - blockPos.func_177971_a - blockPos.relative(offset, n)
 *   - Vec3i.func_177958_n/o/p - getX/getY/getZ
 *   - world.func_180495_p(pos) - level.getBlockState(pos)
 *   - IBlockState.func_177230_c().func_176195_g - block.defaultDestroyTime()
 *   - FMLCommonHandler.getMinecraftServerInstance().func_152344_a - server.execute()
 *   - entityPlayerMP.func_145747_a - player.sendSystemMessage
 *   - entityPlayerMP.func_146105_b - player.displayClientMessage
 *   - ge.b.sendTo(packet, player) - ModNetwork.CHANNEL.send(PLAYER, packet)
 *   - h6 - TribeHighlightPacket
 *   - ax.a(uuid) - TribeManager.getTribeIdFor(uuid)
 *   - ax.h(tribeId) - TribeManager.getMemberCount(tribeId)
 *   - ax.j(tribeId) - TribeManager.getBedList(tribeId)
 *   - ax.b(uuid, task) - TribeManager.assignTask(uuid, task)
 *   - bs.a.MINE - TribeTask.Mode.MINE
 */
public class MineAreaPacket {

    private final BlockPos origin;
    private final Direction facing;

    public MineAreaPacket(BlockPos origin, Direction facing) {
        this.origin = origin;
        this.facing = facing;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(MineAreaPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.origin.getX());
        buf.writeInt(msg.origin.getY());
        buf.writeInt(msg.origin.getZ());
        buf.writeUtf(msg.facing.getName());
    }

    public static MineAreaPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        Direction dir = Direction.byName(buf.readUtf());
        if (dir == null) dir = Direction.NORTH;
        return new MineAreaPacket(pos, dir);
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(MineAreaPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            System.out.println("received an invalid Message @MineArea :(");
            ctx.setPacketHandled(true);
            return;
        }
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            UUID tribeId = TribeManager.getTribeIdFor(player.getUUID());
            if (tribeId == null) return;

            int memberCount = TribeManager.getMemberCount(tribeId);
            int bedCount    = TribeManager.getBedList(tribeId).size() / 2;
            if (memberCount > bedCount) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cYour Tribe will only work for you, if \u00a7ceveryone\u00a7f of them has a \u00a7cbed"));
                player.sendSystemMessage(Component.literal(
                        "\u00a7e" + bedCount + "/" + memberCount + " Beds"));
                return;
            }

            HashSet<BlockPos> blocks = buildMiningArea(msg.origin, msg.facing);

            // Validate - no bedrock-level hardness
            for (BlockPos pos : blocks) {
                var blockState = player.level.getBlockState(pos);
                if (blockState.getDestroySpeed(player.level, pos) < 0.0F) {
                    player.displayClientMessage(
                            Component.literal("This area contains Bedrock and cannot be mined"), true);
                    return;
                }
            }

            TribeTask task = new TribeTask(msg.origin, TribeTask.Mode.MINE, blocks, msg.facing);
            TribeManager.assignTask(tribeId, task);

            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new TribeHighlightPacket(blocks, true));
        });
        ctx.setPacketHandled(true);
    }

    // -- Mining area builder ----------------------------------------------------

    /**
     * Builds a HashSet of BlockPos for a 3-wide - 30-deep - 3-tall column
     * extending from {@code origin} in the given {@code direction}.
     *
     * Side axis is the axis perpendicular to the facing direction on the
     * horizontal plane (rotated 90-): sideAxis = (z, 0, -x) for facing (x, 0, z).
     */
    static HashSet<BlockPos> buildMiningArea(BlockPos origin, Direction facing) {
        HashSet<BlockPos> set = new HashSet<>();
        BlockPos cursor = origin;
        Direction sideDir = getSideDirection(facing);

        for (int depth = 0; depth < 30; depth++) {
            // Centre column + one to each side
            BlockPos left  = cursor.relative(sideDir, -1);
            BlockPos right = cursor.relative(sideDir,  1);

            // Add 3 heights at each lateral position
            for (BlockPos bp : new BlockPos[]{ left, cursor, right }) {
                set.add(bp);
                set.add(bp.above());
                set.add(bp.above(2));
            }

            cursor = cursor.relative(facing.getOpposite().getAxis() == net.minecraft.core.Direction.Axis.X
                    ? facing : facing);
        }
        return set;
    }

    /** Returns the horizontal perpendicular direction to the given facing direction. */
    private static Direction getSideDirection(Direction facing) {
        var axis = facing.getAxis();
        // Rotate 90- around Y: (x,0,z) - (z, 0, -x)
        int sx = facing.getStepZ();
        int sz = -facing.getStepX();
        if (sx > 0) return Direction.EAST;
        if (sx < 0) return Direction.WEST;
        if (sz > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }
}
