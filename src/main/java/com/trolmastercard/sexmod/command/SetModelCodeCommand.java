package com.trolmastercard.sexmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CustomizeNpcPacket;
// import com.trolmastercard.sexmod.util.ModUtil; // Descomenta si tienes ModUtil.prettify
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SetModelCodeCommand — Portado a 1.20.1.
 * * Comando de cliente: /setmodelcode [code[$specifics]]
 * * Aplica un código de modelo personalizado al NPC que estás mirando o a tu avatar.
 */
@OnlyIn(Dist.CLIENT)
public class SetModelCodeCommand {

    // ── Registro en Brigadier ────────────────────────────────────────────────

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setmodelcode")
                // Si el jugador no pone argumentos
                .executes(ctx -> execute(ctx, ""))

                // Si el jugador pone el código (greedyString absorbe todo hasta el final de la línea)
                .then(Commands.argument("code", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "code");
                            return execute(ctx, arg);
                        })
                )
        );
    }

    // ── Ejecución ────────────────────────────────────────────────────────────

    private static int execute(CommandContext<CommandSourceStack> ctx, String rawArg) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;

        if (localPlayer == null) return 0;

        // Si el código está vacío, avisamos al jugador
        if (rawArg.isEmpty()) {
            localPlayer.displayClientMessage(Component.literal("§cPor favor, introduce un código de modelo válido."), false);
            return 0;
        }

        // Separar el código base de los datos específicos (si los hay usando '$')
        String[] parts = rawArg.split("\\$");
        String code = parts.length > 0 ? parts[0] : "";
        String specific = parts.length > 1 ? parts[1] : "";

        // Resolver a quién le aplicamos el modelo (NPC mirado o avatar propio)
        BaseNpcEntity target = resolveTarget(mc.hitResult);

        if (target == null) {
            localPlayer.displayClientMessage(
                    Component.literal("§eYou gotta transform into the girl you want to apply the model-code to"),
                    true // true = muestra en la Actionbar (arriba del inventario)
            );
            return 0;
        }

        // Enviar el paquete al servidor para que todos vean el cambio
        if (specific.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new CustomizeNpcPacket(code, target.getNpcUUID()));
        } else {
            // Asumiendo que target.decodeSpecificData(specific) devuelve un String o NBT válido
            ModNetwork.CHANNEL.sendToServer(new CustomizeNpcPacket(code, target.getNpcUUID(), target.decodeSpecificData(specific)));
        }

        // Confirmación visual
        localPlayer.displayClientMessage(Component.literal(buildConfirmMessage(target)), true);

        return 1; // 1 = Éxito
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String buildConfirmMessage(BaseNpcEntity npc) {
        if (npc instanceof PlayerKoboldEntity) {
            // Reemplaza "ModUtil.prettify" con tu lógica de formateo si es necesario
            String typeName = npc.getType().toShortString(); // Método nativo de 1.20.1 para obtener el nombre
            return "§aApplied model code to your player-" + typeName;
        }

        // En 1.20.1 usamos getName().getString() o getCustomNameOverride() si lo definiste
        String npcName = npc.hasCustomName() ? npc.getCustomName().getString() : npc.getName().getString();
        return "§aApplied model code to this " + npcName;
    }

    /** Busca el NPC al que el jugador está mirando, o el avatar del jugador como plan B. */
    @OnlyIn(Dist.CLIENT)
    private static BaseNpcEntity resolveTarget(HitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();

        // Si estamos mirando directamente a una entidad
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hitResult;
            if (ehr.getEntity() instanceof BaseNpcEntity npc) {
                return npc;
            }
        }

        // Plan B: Si no miramos a nadie, verificamos si nuestro propio avatar es modificable
        return PlayerKoboldEntity.getForPlayer(mc.player);
    }
}