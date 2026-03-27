package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.ModConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * MakeRichWishPacket - ported from bw.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER.
 *
 * Sent when the player makes a "riches" wish (likely from the Allie lamp).
 * The server drops 1-2 gold ingots, diamonds, and emeralds at the given position.
 *
 * Item mapping (1.12.2 - 1.20.1):
 *   Items.field_151045_i  (gold_ingot) - Items.GOLD_INGOT
 *   Items.field_151166_bC (diamond)    - Items.DIAMOND
 *   Items.field_151043_k  (emerald)    - Items.EMERALD
 *
 * Field mapping:
 *   a = pos   (Vec3d - Vec3)
 *   b = valid (fromBytes guard)
 *
 * In 1.12.2:
 *   FMLCommonHandler.instance().getMinecraftServerInstance().func_152344_a(() - {})
 *     - ctx.enqueueWork(...)  (already on server thread in Forge 1.20.1)
 *   (param1MessageContext.getServerHandler()).field_147369_b.field_70170_p
 *     - ctx.getSender().level()
 *   world.func_72838_d(entity) - level.addFreshEntity(entity)
 *   r.f (shared Random)        - ModConstants.RANDOM
 */
public class MakeRichWishPacket {

    final Vec3 pos;

    public MakeRichWishPacket(Vec3 pos) {
        this.pos = pos;
    }

    // =========================================================================
    //  Encode / Decode
    // =========================================================================

    public static void encode(MakeRichWishPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
    }

    public static MakeRichWishPacket decode(FriendlyByteBuf buf) {
        return new MakeRichWishPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    // =========================================================================
    //  Handle  (SERVER side)
    // =========================================================================

    public static void handle(MakeRichWishPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) return;
            ServerLevel level = ctx.getSender().serverLevel();

            double x = msg.pos.x, y = msg.pos.y, z = msg.pos.z;

            ItemEntity gold    = new ItemEntity(level, x, y, z,
                new ItemStack(Items.GOLD_INGOT,    ModConstants.RANDOM.nextInt(2) + 1));
            ItemEntity diamond = new ItemEntity(level, x, y, z,
                new ItemStack(Items.DIAMOND,       ModConstants.RANDOM.nextInt(2) + 1));
            ItemEntity emerald = new ItemEntity(level, x, y, z,
                new ItemStack(Items.EMERALD,       ModConstants.RANDOM.nextInt(2) + 1));

            level.addFreshEntity(gold);
            level.addFreshEntity(diamond);
            level.addFreshEntity(emerald);
        });
        ctx.setPacketHandled(true);
    }
}
