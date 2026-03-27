package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.tribe.TribeTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.*;

/**
 * DevToolsHandler - ported from ad.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT-only debug/developer tools. Active only in a de-obfuscated
 * (development) environment, i.e. when {@code FMLLoader.isProduction()} is
 * false.
 *
 * Chat commands intercepted (all case-insensitive):
 *  {@code resetcolor}          - resets all colour-cycle objects
 *  {@code set <index> <float>} - sets dev float array entry
 *  {@code get <index>}         - prints dev float array entry
 *  {@code time}                - prints current world game time
 *  {@code girls}               - lists all NPC entities in the world
 *  {@code kobs}                - lists all tribe kobolds + saved positions
 *  {@code setcumtime <long>}   - manually sets the cum-dosage timer
 *
 * Also registers a {@link LivingHurtEvent} handler that prints tribe debug
 * info when a {@link KoboldEntity} is hit.
 *
 * Register on the FORGE event bus:
 * <pre>
 *   MinecraftForge.EVENT_BUS.register(new DevToolsHandler());
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class DevToolsHandler {

    /** Number of dev float slots. */
    private static final int DEV_FLOAT_COUNT = 60;

    /** Global dev float array. Null if not running in dev environment. */
    public static float[] devFloats;

    public DevToolsHandler() {
        if (isDevEnv()) devFloats = new float[DEV_FLOAT_COUNT];
    }

    // =========================================================================
    //  Environment check
    // =========================================================================

    /** Returns true when running in the development (de-obfuscated) environment. */
    public static boolean isDevEnv() {
        return !FMLLoader.isProduction();
    }

    // =========================================================================
    //  Chat commands
    // =========================================================================

    /** Handles {@code resetcolor} command. */
    @SubscribeEvent
    public void onChatResetColor(ClientChatEvent event) {
        if (!isDevEnv()) return;
        if (!"resetcolor".equalsIgnoreCase(event.getMessage())) return;

        // Reset all registered colour-cycle objects (dj/de/dy/dg in original)
        // TODO: call reset on your ColourCycle objects here
        // ColourCycleA.reset();
        // ColourCycleB.reset();
    }

    /** Handles {@code set <index> <float>} command. */
    @SubscribeEvent
    public void onChatSet(ClientChatEvent event) {
        if (!isDevEnv()) return;
        String[] parts = event.getOriginalMessage().split(" ");
        if (parts.length != 3 || !"set".equalsIgnoreCase(parts[0])) return;

        try {
            int   index = Integer.parseInt(parts[1]);
            float value = Float.parseFloat(parts[2]);
            if (index > devFloats.length - 1) return;

            chat(String.format("7Set dev float N.%d from %s to %s",
                index, devFloats[index], value));
            devFloats[index] = value;
            event.setCanceled(true);
        } catch (Exception ignored) {}
    }

    /** Handles {@code get <index>} command. */
    @SubscribeEvent
    public void onChatGet(ClientChatEvent event) {
        if (!isDevEnv()) return;
        String[] parts = event.getOriginalMessage().split(" ");
        if (parts.length != 2 || !"get".equalsIgnoreCase(parts[0])) return;

        try {
            int index = Integer.parseInt(parts[1]);
            if (index > devFloats.length - 1) return;
            chat(String.format("edev float N.%d is %s", index, devFloats[index]));
            event.setCanceled(true);
        } catch (Exception ignored) {}
    }

    /** Handles {@code time}, {@code girls}, {@code kobs}, {@code setcumtime} commands. */
    @SubscribeEvent
    public void onChatMisc(ClientChatEvent event) {
        if (!isDevEnv()) return;

        String msg = event.getOriginalMessage().toLowerCase();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if ("time".equals(msg)) {
            chat(String.valueOf(player.level().getGameTime()));
            return;
        }

        if ("girls".equals(msg)) {
            // List all NPC entities
            var npcs = player.level().getEntitiesOfClass(
                com.trolmastercard.sexmod.entity.BaseNpcEntity.class, getHugeAABB(), e -> true);
            chat(String.valueOf(npcs.size()));
            for (var npc : npcs) {
                System.out.printf("%s at %s %s %s%n",
                    npc, npc.getX(), npc.getY(), npc.getZ());
            }
            return;
        }

        if ("kobs".equals(msg)) {
            UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
            int total = TribeManager.getMemberCount(tribeId);
            List<KoboldEntity> members = TribeManager.getMembersLoaded(tribeId);

            for (KoboldEntity kob : members) {
                chat(String.format("alive member %s at %s world.isremote? %s isdead %s girlID %s entityID %s",
                    kob.getKoboldName(), kob.blockPosition(),
                    kob.level().isClientSide(), kob.isRemoved(),
                    kob.getKoboldUUID(), kob.getId()));

                AABB bb = new AABB(kob.blockPosition());
                chat(player.level().getEntitiesOfClass(KoboldEntity.class, bb).isEmpty()
                    ? "couldn't be located" : "appears to actually exist");
            }

            Map<UUID, BlockPos> saved = TribeManager.getSavedPositions(tribeId, player.level());
            for (Map.Entry<UUID, BlockPos> entry : saved.entrySet()) {
                chat(String.format("saved pos of %s at %s",
                    entry.getKey(), entry.getValue()));
            }
            chat("total amount members: " + total);
            return;
        }

        if (msg.startsWith("setcumtime ")) {
            String[] parts = msg.split(" ");
            try {
                long time = Long.parseLong(parts[1]);
                GalathOwnershipData.setLastDosageTime(player.getUUID(), time);
                chat("set to: " + time);
            } catch (Exception e) {
                System.out.println("long: " + parts[1]);
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    //  Kobold hurt debug
    // =========================================================================

    /** When a KoboldEntity is hurt, print its tribe info to chat. */
    @SubscribeEvent
    public void onKoboldHurt(LivingHurtEvent event) {
        if (!isDevEnv()) return;
        if (!(event.getEntity() instanceof KoboldEntity kob)) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
        Collection<TribeTask> tasks = TribeManager.getActiveTasks(tribeId);

        for (TribeTask task : tasks) {
            chat("task: " + task.getKind().name());
            chat("workers involved: ");
            for (KoboldEntity worker : task.getWorkers()) {
                chat(worker.getKoboldName() + " " + worker.getKoboldUUID());
            }
        }

        boolean inLoadedList = TribeManager.getMembersLoaded(tribeId).contains(kob);
        boolean inSavedMap   = TribeManager.getSavedPositions(tribeId, player.level())
            .containsKey(kob.getKoboldUUID());

        chat("tribe contains my exact reference: " + inLoadedList);
        chat("loaded : " + inLoadedList);
        chat("saved  : " + inSavedMap);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    @OnlyIn(Dist.CLIENT)
    private void chat(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }

    private static AABB getHugeAABB() {
        return new AABB(-30_000_000, -256, -30_000_000, 30_000_000, 512, 30_000_000);
    }
}
