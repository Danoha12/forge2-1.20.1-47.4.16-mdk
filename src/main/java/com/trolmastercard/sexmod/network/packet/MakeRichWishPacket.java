package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * MakeRichWishPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Invocado por la Lámpara de Allie para generar oro, diamantes y esmeraldas.
 */
public class MakeRichWishPacket {

    private final Vec3 pos;

    public MakeRichWishPacket(Vec3 pos) {
        this.pos = pos;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(MakeRichWishPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
    }

    public static MakeRichWishPacket decode(FriendlyByteBuf buf) {
        return new MakeRichWishPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(MakeRichWishPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Escudo de seguridad vital en 1.20.1
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                if (ctx.getSender() == null) return;

                ServerLevel level = ctx.getSender().serverLevel();
                double x = msg.pos.x, y = msg.pos.y, z = msg.pos.z;

                // Usamos level.getRandom() que es seguro para multihilos en la 1.20.1
                ItemEntity gold = new ItemEntity(level, x, y, z,
                        new ItemStack(Items.GOLD_INGOT, level.getRandom().nextInt(2) + 1));
                ItemEntity diamond = new ItemEntity(level, x, y, z,
                        new ItemStack(Items.DIAMOND, level.getRandom().nextInt(2) + 1));
                ItemEntity emerald = new ItemEntity(level, x, y, z,
                        new ItemStack(Items.EMERALD, level.getRandom().nextInt(2) + 1));

                // Otorgamos el deseo
                level.addFreshEntity(gold);
                level.addFreshEntity(diamond);
                level.addFreshEntity(emerald);
            });
        }
        ctx.setPacketHandled(true);
    }
}