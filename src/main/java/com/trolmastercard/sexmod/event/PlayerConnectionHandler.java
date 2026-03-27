package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.item.ModItems;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncOwnershipPacket;
import com.trolmastercard.sexmod.network.packet.IsGirlPacket;
import com.trolmastercard.sexmod.network.packet.TribeHighlightPacket;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.Predicate;

/**
 * PlayerConnectionHandler - ported from q.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Handles player login and logout:
 *  - Login:  syncs ownership data, re-spawns the player's NPC if needed,
 *            randomises wand item UUIDs, sends tribe block highlights.
 *  - Logout: resets all NPCs owned by / partnered with the player,
 *            notifies sex partners, removes stale NPCs.
 *
 * Register on the FORGE event bus in your mod constructor.
 */
public class PlayerConnectionHandler {

    // =========================================================================
    //  Hardcoded developer UUIDs  (b / a from original)
    //  These trigger special NPC spawns on login for the mod authors.
    // =========================================================================

    private static final UUID DEV_UUID_A =
        UUID.fromString("b91e6484-8911-4def-ab04-9fa3452fca5f");
    private static final UUID DEV_UUID_B =
        UUID.fromString("adf20149-2adc-4a9d-9af5-8e9aeda019d6");

    // =========================================================================
    //  Login
    // =========================================================================

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Reset any lingering god-mode / no-clip / spectator state
        serverPlayer.setGodMode(false);
        serverPlayer.setNoGravity(false);
        serverPlayer.setSpectator(false);
        if (!serverPlayer.getAbilities().instabuild) {
            serverPlayer.getAbilities().flying = false;
        }

        // Send ownership and "is-girl" sync packets
        ModNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> serverPlayer),
            new IsGirlPacket(true));
        ModNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> serverPlayer),
            new SyncOwnershipPacket(GalathOwnershipData.getOwnershipSnapshot(serverPlayer.getUUID())));

        // Randomise UUID on any Galath Wand items so they can't be duped
        for (ItemStack stack : serverPlayer.getInventory().items) {
            if (stack.is(ModItems.GALATH_WAND.get()) && stack.hasTag()) {
                Objects.requireNonNull(stack.getOrCreateTag())
                       .putUUID("user", UUID.randomUUID());
            }
        }

        // Send tribe block highlights for any tribe this player masters
        UUID tribeId = TribeManager.getTribeIdForMaster(serverPlayer.getUUID());
        if (tribeId != null) {
            Set<BlockPos2> blocks = TribeManager.getAllClaimedBlocksAsSet(tribeId);
            ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new TribeHighlightPacket(blocks, true));
        }

        // Rebuild the PlayerKoboldEntity index
        PlayerKoboldEntity.rebuildIndex();

        // Respawn the player's NPC if one exists
        PlayerKoboldEntity existingNpc =
            PlayerKoboldEntity.getForPlayer(serverPlayer.getUUID());
        Level world = ServerLifecycleHooks.getCurrentServer().overworld();
        removeStaleNpcs(world, serverPlayer, existingNpc);

        if (existingNpc != null) {
            existingNpc.setActive(false);
            existingNpc.setAnimState(AnimState.NULL);
            // Re-queue the NPC for its idle cycle
            KoboldIdleScheduler.schedule(existingNpc);
        }

        // Developer-only spawns
        UUID playerUUID = serverPlayer.getUUID();
        if (playerUUID.equals(DEV_UUID_A)) spawnDevNpcA(world, serverPlayer, playerUUID);
        if (playerUUID.equals(DEV_UUID_B)) spawnDevNpcB(world, serverPlayer, playerUUID);

        // Sync any active gift / currency state
        GiftMenuHelper.syncOnLogin(serverPlayer);
    }

    // =========================================================================
    //  Logout
    // =========================================================================

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        List<BaseNpcEntity> allNpcs = BaseNpcEntity.getAllActive();

        for (BaseNpcEntity npc : allNpcs) {
            // --- NPC owned by / partnered with this player ---
            boolean ownedByPlayer    = player.getUUID().equals(npc.getMasterUUID())
                                    || player.getUUID().equals(npc.getGameUUID());
            boolean isPlayerKobold   = npc instanceof PlayerKoboldEntity pk
                                    && player.getUUID().equals(pk.getPlayerUUID());

            if (ownedByPlayer || isPlayerKobold) {
                // Notify sex partner if applicable
                if (npc.getSexTarget() != null) {
                    ServerPlayer partner = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(npc.getSexTarget());
                    if (partner != null) {
                        ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> partner),
                            new IsGirlPacket(true));
                        resetPlayerState(partner);
                    }
                    player.level().getEntity(0); // no-op placeholder
                    resetPlayerState((ServerPlayer) player);
                }

                resetNpcState(npc);
                npc.setActive(false);
                npc.setAnimState(AnimState.NULL);
                npc.setSexTarget(null);
            }

            // --- PlayerKoboldEntity whose player is the one logging out ---
            if (npc instanceof PlayerKoboldEntity pk
                && player.getUUID().equals(pk.getPlayerUUID())
                && npc.getMasterUUID() != null) {

                ServerPlayer master = ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(npc.getMasterUUID());
                if (master != null) {
                    ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> master),
                        new IsGirlPacket(true));
                    resetPlayerState(master);
                }
                npc.setSexTarget(null);
            }
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Spawns Dev NPC type A for the first developer UUID. */
    private void spawnDevNpcA(Level world, ServerPlayer player, UUID uuid) {
        DevNpcA npc = new DevNpcA(world, uuid);
        npc.setNoGravity(true);
        npc.setInvulnerable(true);
        npc.setNoAi(false);
        npc.setDeltaMovement(0, 0, 0);
        npc.setPos(player.getX(), player.getY() + 69.0, player.getZ());
        world.addFreshEntity(npc);
        npc.init();
    }

    /** Spawns Dev NPC type B for the second developer UUID. */
    private void spawnDevNpcB(Level world, ServerPlayer player, UUID uuid) {
        DevNpcB npc = new DevNpcB(world, uuid);
        npc.setNoGravity(true);
        npc.setInvulnerable(true);
        npc.setNoAi(false);
        npc.setDeltaMovement(0, 0, 0);
        npc.setPos(player.getX(), player.getY() + 69.0, player.getZ());
        world.addFreshEntity(npc);
        npc.init();
    }

    /**
     * Removes any stale duplicate PlayerKoboldEntity instances for the given
     * player, keeping only the canonical {@code keepNpc}.
     */
    private void removeStaleNpcs(Level world, Player player,
                                  PlayerKoboldEntity keepNpc) {
        Predicate<PlayerKoboldEntity> isStale = npc ->
            npc.getPlayerUUID().equals(player.getUUID())
            && (keepNpc == null || npc.getId() != keepNpc.getId());

        List<PlayerKoboldEntity> stale = world.getEntitiesOfClass(
            PlayerKoboldEntity.class,
            net.minecraft.world.phys.AABB.ofSize(
                net.minecraft.world.phys.Vec3.ZERO, Double.MAX_VALUE,
                Double.MAX_VALUE, Double.MAX_VALUE),
            isStale::test);

        stale.forEach(npc -> world.removeEntity(npc.getId(),
            Entity.RemovalReason.DISCARDED));
    }

    /** Resets a player's game-mode flags after a sex sequence ends. */
    private static void resetPlayerState(ServerPlayer player) {
        player.setGodMode(false);
        player.setNoGravity(false);
        player.setSpectator(false);
        player.getAbilities().flying = false;
    }

    /** Resets an NPC's active-sex state. */
    private static void resetNpcState(BaseNpcEntity npc) {
        npc.setSexTarget(null);
        npc.setActive(false);
        npc.setAnimState(AnimState.NULL);
        npc.setNoGravity(false);
        npc.setInvulnerable(false);

        // Move NPC upward until it's not inside a solid block
        net.minecraft.world.phys.Vec3 pos = npc.position();
        Level level = npc.level();
        while (level.getBlockState(
                net.minecraft.core.BlockPos.containing(pos))
                .isSolidRender(level, net.minecraft.core.BlockPos.containing(pos))) {
            pos = pos.add(0, 1, 0);
        }
        npc.teleportTo(pos.x, pos.y, pos.z);
    }
}
