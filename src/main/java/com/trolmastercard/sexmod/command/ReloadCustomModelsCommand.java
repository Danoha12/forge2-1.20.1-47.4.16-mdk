package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncCustomModelsPacket;
import com.trolmastercard.sexmod.registry.ModelWhitelist;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

/**
 * ReloadCustomModelsCommand - ported from aw.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Server command: {@code /reloadcustommodels}
 * Required permission level: 2 (operator)
 *
 * Reloads the custom-model whitelist from disk and broadcasts the updated
 * model list to every connected player.
 *
 * Register in your {@code RegisterCommandsEvent}:
 * <pre>
 *   ReloadCustomModelsCommand.register(event.getDispatcher());
 * </pre>
 */
public class ReloadCustomModelsCommand {

    private ReloadCustomModelsCommand() {}

    /**
     * Registers the {@code /reloadcustommodels} literal with the given dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("reloadcustommodels")
                .requires(src -> src.hasPermission(2))
                .executes(ReloadCustomModelsCommand::execute)
        );
    }

    // =========================================================================
    //  Execution
    // =========================================================================

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        // Reload the whitelist from disk (false = full reload, not incremental)
        ModelWhitelist.reload(false);

        // Broadcast the updated model data to every online player
        SyncCustomModelsPacket packet = new SyncCustomModelsPacket(ModelWhitelist.getSerializedData());
        for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            ctx.getSource().getServer().execute(() ->
                ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    packet)
            );
        }

        return 1; // success
    }
}
