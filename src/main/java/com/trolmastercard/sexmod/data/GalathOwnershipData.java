package com.trolmastercard.sexmod.data;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SyncOwnershipPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;

import java.util.*;

/**
 * GalathOwnershipData - ported from v.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * {@link SavedData} that tracks which player owns which NPC (galath), plus:
 *  - Last cum-dosage timestamp per player (for cooldown logic)
 *  - Set of "mango-owned" NPC UUIDs
 *
 * Keys used in world storage:
 *  - {@code "sexmod:galath_owner_ship"} - the SavedData record name
 *  - {@code "sexmod:ownershipdata"}     - NBT sub-compound for player-NPC map
 *  - {@code "sexmod:mangownershipdata"} - NBT sub-compound for mango-owners
 *
 * In 1.20.1 the data is attached to the overworld's DataStorage; retrieve via:
 * <pre>
 *   level.getServer().overworld().getDataStorage()
 *        .computeIfAbsent(GalathOwnershipData::load,
 *                         GalathOwnershipData::new, KEY);
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GalathOwnershipData extends SavedData {

    // =========================================================================
    //  Storage keys
    // =========================================================================

    public static final String KEY              = "sexmod:galath_owner_ship";
    public static final String KEY_OWNERSHIP    = "sexmod:ownershipdata";
    public static final String KEY_MANGO        = "sexmod:mangownershipdata";

    // =========================================================================
    //  In-memory state
    // =========================================================================

    /** Cooldown duration in game ticks (0 = no cooldown enforced here). */
    public static final long COOLDOWN_TICKS = 0L;

    /** Whether this is currently enabled globally. */
    public static boolean globallyEnabled = true;

    /**
     * Bidirectional map: playerUUID - npcUUID.
     * Left key = player UUID, Right value = NPC UUID.
     */
    private static final BiMap<UUID, UUID> ownershipMap = new BiMap<>();

    /** Last cum-dosage world-time per player UUID. */
    private static final HashMap<UUID, Long> lastDosageTime = new HashMap<>();

    /** Set of NPC UUIDs that are "mango-owned" (special state flag). */
    private static final HashSet<UUID> mangoOwned = new HashSet<>();

    // =========================================================================
    //  Constructors
    // =========================================================================

    public GalathOwnershipData() {}

    // =========================================================================
    //  Static API - ownership
    // =========================================================================

    public static void clearAll() {
        mangoOwned.clear();
        ownershipMap.clear();
    }

    /** Marks {@code npcUUID} as mango-owned. */
    public static void markMangoOwned(UUID npcUUID) {
        UUID ownerUUID = ownershipMap.getByRight(npcUUID);
        if (ownerUUID == null) return;
        mangoOwned.add(ownerUUID);
    }

    public static boolean isMangoOwned(UUID playerUUID) {
        return mangoOwned.contains(playerUUID);
    }

    /**
     * Returns true if the NPC's owner is online, in the same dimension,
     * and within 60 blocks.
     */
    public static boolean isOwnerNearby(BaseNpcEntity npc) {
        UUID ownerUUID = ownershipMap.getByRight(npc.getUUID());
        if (ownerUUID == null) return false;
        Level world = npc.level();
        Player player = world.getPlayerByUUID(ownerUUID);
        if (player == null) return true; // owner exists but not loaded  assume nearby
        if (player.level() != world) return false;
        return player.distanceTo(npc) <= 60.0F;
    }

    public static boolean isOwner(Player player, BaseNpcEntity npc) {
        return npc.getUUID().equals(ownershipMap.getByKey(player.getUUID()));
    }

    /** Removes any entry associated with {@code npc} and notifies its owner. */
    public static void removeOwnership(BaseNpcEntity npc) {
        UUID ownerUUID = ownershipMap.getByRight(npc.getUUID());
        npc.level().removeEntity(npc, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        if (ownerUUID == null) return;
        ownershipMap.removeByKey(ownerUUID);
        Player owner = ServerLifecycleHooks.getCurrentServer()
            .overworld().getPlayerByUUID(ownerUUID);
        if (owner instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> sp),
                new SyncOwnershipPacket(false));
        }
    }

    public static boolean hasNpc(UUID playerUUID) {
        return ownershipMap.getByKey(playerUUID) != null;
    }

    /** Returns the NPC UUID owned by {@code playerUUID}, or null. */
    @org.jetbrains.annotations.Nullable
    public static UUID getNpcForPlayer(UUID playerUUID) {
        return ownershipMap.getByRight(playerUUID);
    }

    /** Returns the NPC UUID owned by {@code npcUUID}'s righthand key, or null. */
    @org.jetbrains.annotations.Nullable
    public static UUID getOwnerForNpc(UUID npcUUID) {
        return ownershipMap.getByKey(npcUUID);
    }

    @org.jetbrains.annotations.Nullable
    public static UUID getNpcForPlayer(Player player) {
        if (player == null) return null;
        return getOwnerForNpc(player.getUUID());
    }

    public static void setOwnership(UUID playerUUID, UUID npcUUID) {
        ownershipMap.put(playerUUID, npcUUID);
    }

    public static void setOwnership(Player player, BaseNpcEntity npc) {
        if (player == null || npc == null) return;
        setOwnership(player.getUUID(), npc.getUUID());
    }

    public static void removeOwnership(UUID playerUUID) {
        ownershipMap.removeByKey(playerUUID);
    }

    public static void removeOwnership(Player player) {
        if (player == null) return;
        removeOwnership(player.getUUID());
    }

    // =========================================================================
    //  Cooldown
    // =========================================================================

    public static boolean isCooldownActive(UUID playerUUID, net.minecraft.world.level.Level level) {
        if (!isMangoOwned(playerUUID)) return false;
        Long time = lastDosageTime.get(playerUUID);
        if (time == null) return true;
        return (level.getGameTime() - time) > COOLDOWN_TICKS;
    }

    public static void setLastDosageTime(UUID playerUUID, Long worldTime) {
        if (playerUUID == null) {
            Main.LOGGER.warn("tried to save last cum dosage time on NULL player");
            return;
        }
        lastDosageTime.put(playerUUID, worldTime);
    }

    // =========================================================================
    //  Snapshot helper for SyncOwnershipPacket
    // =========================================================================

    public static boolean getOwnershipSnapshot(UUID playerUUID) {
        return hasNpc(playerUUID);
    }

    // =========================================================================
    //  Forge event handlers
    // =========================================================================

    /** Tick handler - cleans up ownership entries for NPCs that no longer exist. */
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Level world = server.overworld();
        List<UUID> stale = new ArrayList<>();

        for (Map.Entry<UUID, UUID> entry : ownershipMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            UUID npcUUID    = entry.getValue();
            Player player = world.getPlayerByUUID(playerUUID);
            if (player == null) continue;
            if (BaseNpcEntity.getById(npcUUID) == null) stale.add(playerUUID);
        }

        for (UUID playerUUID : stale) {
            ownershipMap.removeByKey(playerUUID);
            ServerPlayer sp = server.getPlayerList().getPlayer(playerUUID);
            if (sp != null) {
                ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncOwnershipPacket(false));
            }
        }
    }

    // =========================================================================
    //  SavedData serialisation
    // =========================================================================

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag ownership = new CompoundTag();
        ownership.putInt("amount", ownershipMap.size());
        int i = 0;
        for (Map.Entry<UUID, UUID> entry : ownershipMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            UUID npcUUID    = entry.getValue();
            Long dosage = lastDosageTime.getOrDefault(playerUUID, 0L);

            ownership.putUUID("galath"         + i, npcUUID);
            ownership.putUUID("master"         + i, playerUUID);
            ownership.putLong("lastcumdosage"  + i, dosage);
            i++;
        }

        CompoundTag mango = new CompoundTag();
        int j = 0;
        for (UUID uuid : mangoOwned) {
            mango.putUUID("mang" + j++, uuid);
        }

        tag.put(KEY_OWNERSHIP, ownership);
        tag.put(KEY_MANGO,     mango);
        return tag;
    }

    public static GalathOwnershipData load(CompoundTag tag) {
        GalathOwnershipData data = new GalathOwnershipData();

        CompoundTag ownership = tag.getCompound(KEY_OWNERSHIP);
        int amount = ownership.getInt("amount");
        for (int i = 0; i < amount; i++) {
            if (!ownership.hasUUID("master" + i) || !ownership.hasUUID("galath" + i)) {
                Main.LOGGER.fatal("OMFG WHOOP WHOOP SAVING DIDNT WORK CORRECTLY AAAAAAAAAAA");
                continue;
            }
            UUID playerUUID = ownership.getUUID("master" + i);
            UUID npcUUID    = ownership.getUUID("galath" + i);
            long dosage     = ownership.getLong("lastcumdosage" + i);
            ownershipMap.put(playerUUID, npcUUID);
            lastDosageTime.put(playerUUID, dosage);
        }

        CompoundTag mango = tag.getCompound(KEY_MANGO);
        for (int j = 0; mango.hasUUID("mang" + j); j++) {
            mangoOwned.add(mango.getUUID("mang" + j));
        }

        // Clear stale sub-tags
        tag.put(KEY_MANGO,     new CompoundTag());
        tag.put(KEY_OWNERSHIP, new CompoundTag());
        return data;
    }

    // =========================================================================
    //  Minimal BiMap helper (replaces the obfuscated gl<UUID,UUID> class)
    // =========================================================================

    private static final class BiMap<L, R> implements Iterable<Map.Entry<L, R>> {
        private final HashMap<L, R> ltr = new HashMap<>();
        private final HashMap<R, L> rtl = new HashMap<>();

        public void put(L left, R right) {
            ltr.put(left, right);
            rtl.put(right, left);
        }

        @org.jetbrains.annotations.Nullable
        public R getByKey(L left)  { return ltr.get(left); }

        @org.jetbrains.annotations.Nullable
        public L getByRight(R right) { return rtl.get(right); }

        public void removeByKey(L left) {
            R right = ltr.remove(left);
            if (right != null) rtl.remove(right);
        }

        public void removeByRight(R right) {
            L left = rtl.remove(right);
            if (left != null) ltr.remove(left);
        }

        public void clear() { ltr.clear(); rtl.clear(); }

        public int size() { return ltr.size(); }

        @Override
        public java.util.Iterator<Map.Entry<L, R>> iterator() {
            return ltr.entrySet().iterator();
        }

        public Set<Map.Entry<L, R>> entrySet() { return ltr.entrySet(); }
    }
}
