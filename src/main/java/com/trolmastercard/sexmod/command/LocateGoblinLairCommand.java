package com.trolmastercard.sexmod.command; // Te sugiero meterlo en un paquete de comandos

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * LocateGoblinLairCommand — Portado a 1.20.1 (Brigadier).
 * * Encuentra la guarida Goblin (GoblinEntity jefe) más cercana cargada en memoria.
 */
public class LocateGoblinLairCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("locatenearestgoblinlair")
                        .executes(LocateGoblinLairCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel(); // Obtenemos el nivel correcto de forma segura

        // 1. Verificamos la dimensión directamente desde el nivel del comando
        if (level.dimension() != Level.OVERWORLD) {
            String dimName = (level.dimension() == Level.NETHER) ? "Nether" : "End";
            source.sendFailure(Component.literal("Goblin lairs don't exist in the " + dimName));
            return 0; // 0 significa que el comando falló
        }

        GoblinEntity nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        Vec3 sourcePos = source.getPosition();

        // 2. Iteramos SOLO por las entidades cargadas en memoria.
        // ¡Cero lag, no escanea chunks enteros ni físicas!
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof GoblinEntity goblin && goblin.isLairBoss()) {
                double distSqr = goblin.distanceToSqr(sourcePos);
                if (distSqr < nearestDistSqr) {
                    nearest = goblin;
                    nearestDistSqr = distSqr;
                }
            }
        }

        if (nearest == null) {
            // Nota: Si el chunk donde está la guarida no está cargado por ningún jugador, no la encontrará.
            source.sendFailure(Component.literal("No nearby loaded goblin lair found uwu"));
            return 0;
        }

        // 3. Éxito
        BlockPos pos = nearest.blockPosition();
        source.sendSuccess(() -> Component.literal(String.format(
                "Goblin lair found at %d %d %d", pos.getX(), pos.getY(), pos.getZ())), false);

        return 1; // 1 significa que el comando se ejecutó con éxito
    }
}