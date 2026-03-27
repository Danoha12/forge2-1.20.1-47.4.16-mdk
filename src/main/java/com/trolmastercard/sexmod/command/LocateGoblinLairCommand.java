package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * LocateGoblinLairCommand - /locatenearestgoblinlair
 * Finds the nearest goblin lair entity (GoblinEntity with aX==true) and reports its position.
 * Ported from gn.class (Fapcraft 1.12.2 v1.1) to 1.20.1 Brigadier.
 *
 * Original:
 *   CommandBase.func_71517_b()  - getName()
 *   ICommandSender.func_145747_a() - source.sendSuccess(Component, false)
 *   e3 - GoblinEntity (goblin lair/boss entity)
 *   e3.aX - goblin.isLairBoss (boolean marking this goblin as a lair boss)
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
        Entity executor = source.getEntity();

        // Check dimension
        if (executor != null && executor.level().dimension() != Level.OVERWORLD) {
            String dimName = (executor.level().dimension() == Level.NETHER) ? "Nether" : "End";
            source.sendFailure(Component.literal(
                    "goblin lairs don't exist in the " + dimName));
            return 0;
        }

        // Find nearest lair boss in server entity list
        GoblinEntity nearest = null;
        List<GoblinEntity> goblins = source.getServer()
                .getAllLevels()
                .iterator().next()
                .getEntitiesOfClass(GoblinEntity.class,
                        executor != null
                                ? executor.getBoundingBox().inflate(5000)
                                : net.minecraft.world.phys.AABB.ofSize(
                                net.minecraft.world.phys.Vec3.ZERO, 10000, 10000, 10000));

        for (GoblinEntity g : goblins) {
            if (!g.isLairBoss()) continue;
            if (nearest == null
                    || g.distanceToSqr(source.getPosition()) < nearest.distanceToSqr(source.getPosition())) {
                nearest = g;
            }
        }

        if (nearest == null) {
            source.sendFailure(Component.literal("No nearby goblin lair found uwu"));
            return 0;
        }

        BlockPos pos = nearest.blockPosition();
        source.sendSuccess(() -> Component.literal(String.format(
                "goblin lair found at %d %d %d", pos.getX(), pos.getY(), pos.getZ())), false);
        return 1;
    }
}
