package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NpcSpawnCommand {

    // 📋 Lista de nombres de tus NPCs para el autocompletado
    private static final List<String> NPC_TYPES = Arrays.asList(
            "jenny", "ellie", "bia", "luna", "slime", "bee", "allie", "kobold"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnnpc")
                .requires(source -> source.hasPermission(2)) // Solo para OPs (nivel 2)
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(NpcSpawnCommand::suggestNpcs) // Activa el autocompletado con TAB
                        .executes(context -> spawnNpc(context, context.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> spawnNpc(context, Vec3Argument.getVec3(context, "pos")))))
        );
    }

    // 💡 Lógica de sugerencias para el TAB
    private static CompletableFuture<Suggestions> suggestNpcs(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(NPC_TYPES, builder);
    }

    // 🚀 Lógica de invocación
    private static int spawnNpc(CommandContext<CommandSourceStack> context, Vec3 pos) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        String typeName = StringArgumentType.getString(context, "type");

        // Buscamos el tipo de entidad en el registro de tu mod
        EntityType<?> type = level.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .get(new ResourceLocation("sexmod", typeName));

        if (type != null) {
            Entity entity = type.create(level);
            if (entity instanceof BaseNpcEntity npc) {
                npc.moveTo(pos.x, pos.y, pos.z, 0, 0);
                level.addFreshEntity(npc);

                source.sendSuccess(() -> Component.literal("§d[SexMod] §fInvocada §b" + typeName + "§f en su posición."), true);
                return 1;
            }
        }

        source.sendFailure(Component.literal("§c[SexMod] Error: No se encontró el NPC '" + typeName + "'."));
        return 0;
    }
}