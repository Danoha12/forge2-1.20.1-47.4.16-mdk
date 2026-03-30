package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.tribe.TribeManager;
// NOTA: Asumo que tienes una clase StructurePlacer
import com.trolmastercard.sexmod.world.StructurePlacer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * FallTreePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * El cliente envía la posición de un bloque de madera; el servidor baja hasta la base
 * * del árbol y resalta las posiciones de cama disponibles para la tribu del jugador.
 */
public class FallTreePacket {

    private final BlockPos woodPos;

    public FallTreePacket(BlockPos pos) {
        this.woodPos = pos;
    }

    // ── Codec (Optimizado) ───────────────────────────────────────────────────

    public static void encode(FallTreePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.woodPos); // ¡Método nativo mucho más limpio!
    }

    public static FallTreePacket decode(FriendlyByteBuf buf) {
        return new FallTreePacket(buf.readBlockPos());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(FallTreePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) {
                    System.out.println("[SexMod] Error: Mensaje FallTree inválido (sin remitente).");
                    return;
                }

                UUID tribeUUID = TribeManager.getTribeUUIDForPlayer(sender.getUUID());
                if (tribeUUID == null) {
                    System.out.println("[SexMod] Error: El jugador no tiene tribu.");
                    return;
                }

                int memberCount = TribeManager.getMemberCount(tribeUUID);
                int bedCount = (int) Math.floor(TribeManager.getKoboldList(tribeUUID).size() / 2.0D);

                // Verificamos si hay suficientes camas
                if (memberCount > bedCount) {
                    sender.sendSystemMessage(Component.literal("Ur Tribe will only work for you, if everyone of them has a bed")
                            .withStyle(ChatFormatting.RED));
                    sender.sendSystemMessage(Component.literal(bedCount + "/" + memberCount + " Beds")
                            .withStyle(ChatFormatting.YELLOW));
                    return;
                }

                Level world = sender.level();
                BlockPos treeBase = findTreeBase(world, msg.woodPos);

                // Calculamos dónde poner las camas y le enviamos las coordenadas al cliente
                HashSet<BlockPos> bedPositions = StructurePlacer.findBedPositions(world, treeBase, tribeUUID);
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new TribeHighlightPacket(bedPositions, true)
                );
            });
        }
        ctx.setPacketHandled(true);
    }

    // ── Buscador de Base de Árbol (Iterativo y Seguro) ───────────────────────

    /**
     * Desciende hasta la raíz del árbol de forma iterativa (para evitar StackOverflow).
     * Revisa los 8 vecinos diagonales hacia abajo.
     */
    private static BlockPos findTreeBase(Level world, BlockPos startPos) {
        BlockPos currentPos = startPos;
        boolean foundLower = true;

        int[][] offsets = {
                {0, -1, 0}, {1, -1, 0}, {-1, -1, 0},
                {0, -1, 1}, {0, -1, -1},
                {-1, -1, -1}, {1, -1, 1}, {-1, -1, 1}, {1, -1, -1}
        };

        // Bucle while en lugar de recursión.
        // ¡Súper seguro sin importar qué tan alto sea el árbol!
        while (foundLower) {
            foundLower = false;
            for (int[] off : offsets) {
                BlockPos candidate = currentPos.offset(off[0], off[1], off[2]);
                if (world.getBlockState(candidate).is(BlockTags.LOGS)) {
                    currentPos = candidate;
                    foundLower = true;
                    break; // Encontramos un bloque más abajo, reiniciamos la búsqueda desde ahí
                }
            }
        }
        return currentPos;
    }
}