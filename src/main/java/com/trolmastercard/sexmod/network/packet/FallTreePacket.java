package com.trolmastercard.sexmod.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client - Server packet. The client sends the BlockPos of a wood block;
 * the server walks down to the base of the tree and then highlights
 * available bed positions for the sender's tribe.
 * Obfuscated name: fc
 */
public class FallTreePacket {

    private final BlockPos woodPos;

    public FallTreePacket(BlockPos pos) {
        this.woodPos = pos;
    }

    // -- Codec -----------------------------------------------------------------

    public static FallTreePacket decode(FriendlyByteBuf buf) {
        return new FallTreePacket(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.woodPos.getX());
        buf.writeInt(this.woodPos.getY());
        buf.writeInt(this.woodPos.getZ());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid Message @FallTree :(");
                return;
            }

            UUID tribeUUID = TribeManager.getTribeUUIDForPlayer(sender.getUUID());
            if (tribeUUID == null) {
                System.out.println("not tribe for player");
                return;
            }

            int memberCount   = TribeManager.getMemberCount(tribeUUID);
            int bedCount      = (int) Math.floor(TribeManager.getKoboldList(tribeUUID).size() / 2.0D);

            if (memberCount > bedCount) {
                sender.sendSystemMessage(Component.literal(String.format(
                        "cUr Tribe will only work for you, if ceveryoner of them has a cbed")));
                sender.sendSystemMessage(Component.literal(String.format(
                        "e%d/%d Beds", bedCount, memberCount)));
                return;
            }

            Level world = sender.level();
            BlockPos treeBase = findTreeBase(world, this.woodPos);
            HashSet<BlockPos> bedPositions = StructurePlacer.findBedPositions(world, treeBase, tribeUUID);
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                    new TribeHighlightPacket(bedPositions, true));
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Recursively descends to the root of a tree from a wood block,
     * checking all 8 diagonal downward neighbours.
     */
    private static BlockPos findTreeBase(Level world, BlockPos pos) {
        int[][] offsets = {
            {0,-1,0}, {1,-1,0}, {-1,-1,0},
            {0,-1,1}, {0,-1,-1},
            {-1,-1,-1}, {1,-1,1}, {-1,-1,1}, {1,-1,-1}
        };
        for (int[] off : offsets) {
            BlockPos candidate = pos.offset(off[0], off[1], off[2]);
            if (world.getBlockState(candidate).is(BlockTags.LOGS)) {
                return findTreeBase(world, candidate);
            }
        }
        return pos;
    }
}
