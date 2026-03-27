package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side command: /whitelistserver [confirm]
 *
 * Whitelists the current server so it is allowed to send custom models.
 * Obfuscated name: fd
 */
@OnlyIn(Dist.CLIENT)
public class WhitelistServerCommand {

    public static final WhitelistServerCommand INSTANCE = new WhitelistServerCommand();

    /** Register via ClientCommandRegistrationEvent. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whitelistserver")
                .executes(ctx -> execute(ctx, false))
                .then(Commands.argument("confirm", StringArgumentType.word())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "confirm");
                            return execute(ctx, "confirm".equals(arg));
                        })));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean confirmed) {
        CommandSourceStack source = ctx.getSource();

        String serverAddr = CustomModelManager.getCurrentServerAddress();
        if (serverAddr == null) {
            source.sendFailure(Component.literal("eThis is a multiplayer feature only"));
            return 0;
        }

        if (CustomModelManager.isServerWhitelisted(serverAddr)) {
            source.sendSuccess(() -> Component.literal("aServer is already whitelisted :)"), false);
            return 1;
        }

        if (!confirmed) {
            source.sendSuccess(() -> Component.literal(
                    "eBy whitelisting this server, you allow the server to send you the custom models that are used on it"),
                    false);
            source.sendSuccess(() -> Component.literal(
                    "cONLY WHITELIST SERVERS, WHOSE SERVER OWNER YOU KNOW AND TRUST"),
                    false);
            source.sendSuccess(() -> Component.literal("eto confirm your decision type:"), false);
            source.sendSuccess(() -> Component.literal("a/whitelistserver confirm"), false);
            return 0;
        }

        // Confirmed - add to whitelist and reload
        CustomModelManager.whitelistServer(serverAddr);
        source.sendSuccess(() -> Component.literal("aconfirmed :)"), false);
        CustomModelManager.reloadCustomModels();
        return 1;
    }
}
