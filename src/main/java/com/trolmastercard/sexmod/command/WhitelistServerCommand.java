package com.trolmastercard.sexmod.command; // Ajusta a tu paquete de comandos

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.client.CustomModelManager; // Ajusta la ruta a tu clase real
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * WhitelistServerCommand — Portado a 1.20.1.
 * * Comando exclusivo del cliente: /whitelistserver [confirm]
 * * Permite que el servidor actual envíe modelos personalizados.
 */
@OnlyIn(Dist.CLIENT)
public class WhitelistServerCommand {

    // ── Registro ─────────────────────────────────────────────────────────────

    /** * 🚨 1.20.1: Asegúrate de llamar a esto desde el evento RegisterClientCommandsEvent
     * de Forge, NO desde el registro de comandos del servidor.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whitelistserver")
                .executes(ctx -> execute(ctx, false))
                .then(Commands.argument("confirm", StringArgumentType.word())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "confirm");
                            return execute(ctx, "confirm".equals(arg));
                        })
                )
        );
    }

    // ── Ejecución ────────────────────────────────────────────────────────────

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean confirmed) {
        CommandSourceStack source = ctx.getSource();

        String serverAddr = CustomModelManager.getCurrentServerAddress();

        if (serverAddr == null) {
            // 1.20.1: Usamos ChatFormatting en lugar de §e
            source.sendFailure(Component.literal("This is a multiplayer feature only")
                    .withStyle(ChatFormatting.YELLOW));
            return 0; // 0 = Fallo / Sin cambios
        }

        if (CustomModelManager.isServerWhitelisted(serverAddr)) {
            source.sendSuccess(() -> Component.literal("Server is already whitelisted :)")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1; // 1 = Éxito
        }

        if (!confirmed) {
            source.sendSuccess(() -> Component.literal(
                            "By whitelisting this server, you allow the server to send you the custom models that are used on it")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal(
                            "ONLY WHITELIST SERVERS, WHOSE SERVER OWNER YOU KNOW AND TRUST")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            source.sendSuccess(() -> Component.literal("to confirm your decision type:")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("/whitelistserver confirm")
                    .withStyle(ChatFormatting.GREEN), false);
            return 0;
        }

        // Confirmado — añadir a la lista blanca y recargar
        CustomModelManager.whitelistServer(serverAddr);
        source.sendSuccess(() -> Component.literal("confirmed :)")
                .withStyle(ChatFormatting.GREEN), false);

        CustomModelManager.reloadCustomModels();
        return 1;
    }
}