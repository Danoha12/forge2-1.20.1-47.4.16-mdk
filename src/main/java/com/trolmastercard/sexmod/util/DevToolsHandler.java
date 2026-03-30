package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.tribe.TribeTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.*;

/**
 * DevToolsHandler — Portado a 1.20.1.
 * * Solo activo en entorno de desarrollo (!isProduction).
 * * Permite manipular los "devFloats" desde el chat para pruebas rápidas.
 */
@OnlyIn(Dist.CLIENT)
public class DevToolsHandler {

    private static final int DEV_FLOAT_COUNT = 60;
    public static float[] devFloats;

    public DevToolsHandler() {
        if (isDevEnv()) {
            devFloats = new float[DEV_FLOAT_COUNT];
        }
    }

    public static boolean isDevEnv() {
        return !FMLLoader.isProduction();
    }

    // ── Comandos de Chat (Interceptación) ────────────────────────────────────

    @SubscribeEvent
    public void onChatCommand(ClientChatEvent event) {
        if (!isDevEnv()) return;

        String rawMsg = event.getOriginalMessage();
        String msg = rawMsg.toLowerCase();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Comando: set <index> <valor>
        if (msg.startsWith("set ")) {
            handleSetCommand(event, rawMsg);
        }
        // Comando: get <index>
        else if (msg.startsWith("get ")) {
            handleGetCommand(event, rawMsg);
        }
        // Comandos simples
        else {
            switch (msg) {
                case "time" -> {
                    chat("Game Time: " + player.level().getGameTime());
                    event.setCanceled(true);
                }
                case "girls" -> {
                    listNpcs(player);
                    event.setCanceled(true);
                }
                case "kobs" -> {
                    debugTribes(player);
                    event.setCanceled(true);
                }
            }
        }
    }

    private void handleSetCommand(ClientChatEvent event, String msg) {
        String[] parts = msg.split(" ");
        if (parts.length != 3) return;
        try {
            int index = Integer.parseInt(parts[1]);
            float value = Float.parseFloat(parts[2]);
            if (index >= 0 && index < DEV_FLOAT_COUNT) {
                chat(String.format("§7[Dev] Float %d: %.2f -> %.2f", index, devFloats[index], value));
                devFloats[index] = value;
                event.setCanceled(true);
            }
        } catch (Exception ignored) {}
    }

    private void handleGetCommand(ClientChatEvent event, String msg) {
        String[] parts = msg.split(" ");
        if (parts.length != 2) return;
        try {
            int index = Integer.parseInt(parts[1]);
            if (index >= 0 && index < DEV_FLOAT_COUNT) {
                chat(String.format("§e[Dev] Float %d es: %.2f", index, devFloats[index]));
                event.setCanceled(true);
            }
        } catch (Exception ignored) {}
    }

    // ── Lógica de Debug de Entidades ─────────────────────────────────────────

    private void listNpcs(LocalPlayer player) {
        // En 1.20.1 el límite de altura es diferente, usamos un AABB amplio
        AABB area = player.getBoundingBox().inflate(128);
        List<BaseNpcEntity> npcs = player.level().getEntitiesOfClass(BaseNpcEntity.class, area);

        chat("§bNPCs cercanos: " + npcs.size());
        for (BaseNpcEntity npc : npcs) {
            System.out.println("NPC: " + npc.getNpcName() + " en " + npc.blockPosition());
        }
    }

    private void debugTribes(LocalPlayer player) {
        UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
        List<KoboldEntity> members = TribeManager.getMembersLoaded(tribeId);

        chat("§6--- Info de Tribu ---");
        chat("Miembros cargados: " + members.size());

        for (KoboldEntity kob : members) {
            chat(String.format("§a%s §7(ID: %d) - %s",
                    kob.getKoboldName(), kob.getId(), kob.blockPosition().toShortString()));
        }
    }

    // ── Debug de Daño (Kobolds) ──────────────────────────────────────────────

    @SubscribeEvent
    public void onKoboldHurt(LivingHurtEvent event) {
        if (!isDevEnv()) return;
        if (!(event.getEntity() instanceof KoboldEntity kob)) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
        Collection<TribeTask> tasks = TribeManager.getActiveTasks(tribeId);

        chat("§cKobold herido: " + kob.getKoboldName());
        for (TribeTask task : tasks) {
            if (task.getWorkers().contains(kob)) {
                chat("§7Trabajando en: " + task.getKind().name());
            }
        }
    }

    // ── Helper de Chat ───────────────────────────────────────────────────────

    private void chat(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}