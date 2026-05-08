package com.trolmastercard.sexmod.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * TribeHighlightPacket - highlights or un-highlights a set of block positions
 * on the client (tribe task visualization).
 * Ported from h6.class.
 */
public class TribeHighlightPacket {

    private final Set<BlockPos> positions;
    private final boolean highlight;

    public TribeHighlightPacket(Set<BlockPos> positions, boolean highlight) {
        this.positions = positions;
        this.highlight = highlight;
    }

    public TribeHighlightPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.positions = new HashSet<>();
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        this.highlight = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) buf.writeBlockPos(pos);
        buf.writeBoolean(highlight);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: highlight/unhighlight tribe task blocks
        });
        ctx.get().setPacketHandled(true);
    }

    public Set<BlockPos> getPositions() { return positions; }
    public boolean isHighlight() { return highlight; }
}