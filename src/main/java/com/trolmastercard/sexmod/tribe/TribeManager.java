package com.trolmastercard.sexmod.tribe;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.TribeHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.*;

/**
 * TribeManager - ported from ax.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * <p>Manages all active {@link TribeData} instances keyed by tribe UUID,
 * bed-block ownership tracking, and world persistence via {@link TribesWorldData}.</p>
 *
 * <p>Call {@link #clearAll()} when the server shuts down / world unloads.</p>
 */
public final class TribeManager {

    // =========================================================================
    //  Constants
    // =========================================================================

    /** Maximum kobolds in one tribe before it stops growing. */
    static final int MAX_TRIBE_SIZE = 4;

    /**
     * Formation offsets for the 5 kobolds spawned together.
     * Index 0 = leader (body size 0.25), indices 1-3 = members.
     */
    static final Vec3[] SPAWN_OFFSETS = {
        new Vec3( 0.0,  0.0,  0.0),
        new Vec3( 0.5,  0.0,  0.0),
        new Vec3(-0.5,  0.0,  0.0),
        new Vec3( 0.0,  0.0,  0.5),
        new Vec3( 0.0,  0.0, -0.5)
    };

    // =========================================================================
    //  State (server-side only)
    // =========================================================================

    /** All live tribes, keyed by tribe UUID. */
    private static final HashMap<UUID, TribeData> tribes = new HashMap<>();

    /**
     * Maps each {@link KoboldEntity} to the two {@link BlockPos} halves of
     * its claimed bed.
     */
    static HashMap<KoboldEntity, BlockPos[]> koboldBedMap = new HashMap<>();

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    public static void clearAll() {
        tribes.clear();
        koboldBedMap.clear();
    }

    // =========================================================================
    //  Tribe spawning
    // =========================================================================

    /**
     * Spawns a new tribe of kobolds (1 leader + 3 members + 1 extra) at the
     * given world position and registers them with a new tribe UUID.
     *
     * Equivalent to the original {@code a(World, Vec3d)} method.
     */
    public static void spawnTribeAt(Level level, Vec3 pos) {
        UUID tribeId = UUID.randomUUID();
        RandomSource rng = level.getRandom();

        // Body sizes: index 0 = leader (0.25), indices 1-3 = random members
        float[] bodySizes = new float[4];
        bodySizes[0] = 0.25F;
        for (int i = 1; i < bodySizes.length; i++) bodySizes[i] = KoboldEntity.randomBodySize(rng);

        List<KoboldEntity> kobolds = new ArrayList<>();
        for (float size : bodySizes) {
            KoboldEntity k = KoboldEntity.spawn(level, tribeId, size);
            kobolds.add(k);
        }

        EyeAndKoboldColor color =
            EyeAndKoboldColor.values()[rng.nextInt(EyeAndKoboldColor.values().length)];

        TribeData data = new TribeData(tribeId, color, kobolds.get(0), kobolds);
        tribes.put(tribeId, data);

        for (int i = 0; i < kobolds.size(); i++) {
            KoboldEntity k = kobolds.get(i);
            Vec3 offset = SPAWN_OFFSETS[Math.min(i, SPAWN_OFFSETS.length - 1)];
            k.setPos(pos.x + offset.x, pos.y, pos.z + offset.z);
            level.addFreshEntity(k);
        }
    }

    // =========================================================================
    //  Tribe creation / modification
    // =========================================================================

    public static boolean tribeExists(UUID tribeId) {
        return tribes.get(tribeId) != null;
    }

    public static void setMaster(UUID tribeId, UUID masterPlayerUUID) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.setMasterUUID(masterPlayerUUID);
    }

    /** Creates an empty tribe (no kobolds yet). */
    public static void createEmptyTribe(UUID tribeId, EyeAndKoboldColor color) {
        if (tribes.containsKey(tribeId)) {
            Main.LOGGER.info("tribe of UUID {} already exists", tribeId);
            return;
        }
        tribes.put(tribeId, new TribeData(tribeId, color));
    }

    // =========================================================================
    //  Member management
    // =========================================================================

    /** Adds {@code kobold} to tribe {@code tribeId} and updates its synced data. */
    public static void addMember(UUID tribeId, KoboldEntity kobold) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }

        data.addMember(kobold);
        tribes.put(tribeId, data);

        // Sync tribe UUID to entity's DataParameter
        kobold.entityData.set(KoboldEntity.TRIBE_ID, Optional.of(tribeId));

        // Sync tribe name if not already set
        if (!kobold.isLeader()) {
            kobold.entityData.set(KoboldEntity.TRIBE_NAME, data.getColor().toString());
        }
    }

    /** Removes {@code kobold} from its tribe, potentially disbanding the tribe. */
    public static void removeMember(UUID tribeId, KoboldEntity kobold) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }

        data.removeMember(kobold);
        data.removeKnownPosition(kobold.getUUID());

        // Replace leader if needed
        if (data.getLeader() != null
            && data.getLeader().getId() == kobold.getId()) {
            KoboldEntity newLeader = data.findSmallestMember();
            if (newLeader != null) data.setLeader(newLeader);
        }

        // Notify all tasks that reference this kobold
        for (TribeTask task : data.getTasks()) task.onMemberRemoved(kobold);

        // Disband if empty and kobold had a master
        if (!data.getMembers().isEmpty()) {
            tribes.put(tribeId, data);
            return;
        }
        if (!kobold.hasMaster()) return;

        Player master = kobold.getMasterPlayer();
        if (master != null) {
            Set<BlockPos> allBlocks = new HashSet<>();
            allBlocks.addAll(data.getBedPositions());
            allBlocks.addAll(data.getChestPositions());
            for (TribeTask t : data.getTasks()) allBlocks.addAll(t.getTargetBlocks());

            ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (ServerPlayer) master),
                new TribeHighlightPacket(allBlocks, false)
            );
            master.sendSystemMessage(Component.literal(
                "\u00a7cur \u00a7ftribe \u00a7chas been \u00a7feradicated \u00a7fuwu"));
        }
    }

    // =========================================================================
    //  Leader
    // =========================================================================

    public static void setLeader(UUID tribeId, KoboldEntity kobold) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.setLeader(kobold);
    }

    public static void refreshLeader(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }

        KoboldEntity leader = data.getLeader();
        if (leader != null) {
            if (leader.isRemoved()) {
                data.setLeader(data.findSmallestMember());
            }
            return;
        }
        data.setLeader(data.findSmallestMember());
    }

    @Nullable
    public static KoboldEntity getLeader(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return null; }
        return data.getLeader();
    }

    public static boolean isLeader(UUID tribeId, KoboldEntity kobold) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return false; }
        if (data.getLeader() == null) return false;
        return data.getLeader().getId() == kobold.getId();
    }

    // =========================================================================
    //  Phase
    // =========================================================================

    public static TribePhase getPhase(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return TribePhase.REST; }
        return data.getPhase();
    }

    public static void setPhase(UUID tribeId, TribePhase phase) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.setPhase(phase);
    }

    public static int getMemberCount(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return 0; }
        return data.getTotalMemberCount();
    }

    public static List<KoboldEntity> getLiveMembers(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new ArrayList<>(); }
        return data.getMembers();
    }

    // =========================================================================
    //  Color
    // =========================================================================

    public static EyeAndKoboldColor getTribeColor(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return KoboldEntity.DEFAULT_COLOR; }
        return data.getColor();
    }

    // =========================================================================
    //  Beds
    // =========================================================================

    public static boolean isBedClaimedByTribe(BlockPos pos) {
        for (BlockPos[] halves : koboldBedMap.values()) {
            if (halves[0].equals(pos) || halves[1].equals(pos)) return true;
        }
        return false;
    }

    @Nullable
    public static BlockPos[] getKoboldBedHalves(KoboldEntity kobold) {
        return koboldBedMap.get(kobold);
    }

    /**
     * Registers both halves of the bed at {@code pos} as claimed by {@code kobold}.
     * Looks for the adjacent half automatically.
     */
    public static void claimBed(KoboldEntity kobold, BlockPos pos) {
        Level level = kobold.level();
        BlockPos otherHalf = null;
        BlockPos[] adjacent = { pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west() };
        for (BlockPos adj : adjacent) {
            if (level.getBlockState(adj).getBlock() instanceof BedBlock) {
                otherHalf = adj; break;
            }
        }
        if (otherHalf == null) {
            Main.LOGGER.warn("bed @{} apparently doesn't have another half.. wtf", pos);
            return;
        }
        koboldBedMap.put(kobold, new BlockPos[]{ pos, otherHalf });
    }

    public static void releaseBed(KoboldEntity kobold) {
        koboldBedMap.remove(kobold);
    }

    public static HashSet<BlockPos> getTribeBedPositions(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashSet<>(); }
        return data.getBedPositions();
    }

    public static void addBedPos(UUID tribeId, BlockPos pos) {
        if (pos == null) return;
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getBedPositions().add(pos);
    }

    public static void removeBedPos(UUID tribeId, BlockPos pos) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getBedPositions().remove(pos);
    }

    // =========================================================================
    //  Chests
    // =========================================================================

    public static HashSet<BlockPos> getTribeChestPositions(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashSet<>(); }
        return data.getChestPositions();
    }

    public static void addChestPos(UUID tribeId, BlockPos pos) {
        if (pos == null) return;
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getChestPositions().add(pos);
    }

    public static void removeChestPos(UUID tribeId, BlockPos pos) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getChestPositions().remove(pos);
    }

    // =========================================================================
    //  Tasks
    // =========================================================================

    public static HashSet<BlockPos> getOrCreateTaskBlocks(UUID tribeId, TribeTask task) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashSet<>(); }
        if (task != null) {
            data.addTask(task);
            return task.getTargetBlocks();
        }
        return new HashSet<>();
    }

    public static HashSet<BlockPos> getTaskBlocksAt(UUID tribeId, BlockPos pos) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashSet<>(); }
        TribeTask task = null;
        for (TribeTask t : data.getTasks()) {
            if (t.getTargetBlocks().contains(pos)) { task = t; break; }
        }
        return getOrCreateTaskBlocks(tribeId, task);
    }

    public static void removeTask(UUID tribeId, TribeTask task) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.removeTask(task);
    }

    public static void removeTaskForKobold(UUID tribeId, KoboldEntity kobold) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        TribeTask target = null;
        for (TribeTask t : data.getTasks()) {
            if (t.hasWorker(kobold)) { target = t; break; }
        }
        if (target == null) {
            Main.LOGGER.warn("task of worker {} not found uwu", kobold.getUUID());
            return;
        }
        data.removeTask(target);
    }

    @Nullable
    public static Collection<TribeTask> getTasks(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return null; }
        return data.getTasks();
    }

    // =========================================================================
    //  Threat tracking
    // =========================================================================

    public static HashSet<LivingEntity> getThreats(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashSet<>(); }
        return data.getThreats();
    }

    public static void addThreat(UUID tribeId, LivingEntity entity) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getThreats().add(entity);
    }

    public static void removeThreat(UUID tribeId, LivingEntity entity) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getThreats().remove(entity);
    }

    public static boolean hasActiveSexAnimation(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return false; }
        for (KoboldEntity k : data.getMembers()) {
            if (k.getSexTarget() != null) return true;
        }
        return false;
    }

    // =========================================================================
    //  Alarm state
    // =========================================================================

    public static boolean isAlarmed(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return false; }
        return data.isAlarmed();
    }

    public static void setAlarmed(UUID tribeId, boolean alarmed) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.setAlarmed(alarmed);
    }

    // =========================================================================
    //  Known positions (for kobolds that have been unloaded)
    // =========================================================================

    @Nullable
    public static UUID getTribeIdForMaster(UUID masterUUID) {
        if (masterUUID == null) return null;
        for (Map.Entry<UUID, TribeData> entry : tribes.entrySet()) {
            TribeData data = entry.getValue();
            if (data.getTotalMemberCount() == 0 && data.getTotalMemberCountFromMap() == 0) continue;
            if (masterUUID.equals(data.getMasterUUID())) return entry.getKey();
        }
        return null;
    }

    @Nullable
    public static UUID getMasterForTribe(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return null; }
        List<KoboldEntity> members = data.getMembers();
        if (members.isEmpty()) return null;
        KoboldEntity first = members.get(0);
        if (!first.hasMaster()) return null;
        String uuidStr = first.entityData.get(BaseNpcEntity.MASTER_UUID);
        return UUID.fromString(uuidStr);
    }

    public static HashSet<BlockPos> getAllClaimedBlocks(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        HashSet<BlockPos> result = new HashSet<>();
        if (data == null) { logMissing(tribeId); return result; }
        for (TribeTask t : data.getTasks()) result.addAll(t.getTargetBlocks());
        result.addAll(data.getChestPositions());
        result.addAll(data.getBedPositions());
        return result;
    }

    /** Returns the known-positions map for cleanup validation. */
    public static HashMap<UUID, BlockPos> getKnownPositions(UUID tribeId, Level level) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return new HashMap<>(); }

        HashMap<UUID, BlockPos> map = data.getKnownPositions();
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, BlockPos> entry : map.entrySet()) {
            BlockPos pos = entry.getValue();
            if (!level.isLoaded(pos)) continue;
            AABB search = new AABB(pos).inflate(3);
            List<KoboldEntity> nearby = level.getEntitiesOfClass(KoboldEntity.class, search);
            boolean found = nearby.stream().anyMatch(k -> entry.getKey().equals(k.getUUID()));
            if (!found) stale.add(entry.getKey());
        }
        stale.forEach(map::remove);
        return map;
    }

    public static void setKnownPosition(UUID tribeId, UUID koboldId, BlockPos pos) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.getKnownPositions().put(koboldId, pos);
    }

    @Nullable
    public static BlockPos getMeetingPoint(UUID tribeId) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return null; }
        return data.getMeetingPoint();
    }

    public static void setMeetingPoint(UUID tribeId, BlockPos pos) {
        TribeData data = tribes.get(tribeId);
        if (data == null) { logMissing(tribeId); return; }
        data.setMeetingPoint(pos);
    }

    // =========================================================================
    //  Logging helpers
    // =========================================================================

    private static void logMissing(UUID tribeId) {
        Main.LOGGER.warn("tribe of UUID {} not found uwu", tribeId);
    }

    // =========================================================================
    //  WorldSavedData - persistence layer
    // =========================================================================

    /**
     * {@link SavedData} that serialises/deserialises all tribe state.
     *
     * Register via {@code level.getDataStorage().computeIfAbsent(..., "tribes")}
     * in the {@code LevelEvent.Load} handler.
     *
     * Equivalent to the original inner class {@code ax.b}.
     */
    @Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TribesWorldData extends SavedData {

        public static final String KEY = "tribes";

        public TribesWorldData() {}

        // -- Event handlers ------------------------------------------------

        @SubscribeEvent
        public static void onWorldSave(LevelEvent.Save event) {
            if (event.getLevel().isClientSide()) return;
            // Saving is triggered automatically by the server; setDirty() is enough
        }

        @SubscribeEvent
        public static void onWorldLoad(LevelEvent.Load event) {
            if (event.getLevel().isClientSide()) return;
            // Load is handled by the server's DataStorage on world init
        }

        /** Prevents kobolds from sleeping in tribe-claimed beds. */
        @SubscribeEvent
        public static void onPlayerSleep(PlayerSleepInBedEvent event) {
            if (isBedClaimedByTribe(event.getPos()))
                event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }

        /** Tracks newly placed chests adjacent to tribe-claimed chests (double-chest). */
        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (event.getLevel().isClientSide()) return;
            BlockPos placed = event.getPos();
            BlockState state = event.getState();
            Level level = (Level) event.getLevel();
            if (!(state.getBlock() instanceof ChestBlock)) return;

            BlockPos adjacentChest = findAdjacentMatchingChest(level, placed, state);
            if (adjacentChest == null) return;

            for (Map.Entry<UUID, TribeData> entry : tribes.entrySet()) {
                TribeData data = entry.getValue();
                if (!data.getChestPositions().contains(adjacentChest)) continue;
                data.getChestPositions().add(placed);

                UUID masterUUID = getMasterForTribe(entry.getKey());
                if (masterUUID == null) continue;
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(masterUUID);
                if (player == null) continue;
                ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new TribeHighlightPacket(placed, true));
            }
        }

        /** Removes chest/bed positions from tribes when blocks are broken. */
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getLevel().isClientSide()) return;
            BlockPos pos = event.getPos();
            Level level = (Level) event.getLevel();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock) {
                for (Map.Entry<UUID, TribeData> entry : tribes.entrySet()) {
                    TribeData data = entry.getValue();
                    if (!data.getChestPositions().contains(pos)) continue;
                    data.getChestPositions().remove(pos);
                    UUID masterUUID = getMasterForTribe(entry.getKey());
                    if (masterUUID == null) continue;
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(masterUUID);
                    if (player == null) continue;
                    ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new TribeHighlightPacket(pos, false));
                }
            }

            if (state.getBlock() instanceof BedBlock) {
                for (Map.Entry<UUID, TribeData> entry : tribes.entrySet()) {
                    TribeData data = entry.getValue();
                    if (!data.getBedPositions().contains(pos)) continue;
                    BlockPos otherHalf = BedBlock.getRelativePosition(
                        state, pos, state.getValue(BedBlock.PART));
                    data.getBedPositions().remove(pos);
                    data.getBedPositions().remove(otherHalf);

                    UUID masterUUID = getMasterForTribe(entry.getKey());
                    if (masterUUID == null) continue;
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(masterUUID);
                    if (player == null) continue;
                    Set<BlockPos> removed = new HashSet<>(Arrays.asList(pos, otherHalf));
                    ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new TribeHighlightPacket(removed, false));
                }
            }
        }

        /**
         * Injects a {@link TribeAttackGoal} into zombies, skeletons, and spiders
         * when they join the world, making them attack kobolds.
         */
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Zombie zombie) {
                zombie.goalSelector.addGoal(3, new TribeAttackGoal(zombie, true, false));
            }
            if (entity instanceof AbstractSkeleton skeleton) {
                skeleton.goalSelector.addGoal(3, new TribeAttackGoal(skeleton, true, false));
            }
            if (entity instanceof Spider spider) {
                spider.goalSelector.addGoal(3, new TribeAttackGoal(spider, true, true));
            }
        }

        // -- SavedData serialisation ---------------------------------------

        @Override
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
            int tribeIndex = 0;
            for (Map.Entry<UUID, TribeData> entry : tribes.entrySet()) {
                UUID tribeId   = entry.getKey();
                TribeData data = entry.getValue();
                UUID masterUUID = data.getMasterUUID();

                tag.putString("tribeId"    + tribeIndex, tribeId.toString());
                tag.putString("tribeColor" + tribeIndex, data.getColor().toString());
                if (masterUUID != null)
                    tag.putString("tribeMaster" + tribeIndex, masterUUID.toString());

                // Members
                int memberIndex = 0;
                Set<UUID> saved = new HashSet<>();
                for (KoboldEntity k : data.getMembers()) {
                    if (k.isRemoved()) continue;
                    BlockPos pos = k.blockPosition();
                    UUID kid = k.getUUID();
                    tag.putString(tribeId + "member" + memberIndex + "pos",
                        pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
                    tag.putString(tribeId + "member" + memberIndex + "id", kid.toString());
                    saved.add(kid);
                    memberIndex++;
                }
                // Also save known-position entries for unloaded kobolds
                for (Map.Entry<UUID, BlockPos> kp : data.getKnownPositions().entrySet()) {
                    if (saved.contains(kp.getKey())) continue;
                    BlockPos pos = kp.getValue();
                    tag.putString(tribeId + "member" + memberIndex + "pos",
                        pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
                    tag.putString(tribeId + "member" + memberIndex + "id", kp.getKey().toString());
                    memberIndex++;
                }

                // Beds
                int bedIdx = 0;
                for (BlockPos pos : data.getBedPositions()) {
                    tag.putString(tribeId + "bed" + bedIdx,
                        pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
                    bedIdx++;
                }

                // Chests
                int chestIdx = 0;
                for (BlockPos pos : data.getChestPositions()) {
                    tag.putString(tribeId + "chest" + chestIdx,
                        pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
                    chestIdx++;
                }

                // Tasks
                int taskIdx = 0;
                for (TribeTask task : data.getTasks()) {
                    tag.putString(tribeId + taskIdx + "taskKind", task.getKind().toString());
                    BlockPos tp = task.getTargetPos();
                    tag.putString(tribeId + taskIdx + "pos",
                        tp.getX() + "|" + tp.getY() + "|" + tp.getZ());
                    tag.putString(tribeId + taskIdx + "facing",
                        task.getFacing().getSerializedName());
                    int blockIdx = 0;
                    for (BlockPos bp : task.getTargetBlocks()) {
                        tag.putString(tribeId + taskIdx + "block" + blockIdx,
                            bp.getX() + "|" + bp.getY() + "|" + bp.getZ());
                        blockIdx++;
                    }
                    taskIdx++;
                }
                tribeIndex++;
            }
            return tag;
        }

        public static TribesWorldData load(net.minecraft.nbt.CompoundTag tag) {
            TribesWorldData data = new TribesWorldData();
            int i = 0;
            while (true) {
                String tribeIdStr = readAndClear(tag, "tribeId" + i);
                if (tribeIdStr.isEmpty()) break;

                UUID tribeId = UUID.fromString(tribeIdStr);
                String colorStr = readAndClear(tag, "tribeColor" + i);
                EyeAndKoboldColor color = EyeAndKoboldColor.valueOf(colorStr);
                createEmptyTribe(tribeId, color);

                String masterStr = readAndClear(tag, "tribeMaster" + i);
                if (!masterStr.isEmpty())
                    setMaster(tribeId, UUID.fromString(masterStr));

                // Members
                int j = 0;
                while (true) {
                    String posStr = readAndClear(tag, tribeId + "member" + j + "pos");
                    if (posStr.isEmpty()) break;
                    String idStr  = readAndClear(tag, tribeId + "member" + j + "id");
                    if (idStr.isEmpty()) break;
                    String[] xyz = posStr.split("\\|");
                    BlockPos pos = new BlockPos(
                        Integer.parseInt(xyz[0]),
                        Integer.parseInt(xyz[1]),
                        Integer.parseInt(xyz[2]));
                    setKnownPosition(tribeId, UUID.fromString(idStr), pos);
                    j++;
                }

                // Beds
                int b = 0;
                while (true) {
                    String posStr = readAndClear(tag, tribeId + "bed" + b);
                    if (posStr.isEmpty()) break;
                    String[] xyz = posStr.split("\\|");
                    addBedPos(tribeId, new BlockPos(
                        Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
                    b++;
                }

                // Chests
                int c = 0;
                while (true) {
                    String posStr = readAndClear(tag, tribeId + "chest" + c);
                    if (posStr.isEmpty()) break;
                    String[] xyz = posStr.split("\\|");
                    addChestPos(tribeId, new BlockPos(
                        Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
                    c++;
                }

                // Tasks
                int t = 0;
                while (true) {
                    String kind = readAndClear(tag, tribeId + t + "taskKind");
                    if (kind.isEmpty()) break;
                    String facingStr = readAndClear(tag, tribeId + t + "facing");
                    net.minecraft.core.Direction facing = facingStr.isEmpty()
                        ? net.minecraft.core.Direction.NORTH
                        : net.minecraft.core.Direction.byName(facingStr);

                    String posStr = readAndClear(tag, tribeId + t + "pos");
                    String[] xyz  = posStr.split("\\|");
                    BlockPos taskPos = new BlockPos(
                        Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));

                    Set<BlockPos> blocks = new HashSet<>();
                    int bl = 0;
                    while (true) {
                        String bStr = readAndClear(tag, tribeId + t + "block" + bl);
                        if (bStr.isEmpty()) break;
                        String[] bxyz = bStr.split("\\|");
                        blocks.add(new BlockPos(
                            Integer.parseInt(bxyz[0]),
                            Integer.parseInt(bxyz[1]),
                            Integer.parseInt(bxyz[2])));
                        bl++;
                    }
                    TribeTask task = new TribeTask(taskPos,
                        TribeTask.Kind.valueOf(kind), blocks, facing);
                    removeTask(tribeId, task); // ensure no duplicate
                    getOrCreateTaskBlocks(tribeId, task);
                    t++;
                }
                i++;
            }
            return data;
        }

        // Helper: reads and clears a string tag (avoids reading stale values across loops)
        private static String readAndClear(net.minecraft.nbt.CompoundTag tag, String key) {
            String val = tag.getString(key);
            tag.putString(key, "");
            return val;
        }
    }

    // =========================================================================
    //  Helper
    // =========================================================================

    @Nullable
    private static BlockPos findAdjacentMatchingChest(Level level, BlockPos placed, BlockState state) {
        for (BlockPos adj : new BlockPos[]{
            placed.north(), placed.south(), placed.east(), placed.west()
        }) {
            BlockState adjState = level.getBlockState(adj);
            if (adjState.getBlock() instanceof ChestBlock) return adj;
        }
        return null;
    }
}
