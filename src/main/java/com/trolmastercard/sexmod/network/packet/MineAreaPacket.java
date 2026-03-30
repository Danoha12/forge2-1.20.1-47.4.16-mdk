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
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MineAreaPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Ordena a la tribu minar una columna de 3x30 bloques en la dirección mirada.
 */
public class MineAreaPacket {

    private final BlockPos origin;
    private final Direction facing;

    public MineAreaPacket(BlockPos origin, Direction facing) {
        this.origin = origin;
        this.facing = facing;
    }

    // ── Serialización (Optimizada 1.20.1) ────────────────────────────────────

    public static void encode(MineAreaPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.origin);
        buf.writeEnum(msg.facing);
    }

    public static MineAreaPacket decode(FriendlyByteBuf buf) {
        return new MineAreaPacket(
                buf.readBlockPos(),
                buf.readEnum(Direction.class)
        );
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(MineAreaPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (!ctx.getDirection().getReceptionSide().isServer()) {
            System.out.println("[SexMod] Error: MineAreaPacket recibido en el lado equivocado.");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            UUID tribeId = TribeManager.getTribeIdFor(player.getUUID());
            if (tribeId == null) return;

            int memberCount = TribeManager.getMemberCount(tribeId);
            int bedCount = TribeManager.getBedList(tribeId).size() / 2;

            if (memberCount > bedCount) {
                player.sendSystemMessage(Component.literal("Your Tribe will only work for you, if everyone of them has a bed")
                        .withStyle(ChatFormatting.RED));
                player.sendSystemMessage(Component.literal(bedCount + "/" + memberCount + " Beds")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }

            HashSet<BlockPos> blocks = buildMiningArea(msg.origin, msg.facing);

            // Validar que no haya Bedrock (dureza < 0)
            for (BlockPos pos : blocks) {
                var blockState = player.level().getBlockState(pos);
                if (blockState.getDestroySpeed(player.level(), pos) < 0.0F) {
                    player.displayClientMessage(
                            Component.literal("This area contains Bedrock and cannot be mined").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }

            // Asignar tarea y enviar resaltado al cliente
            TribeTask task = new TribeTask(msg.origin, TribeTask.Mode.MINE, blocks, msg.facing);
            TribeManager.assignTask(tribeId, task);

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new TribeHighlightPacket(blocks, true)
            );
        });
        ctx.setPacketHandled(true);
    }

    // ── Constructor de Área de Minería ───────────────────────────────────────

    /**
     * Construye un HashSet de BlockPos para una columna de 3 de ancho × 30 de largo × 3 de alto.
     */
    static HashSet<BlockPos> buildMiningArea(BlockPos origin, Direction facing) {
        HashSet<BlockPos> set = new HashSet<>();
        BlockPos cursor = origin;

        // Magia de la 1.20.1: getClockWise() nos da la dirección perpendicular al instante
        Direction sideDir = facing.getClockWise();

        for (int depth = 0; depth < 30; depth++) {
            // Columna central + una a cada lado
            BlockPos left = cursor.relative(sideDir.getOpposite()); // O usar getCounterClockWise()
            BlockPos right = cursor.relative(sideDir);

            // Añadir 3 bloques de altura en cada posición lateral
            for (BlockPos bp : new BlockPos[]{ left, cursor, right }) {
                set.add(bp);
                set.add(bp.above());
                set.add(bp.above(2));
            }

            // Avanzar el cursor hacia adelante
            cursor = cursor.relative(facing);
        }
        return set;
    }
}