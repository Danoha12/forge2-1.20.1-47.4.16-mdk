package com.trolmastercard.sexmod.command;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side command {@code /setmodelcode [code[$specifics]]}.
 *
 * Applies a model code string to the looked-at NPC, or to the player's own
 * PlayerKoboldEntity if no NPC is targeted.
 *
 * Obfuscated name: fx
 */
@OnlyIn(Dist.CLIENT)
public class SetModelCodeCommand {

    public static final SetModelCodeCommand INSTANCE = new SetModelCodeCommand();

    /** Register via ClientCommandRegistrationEvent. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setmodelcode")
                .executes(ctx -> execute(ctx, ""))
                .then(Commands.argument("code", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "code");
                            return execute(ctx, arg);
                        })));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String rawArg) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) return 0;

        // Split code$specifics
        String[] parts  = rawArg.split("\\$");
        String   code   = parts.length > 0 ? parts[0] : "";
        String   specific = parts.length > 1 ? parts[1] : "";

        // Resolve target NPC
        BaseNpcEntity target = resolveTarget(mc.hitResult);
        if (target == null) {
            localPlayer.displayClientMessage(
                    Component.literal("You gotta transform into the girl you want to apply the model-code to"),
                    true);
            return 0;
        }

        if (specific.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new CustomizeNpcPacket(code, target.getNpcUUID()));
        } else {
            ModNetwork.CHANNEL.sendToServer(
                    new CustomizeNpcPacket(code, target.getNpcUUID(),
                            target.decodeSpecificData(specific)));
        }

        localPlayer.displayClientMessage(
                Component.literal(buildConfirmMessage(target)), true);
        return 1;
    }

    private static String buildConfirmMessage(BaseNpcEntity npc) {
        if (npc instanceof PlayerKoboldEntity) {
            NpcType type = NpcType.fromEntity(npc);
            String typeName = type != null ? ModUtil.prettify(type.toString()) : "?";
            return "eapplied model code to your player-" + typeName;
        }
        return "eapplied model code to this " + npc.getNpcName();
    }

    /** Returns the looked-at NPC, or the local player's PlayerKoboldEntity as fallback. */
    @OnlyIn(Dist.CLIENT)
    private static BaseNpcEntity resolveTarget(HitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();

        if (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof BaseNpcEntity npc) {
            return npc;
        }
        // Fallback: look for the player's own PlayerKoboldEntity
        return PlayerKoboldEntity.getForPlayer(mc.player);
    }
}
