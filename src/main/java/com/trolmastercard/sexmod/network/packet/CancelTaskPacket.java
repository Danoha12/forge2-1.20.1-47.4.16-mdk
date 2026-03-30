package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CancelTaskPacket — Portado a 1.20.1 y optimizado.
 * * Enviado desde el CLIENTE al SERVIDOR cuando un jugador cancela una tarea de la tribu.
 * El servidor elimina la tarea del TribeManager y sincroniza visualmente con el cliente.
 */
public class CancelTaskPacket {

  private final BlockPos pos;

  public CancelTaskPacket(BlockPos pos) {
    this.pos = pos;
  }

  // ── Codificación / Decodificación ─────────────────────────────────────────

  public static void encode(CancelTaskPacket msg, FriendlyByteBuf buf) {
    // En 1.20.1 usamos el método nativo para BlockPos, es más eficiente que 3 ints
    buf.writeBlockPos(msg.pos);
  }

  public static CancelTaskPacket decode(FriendlyByteBuf buf) {
    return new CancelTaskPacket(buf.readBlockPos());
  }

  // ── Manejador (Handler) ───────────────────────────────────────────────────

  public static void handle(CancelTaskPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
    NetworkEvent.Context ctx = ctxSupplier.get();

    // Las tareas solo se gestionan en el servidor
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getSender();
      if (player == null) return;

      // Obtener el ID de la tribu asociada al jugador
      UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
      if (tribeId == null) return;

      // Cancelar la tarea en la posición indicada y obtener los bloques afectados
      Set<BlockPos> cancelledBlocks = TribeManager.getTaskBlocksAt(tribeId, msg.pos);

      if (cancelledBlocks != null && !cancelledBlocks.isEmpty()) {
        // Notificar al cliente para que elimine los resaltados visuales (Highlights)
        // Usamos TribeHighlightPacket con el flag 'false' para remover
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TribeHighlightPacket(new HashSet<>(cancelledBlocks), false)
        );
      }
    });
    ctx.setPacketHandled(true);
  }
}