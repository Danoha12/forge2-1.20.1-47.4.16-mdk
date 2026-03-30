package com.trolmastercard.sexmod.command; // Te sugiero agrupar los comandos en su propio paquete

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncCustomModelsPacket;
import com.trolmastercard.sexmod.registry.ModelWhitelist; // Asegúrate de que esta clase exista
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.network.PacketDistributor;

/**
 * ReloadCustomModelsCommand — Portado a 1.20.1.
 * * Comando de servidor: /reloadcustommodels
 * * Nivel de permiso: 2 (Operador)
 * * Recarga la lista de modelos permitidos y la transmite a todos los jugadores.
 */
public class ReloadCustomModelsCommand {

  private ReloadCustomModelsCommand() {}

  /**
   * Registra el comando literal en el dispatcher.
   * Llámalo desde tu evento RegisterCommandsEvent.
   */
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
            Commands.literal("reloadcustommodels")
                    .requires(src -> src.hasPermission(2))
                    .executes(ReloadCustomModelsCommand::execute)
    );
  }

  // ── Ejecución ────────────────────────────────────────────────────────────

  private static int execute(CommandContext<CommandSourceStack> ctx) {
    // Recarga la lista blanca desde el disco
    ModelWhitelist.reload(false);

    // 🚨 1.20.1: Usamos PacketDistributor.ALL para enviar a todos a la vez.
    // Cero bucles, cero server.execute() innecesarios. ¡Eficiencia pura!
    SyncCustomModelsPacket packet = new SyncCustomModelsPacket(ModelWhitelist.getSerializedData());
    ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);

    // En Brigadier, devolver 1 significa éxito, 0 significa fallo.
    return 1;
  }
}