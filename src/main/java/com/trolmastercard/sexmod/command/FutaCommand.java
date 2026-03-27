package com.trolmastercard.sexmod.command;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.util.LightUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.Random;

/**
 * FutaCommand - ported from a_.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Client-side command: {@code /futa <true|false>}
 *
 * Toggles the "futa" mode for galath/NPC entities. The setting is persisted
 * to disk at {@code sexmod/futa}.
 *
 * When set to true, spawns DRAGON_BREATH particles at the "cockParticles" bone
 * of every active client-side galath NPC.
 *
 * In 1.12.2 this was an {@code IClientCommand} registered via the client proxy.
 * In 1.20.1 use Brigadier and register on {@code RegisterCommandsEvent}.
 */
public class FutaCommand {

    public static final String FILE_PATH = "sexmod/futa";

    private static final int   PARTICLE_COUNT = 10;
    private static final float PARTICLE_SPEED = 0.025F;

    /** Current futa-mode state - persisted to {@code sexmod/futa}. */
    public static boolean enabled = true;

    static {
        // Read persisted value on class load
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line = reader.readLine();
            if (line != null) {
                line = line.toLowerCase().trim();
                if ("true".equals(line))  enabled = true;
                if ("false".equals(line)) enabled = false;
            }
        } catch (Exception ignored) {}
    }

    private FutaCommand() {}

    // =========================================================================
    //  Registration
    // =========================================================================

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("futa")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(FutaCommand::execute))
                .executes(FutaCommand::printUsage)
        );
    }

    // =========================================================================
    //  Execution
    // =========================================================================

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        boolean value = BoolArgumentType.getBool(ctx, "value");
        enabled = value;

        // Persist to disk
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(String.valueOf(value));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Spawn particles on all client-side galath NPCs
        spawnCockParticles(ctx.getSource());

        return 1;
    }

    private static int printUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
            () -> Component.literal("eYou can either do 7/futa true eor 7/futa false"),
            false);
        return 0;
    }

    // =========================================================================
    //  Particle effect
    // =========================================================================

    private static void spawnCockParticles(CommandSourceStack source) {
        try {
            var level = source.getLevel();
            Random random = new Random();

            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (npc.isRemoved()) continue;
                if (!npc.level().isClientSide()) continue;
                if (!npc.isGalathType()) continue;

                Vec3 particlePos = npc.getBonePosition("cockParticles")
                    .add(npc.position());

                int sign = LightUtil.getLightSign();
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    level.addParticle(
                        ParticleTypes.DRAGON_BREATH,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        random.nextFloat() * PARTICLE_SPEED * sign,
                        random.nextFloat() * PARTICLE_SPEED * sign,
                        random.nextFloat() * PARTICLE_SPEED * sign);
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }
}
