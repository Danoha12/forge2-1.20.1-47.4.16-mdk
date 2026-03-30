package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.Random;

/**
 * FutaCommand — Portado a 1.20.1.
 * * Comando exclusivo de cliente: /futa <true|false>
 * * Usa Brigadier y guarda la configuración de forma segura.
 */
public class FutaCommand {

    // Usamos el directorio raíz del juego de forma segura a través de FMLPaths
    private static final Path FOLDER_PATH = FMLPaths.GAMEDIR.get().resolve("sexmod");
    private static final Path FILE_PATH = FOLDER_PATH.resolve("futa.txt");

    private static final int PARTICLE_COUNT = 10;
    private static final float PARTICLE_SPEED = 0.025F;

    public static boolean enabled = true;

    // ── Carga Inicial ────────────────────────────────────────────────────────

    static {
        try {
            if (Files.exists(FILE_PATH)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH.toFile()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        enabled = Boolean.parseBoolean(line.trim().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SexMod] Error al cargar la config de FutaCommand: " + e.getMessage());
        }
    }

    private FutaCommand() {}

    // ── Registro en Brigadier ────────────────────────────────────────────────

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("futa")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(FutaCommand::execute))
                        .executes(FutaCommand::printUsage)
        );
    }

    // ── Ejecución ────────────────────────────────────────────────────────────

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        enabled = BoolArgumentType.getBool(ctx, "value");

        // Guardar a disco de forma segura creando la carpeta si no existe
        try {
            Files.createDirectories(FOLDER_PATH);
            try (FileWriter writer = new FileWriter(FILE_PATH.toFile())) {
                writer.write(String.valueOf(enabled));
            }
        } catch (IOException e) {
            System.err.println("[SexMod] No se pudo guardar el archivo futa.txt");
        }

        // Feedback al jugador
        ctx.getSource().sendSuccess(() ->
                Component.literal("§dModo Futa configurado a: §f" + enabled), false);

        if (enabled) {
            spawnCockParticles();
        }

        return 1; // 1 = Comando ejecutado con éxito
    }

    private static int printUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eUso correcto: §7/futa true §eo §7/futa false"), false);
        return 0; // 0 = Comando fallido (o solo mostró info)
    }

    // ── Efectos Visuales ─────────────────────────────────────────────────────

    private static void spawnCockParticles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return; // Seguridad extra

        try {
            Random random = new Random();

            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (npc.isRemoved() || !npc.level().isClientSide()) continue;

                // Asumimos que tienes el método isGalathType() en BaseNpcEntity
                if (!npc.isGalathType()) continue;

                // En GeckoLib 4, conseguir posiciones absolutas de huesos fuera del renderizador
                // es complicado. Asumimos que getBonePosition hace la magia matemática.
                Vec3 particlePos = npc.getBonePosition("cockParticles").add(npc.position());

                // Determinar la dirección de las partículas basándonos en hacia dónde mira el NPC
                float yaw = npc.getYRot() * ((float) Math.PI / 180F);
                double dirX = -Math.sin(yaw);
                double dirZ = Math.cos(yaw);

                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    mc.level.addParticle(
                            ParticleTypes.DRAGON_BREATH,
                            particlePos.x,
                            particlePos.y,
                            particlePos.z,
                            dirX * random.nextFloat() * PARTICLE_SPEED,
                            random.nextFloat() * PARTICLE_SPEED,
                            dirZ * random.nextFloat() * PARTICLE_SPEED
                    );
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }
}