package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * RemoveItemsPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Remueve una cantidad específica de un ítem del inventario del jugador.
 * * Usado para pagar el "costo" de las acciones especiales en la GUI.
 */
public class RemoveItemsPacket {

  private final ItemStack stackToRemove;

  public RemoveItemsPacket(ItemStack stackToRemove) {
    this.stackToRemove = stackToRemove;
  }

  // ── Codec ────────────────────────────────────────────────────────────────

  public static void encode(RemoveItemsPacket msg, FriendlyByteBuf buf) {
    // Ya no enviamos el UUID del jugador, el servidor ya sabe quién es
    buf.writeItem(msg.stackToRemove);
  }

  public static RemoveItemsPacket decode(FriendlyByteBuf buf) {
    return new RemoveItemsPacket(buf.readItem());
  }

  // ── Manejador (Handler) ──────────────────────────────────────────────────

  public static void handle(RemoveItemsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
    NetworkEvent.Context ctx = ctxSupplier.get();

    // Solo procesamos en el lado del Servidor
    if (!ctx.getDirection().getReceptionSide().isServer()) return;

    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getSender();
      if (player == null) return;

      int amountLeft = msg.stackToRemove.getCount();

      // Buscamos en todo el inventario para cobrar el total
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
        if (amountLeft <= 0) break;

        ItemStack slotStack = player.getInventory().getItem(i);

        // Verificamos si es el mismo ítem (ignorando NBT si es necesario)
        if (slotStack.is(msg.stackToRemove.getItem())) {
          int toShrink = Math.min(slotStack.getCount(), amountLeft);

          slotStack.shrink(toShrink);
          amountLeft -= toShrink;

          // Notificar al inventario que hubo un cambio
          player.getInventory().setItem(i, slotStack);
        }
      }

      // Forzar actualización de los slots para el cliente
      player.containerMenu.broadcastChanges();
    });

    ctx.setPacketHandled(true);
  }
}