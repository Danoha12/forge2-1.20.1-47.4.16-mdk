package com.trolmastercard.sexmod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.PathfinderMob;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BaseNpcEntity - ported from em.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Abstract base for all mod NPCs. Wraps GeckoLib animation, synced DataParams,
 * NPC UUID identity, partner tracking, home position, master (follow) state,
 * and the global NPC list used for lookup across all worlds.
 *
 * DataParameters (IDs preserved from original):
 *   v  (110) String  - MASTER_UUID      (master player UUID string, "" = none)
 *   G  (109) Boolean - SHOULD_AT_TARGET (teleport to targetPos every tick)
 *   e  (108) String  - TARGET_POS       ("|"-delimited x|y|z)
 *   w  (107) Float   - TARGET_YAW
 *   u  (106) String  - NPC_UUID         (this NPC's identity UUID)
 *   D  (105) Integer - MODEL_INDEX      (clothing/model variant)
 *   J  (104) String  - ANIM_STATE       (fp enum name)
 *   h  (103) String  - CUSTOM_NAME_TAG
 *   y  (102) String  - PARTNER_UUID     ("null" = no partner)
 *   a  (101) String  - WALK_SPEED_MODE  (WalkSpeedMode enum name)
 *   b  (100) String  - SUB_ANIM_KEY_1
 *   c  ( 99) String  - SUB_ANIM_KEY_2
 *
 * 1.12.2 - 1.20.1 key migrations:
 *   EntityCreature - PathfinderMob
 *   IAnimatable / AnimatedGeoModel - GeoEntity / GeckoLib 4
 *   EntityDataManager.func_187226_a - SynchedEntityData.defineId
 *   func_187225_a - entityData.get; func_187227_b - entityData.set
 *   func_70088_a - defineSynchedData
 *   func_70619_bc - aiStep
 *   func_70071_h_ - tick
 *   func_110147_ax - registerAttributes (via applyEntityAttributes)
 *   func_184651_r - registerGoals
 *   EntityAISwimming - FloatGoal; EntityAIWanderAvoidWater - WaterAvoidingRandomStrollGoal
 *   EntityAITempt - TemptGoal; hz - NpcOpenDoorGoal
 *   SharedMonsterAttributes - Attributes
 *   World - Level; WorldServer - ServerLevel
 *   em.ad() - getAllNpcs(); em.g(uuid) - getByNpcUUID(uuid)
 *   WorldClient.func_175644_a - level.getEntitiesOfClass (client)
 *   WorldServer.func_175644_a - serverLevel.getEntitiesOfClass (server)
 *   field_70170_p.field_72995_K - level.isClientSide()
 *   field_70128_L - isRemoved()
 *   func_70005_c_() - getName().getString()
 *   func_145747_a(ITextComponent) - sendSystemMessage
 *   func_174791_d() - position()
 *   func_180425_c() - blockPosition()
 *   func_70634_a - teleportTo
 *   func_70101_b - setRot; func_70034_d - setYBodyRot
 *   func_70080_a - moveTo
 *   ge.b.sendToServer - ModNetwork.CHANNEL.sendToServer
 *   ge.b.sendToAllTracking - sendToAllTracking
 *   new n(f(),key,val) - NpcSubAnimPacket
 *   new s(f()) - NpcSexEndPacket
 *   new gd(uuid,slot,fp) - NpcSubAnimStatePacket
 *   fs.b/a - NpcSexStateManager.add/remove
 *   g0.a() - ServerUtils.isServerRunning()
 */
public abstract class BaseNpcEntity extends PathfinderMob implements GeoEntity {

    // -- Global NPC list --------------------------------------------------------

    private static final Set<BaseNpcEntity> ALL_NPCS = Collections.newSetFromMap(new WeakHashMap<>());

    public static Set<BaseNpcEntity> getAllNpcs() {
        if (ServerUtils.isServerRunning()) {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                List<BaseNpcEntity> list = new ArrayList<>();
                for (var sl : server.getAllLevels())
                    list.addAll(sl.getEntitiesOfClass(BaseNpcEntity.class, sl.getWorldBorder().createInsideBoundingBox(1e7)));
                return new LinkedHashSet<>(list);
            }
        }
        // Client-side fallback
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.level != null)
            return new LinkedHashSet<>(mc.level.getEntitiesOfClass(BaseNpcEntity.class, mc.level.getWorldBorder().createInsideBoundingBox(1e7)));
        return Collections.emptySet();
    }

    /** Returns all server-side NPC instances matching the given NPC UUID. */
    public static List<BaseNpcEntity> getByNpcUUID(UUID npcUUID) {
        List<BaseNpcEntity> result = new ArrayList<>();
        for (BaseNpcEntity n : getAllNpcs())
            if (npcUUID.equals(n.getNpcUUID())) result.add(n);
        return result;
    }

    /** Returns the server-side NPC for the given NPC UUID, or null. */
    @Nullable
    public static BaseNpcEntity getServerNpc(UUID npcUUID) {
        for (BaseNpcEntity n : getAllNpcs())
            if (!n.level.isClientSide() && npcUUID.equals(n.getNpcUUID())) return n;
        return null;
    }

    /** Returns the client-side NPC for the given NPC UUID, or null. */
    @Nullable
    public static BaseNpcEntity getClientNpc(UUID npcUUID) {
        for (BaseNpcEntity n : getAllNpcs())
            if (n.level.isClientSide() && npcUUID.equals(n.getNpcUUID())) return n;
        return null;
    }

    /** Returns the NPC whose sex partner is the given player UUID. */
    @Nullable
    public static BaseNpcEntity getNpcBySexPartner(@Nullable UUID playerUUID, boolean clientSide) {
        if (playerUUID == null) return null;
        for (BaseNpcEntity n : getAllNpcs())
            if (n.level.isClientSide() == clientSide && playerUUID.equals(n.getPartnerUUID())) return n;
        return null;
    }

    // -- DataParameters ---------------------------------------------------------

    /** em.v (id 110) - master player UUID string ("" = none). */
    public static final EntityDataAccessor<String>  MASTER_UUID     = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.G (id 109) - should teleport to TARGET_POS every tick. */
    public static final EntityDataAccessor<Boolean> SHOULD_AT_TARGET= SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    /** em.e (id 108) - target position as "|"-delimited string. */
    public static final EntityDataAccessor<String>  TARGET_POS      = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.w (id 107) - target yaw. */
    public static final EntityDataAccessor<Float>   TARGET_YAW      = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    /** em.u (id 106) - this NPC's identity UUID. */
    public static final EntityDataAccessor<String>  NPC_UUID        = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.D (id 105) - model/clothing variant index. */
    public static final EntityDataAccessor<Integer> MODEL_INDEX     = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    /** em.J (id 104) - current AnimState enum name. */
    public static final EntityDataAccessor<String>  ANIM_STATE      = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.h (id 103) - custom name override tag. */
    public static final EntityDataAccessor<String>  CUSTOM_NAME_TAG = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.y (id 102) - sex partner UUID ("null" = none). */
    public static final EntityDataAccessor<String>  PARTNER_UUID    = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.a (id 101) - walk speed mode (WalkSpeedMode enum name). */
    public static final EntityDataAccessor<String>  WALK_SPEED_MODE = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.b (id 100) - sub-animation key 1 (e.g. "animationFollowUp"). */
    public static final EntityDataAccessor<String>  SUB_ANIM_KEY_1  = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    /** em.c (id 99)  - sub-animation key 2. */
    public static final EntityDataAccessor<String>  SUB_ANIM_KEY_2  = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);

    // -- Instance state ---------------------------------------------------------

    public Vec3    homePos    = Vec3.ZERO;
    public boolean isSexMode  = false;   // em.i  in sex mode
    public float   modelScale = 1.0F;    // em.n
    public boolean editorMode = false;   // em.F

    /** Wander AI goal ref (kept for subclass override). */
    public WaterAvoidingRandomStrollGoal wanderGoal;
    /** Tempt goal (approach player with items). */
    public TemptGoal temptGoal;

    // Path navigator ref from parent (set in defineSynchedData)
    public net.minecraft.world.entity.ai.navigation.PathNavigation pathNav;

    // Sub-animation variant map: key - (current, last)
    final Map<String, int[]> subAnimMap = new HashMap<>();

    // For animation state follow-up queuing
    final List<Map.Entry<AnimState, Map.Entry<List<String>, Integer>>> animQueue = null;

    // Temptation items (same as original: fish, wheat, bread, seeds)
    protected static final Set<net.minecraft.world.item.Item> TEMPT_ITEMS = new HashSet<>(Arrays.asList(
            Items.COD, Items.WHEAT, Items.BREAD, Items.WHEAT_SEEDS));

    // -- Constructor ------------------------------------------------------------

    protected BaseNpcEntity(EntityType<? extends BaseNpcEntity> type, Level level) {
        super(type, level);
        if (level.isClientSide()) {
            ensureControllerInit();
        }
        var nav = getNavigation();
        if (nav instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation gpn)
            gpn.setCanOpenDoors(true);
    }

    // -- Synced data ------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        pathNav = getNavigation();
        entityData.define(NPC_UUID,        UUID.randomUUID().toString());
        entityData.define(MODEL_INDEX,     1);
        entityData.define(ANIM_STATE,      AnimState.NULL.name());
        entityData.define(CUSTOM_NAME_TAG, "");
        entityData.define(PARTNER_UUID,    "null");
        entityData.define(SHOULD_AT_TARGET,false);
        entityData.define(TARGET_YAW,      0.0F);
        entityData.define(TARGET_POS,      "0|0|0");
        entityData.define(MASTER_UUID,     "");
        entityData.define(WALK_SPEED_MODE, WalkSpeedMode.WALK.name());
        entityData.define(SUB_ANIM_KEY_1,  "");
        entityData.define(SUB_ANIM_KEY_2,  "");
    }

    // -- Attributes & Goals -----------------------------------------------------

    @Override
    protected void registerGoals() {
        wanderGoal = new WaterAvoidingRandomStrollGoal(this, 0.35);
        temptGoal  = new TemptGoal(this, 0.4, net.minecraft.world.item.crafting.Ingredient.of(
                Items.COD, Items.WHEAT, Items.BREAD, Items.WHEAT_SEEDS), false);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, temptGoal);
        this.goalSelector.addGoal(3, new NpcOpenDoorGoal(this));
        this.goalSelector.addGoal(5, wanderGoal);
    }

    public static void applyAttributes(net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder builder) {
        PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 30.0);
    }

    // -- Core tick / aiStep -----------------------------------------------------

    @Override
    public void aiStep() {
        if (entityData.get(SHOULD_AT_TARGET)) {
            setYHeadRot(entityData.get(TARGET_YAW));
            Vec3 tp = getTargetPos();
            moveTo(tp.x, tp.y, tp.z, entityData.get(TARGET_YAW), getXRot());
            setYRot(entityData.get(TARGET_YAW));
        }
        if (homePos.equals(Vec3.ZERO)) homePos = Vec3.atCenterOf(blockPosition());
        tickNpcEditor();
    }

    @Override
    public void tick() {
        super.tick();
        tickAnimationFollowUp();
    }

    /** Tick the animation state follow-up (fp.followUp chain). */
    void tickAnimationFollowUp() {
        AnimState state = getAnimState();
        if (state == null) return;
        state.ticksPlaying[level.isClientSide() ? 1 : 0]++;
        if (state.ticksPlaying[level.isClientSide() ? 1 : 0] + 1 < state.length) return;
        if (state.followUp == null) return;
        if (!level.isClientSide()) setAnimStateFiltered(state.followUp);
    }

    /** NPC editor mode glowing / tribe check. */
    protected void tickNpcEditor() {
        if (!NpcEditor.enabled) return;
        Set<String> tribes = getTribeSet();
        if (tribes.isEmpty()) return;
        NpcTribeType fy = NpcTribeType.forEntity(this);
        String editorTribe = NpcEditor.getActiveTribe();
        Set<String> invalid = new HashSet<>();
        for (String t : tribes) {
            if (!editorTribe.equals(NpcEditor.getTribeFor(t, editorTribe))) { invalid.add(t); continue; }
            Set<NpcTribeType> allowed = NpcEditor.getTribeTypes(t);
            if (allowed != null && !allowed.isEmpty() && !allowed.contains(fy)) invalid.add(t);
        }
        if (!invalid.isEmpty()) {
            tribes.removeAll(invalid);
            setModelIndex(computeModelForTribes(tribes));
        }
    }

    // -- AnimState accessors ----------------------------------------------------

    public AnimState getAnimState() {
        try { return AnimState.valueOf(entityData.get(ANIM_STATE)); }
        catch (Exception e) { return AnimState.NULL; }
    }

    /**
     * Sets the AnimState with guard logic (mirrors em.b(fp)).
     * Clients send via NpcSubAnimPacket; server sets directly.
     */
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (cur == next) return;
        if (next == AnimState.ATTACK && cur != AnimState.NULL) return;
        AnimState safe = (next == null) ? AnimState.NULL : next;
        cur.ticksPlaying = new int[]{ 0, 0 };
        if (level.isClientSide()) {
            sendSubAnim("currentAction", safe.name());
        } else {
            entityData.set(ANIM_STATE, safe.name());
        }
    }

    /** Sets a sub-animation key-value pair (mirrors em.a(String,String)). */
    @OnlyIn(Dist.CLIENT)
    public void sendSubAnim(String key, String value) {
        ModNetwork.CHANNEL.sendToServer(new NpcSubAnimPacket(getNpcUUID(), key, value));
    }

    /** Sets sub-animation slot (slot 0 = SUB_ANIM_KEY_1, 1 = SUB_ANIM_KEY_2). */
    public void setSubAnimState(int slot, AnimState state) {
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new NpcSubAnimStatePacket(getNpcUUID(), slot, state));
    }

    public void setSubAnim(String key, String value) {
        if (level.isClientSide()) sendSubAnim(key, value);
        else entityData.set(key.equals(SUB_ANIM_KEY_1.toString()) ? SUB_ANIM_KEY_1 : SUB_ANIM_KEY_2, value);
    }

    // -- NPC UUID ---------------------------------------------------------------

    public UUID getNpcUUID() {
        try { return UUID.fromString(entityData.get(NPC_UUID)); }
        catch (Exception e) {
            UUID id = UUID.randomUUID();
            entityData.set(NPC_UUID, id.toString());
            return id;
        }
    }

    // -- Partner (sex partner) --------------------------------------------------

    @Nullable public UUID getPartnerUUID() {
        String s = entityData.get(PARTNER_UUID);
        return "null".equals(s) ? null : UUID.tryParse(s);
    }

    public void setPartnerUUID(@Nullable UUID uuid) {
        if (level.isClientSide())
            sendSubAnim("playerSheHasSexWith", uuid == null ? "null" : uuid.toString());
        else
            entityData.set(PARTNER_UUID, uuid == null ? "null" : uuid.toString());
    }

    public void setPartnerUUID(Player player) { setPartnerUUID(player.getGameProfile().getId()); }

    /** Returns true if this entity currently has a sex partner. */
    public boolean isSexModeActive() { return isSexMode; }

    @Nullable public Player getSexPartnerPlayer() {
        UUID id = getPartnerUUID();
        return id == null ? null : level.getPlayerByUUID(id);
    }

    // -- Master (follow target) -------------------------------------------------

    /** Returns true if the NPC has a master player. */
    public boolean hasMaster() { return !entityData.get(MASTER_UUID).isEmpty(); }

    @Nullable public UUID getMasterUUID() {
        String s = entityData.get(MASTER_UUID);
        return s.isEmpty() ? null : UUID.tryParse(s);
    }

    /** Returns the Optional UUID of the master (follow target). */
    public Optional<UUID> getMaster() { return Optional.ofNullable(getMasterUUID()); }

    public void setMaster(String uuidString) {
        if (level.isClientSide()) sendSubAnim("master", uuidString);
        else entityData.set(MASTER_UUID, uuidString);
    }

    public void stopFollow() {
        if (level.isClientSide()) {
            sendSubAnim("master", "");
            sendSubAnim("walk speed", WalkSpeedMode.WALK.name());
        } else {
            entityData.set(MASTER_UUID, "");
            entityData.set(WALK_SPEED_MODE, WalkSpeedMode.WALK.name());
        }
    }

    @Nullable public Player getMasterPlayer() {
        UUID id = getMasterUUID();
        return id == null ? null : level.getPlayerByUUID(id);
    }

    // -- Target position --------------------------------------------------------

    public Vec3 getTargetPos() {
        String[] parts = entityData.get(TARGET_POS).split("\\|");
        return new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    public void setTargetPos(Vec3 v) {
        if (level.isClientSide())
            sendSubAnim("targetPos", v.x + "f" + v.y + "f" + v.z + "f");
        else
            entityData.set(TARGET_POS, v.x + "|" + v.y + "|" + v.z);
    }

    public void setShouldBeAtTargetPos(boolean v) {
        if (level.isClientSide()) sendSubAnim("shouldbeattargetpos", String.valueOf(v));
        else entityData.set(SHOULD_AT_TARGET, v);
    }

    public boolean shouldBeAtTargetPos() { return entityData.get(SHOULD_AT_TARGET); }

    public Float getStoredYaw() { return entityData.get(TARGET_YAW); }
    public void setStoredYaw(float yaw) { entityData.set(TARGET_YAW, yaw); }

    // -- Model index ------------------------------------------------------------

    public int getModelIndex() { return entityData.get(MODEL_INDEX); }

    public void setModelIndex(int idx) {
        if (level.isClientSide()) sendSubAnim("currentModel", "0");
        else entityData.set(MODEL_INDEX, idx);
    }

    // -- Custom name ------------------------------------------------------------

    public String getCustomNameOverride() { return entityData.get(CUSTOM_NAME_TAG); }
    public void setCustomNameOverride(String name) { entityData.set(CUSTOM_NAME_TAG, name); }

    // -- Sex offset pos (player stand position in front of NPC) ----------------

    /** Returns the position the player should stand at for sex animations. */
    public Vec3 getSexOffsetPos() {
        return new Vec3(getX(), getY(), getZ());
    }

    // -- Arm height slot (for sub-anim targeting) -------------------------------

    public int getArmHeightSlot() {
        return entityData.get(MODEL_INDEX) == 0 ? 1 : 0;
    }

    // -- Controller init (called client-side in constructor) -------------------

    @OnlyIn(Dist.CLIENT)
    protected void ensureControllerInit() { /* subclasses call registerControllers */ }

    // -- In-combat / combat flag ------------------------------------------------

    public boolean isInCombat() { return getTarget() != null; }

    // -- Home position ----------------------------------------------------------

    public Vec3 getHomePos() { return homePos; }
    public void setHomePos(Vec3 pos) { homePos = pos; }

    // -- Message broadcast helpers ----------------------------------------------

    public void sendNpcMessage(String msg) {
        for (Player p : NpcUtils.getNearbyPlayers(this))
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }

    /** Broadcasts a particle to nearby players. */
    public void spawnParticleAt(net.minecraft.core.particles.ParticleType<?> type) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles((net.minecraft.core.particles.SimpleParticleType) type,
                    getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        }
    }

    // -- Sound helpers ----------------------------------------------------------

    public void playNpcSound(SoundEvent sound, float pitch) {
        level.playSound(null, blockPosition(), sound, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, pitch);
    }

    public void playNpcSound(SoundEvent[] sounds, float pitch) {
        playNpcSound(sounds[random.nextInt(sounds.length)], pitch);
    }

    // -- Cancel current action -------------------------------------------------

    public void cancelCurrentAction() {
        setPartnerUUID((UUID) null);
        setAnimStateFiltered(AnimState.NULL);
    }

    // -- Sex mode --------------------------------------------------------------

    public void setSexMode(boolean active) {
        isSexMode = active;
        if (active) NpcSexStateManager.add(this);
        else        NpcSexStateManager.remove(this);
    }

    // -- Open action menu helpers -----------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public static void openActionMenu(Player player, BaseNpcEntity npc, String[] actions, boolean showInventory) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new NpcContextMenuScreen(npc, player, actions, null, showInventory));
    }

    // -- Tribe helpers (stub - subclasses override or NpcTribeManager provides) -

    protected Set<String> getTribeSet() { return Collections.emptySet(); }

    protected int computeModelForTribes(Set<String> tribes) { return getModelIndex(); }

    // -- AABB expansion (NPC stands slightly above ground) ---------------------

    @Override
    public net.minecraft.world.phys.AABB getBoundingBox() {
        return super.getBoundingBox().move(0, 0.5, 0);
    }

    // -- Misc overrides --------------------------------------------------------

    @Override public boolean canBeLeashed(Player p) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAggressive() { return false; }

    // -- NBT -------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("homeX", homePos.x);
        tag.putDouble("homeY", homePos.y);
        tag.putDouble("homeZ", homePos.z);
        tag.putString("girlID", entityData.get(NPC_UUID));
        String name = getCustomNameOverride();
        if (!name.isEmpty()) tag.putString("sexmod:customname", name);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        homePos = new Vec3(tag.getDouble("homeX"), tag.getDouble("homeY"), tag.getDouble("homeZ"));
        String name = tag.getString("sexmod:customname");
        if (!name.isEmpty()) setCustomNameOverride(name);

        String savedId = tag.getString("girlID");
        if (savedId.isEmpty()) return;
        UUID savedUUID = UUID.fromString(savedId);

        // Dupe check
        for (BaseNpcEntity other : getAllNpcs()) {
            if (other.level.isClientSide() || other == this || other.isRemoved()) continue;
            if (savedUUID.equals(other.getNpcUUID())) {
                Main.LOGGER.warn("Dupe NPC '{}' with id '{}'  deleted.", getName().getString(), savedUUID);
                discard();
                return;
            }
        }
        entityData.set(NPC_UUID, savedUUID.toString());
    }

    // -- GeckoLib --------------------------------------------------------------

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar registrar);

    @Override
    public abstract AnimatableInstanceCache getAnimatableInstanceCache();

    /** Returns the primary AnimationController for this entity, or null. */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public software.bernie.geckolib.core.animation.AnimationController<?> getMainAnimationController() {
        try {
            var manager = getAnimatableInstanceCache()
                    .getManagerForId(getUUID().hashCode());
            if (manager == null) return null;
            return manager.getAnimationControllers().values().stream().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // -- Missing helper methods (stubs for subclass override) ------------------

    /** Alias for setAnimStateFiltered - used by entity subclasses. */
    public void setAnimState(AnimState state) { setAnimStateFiltered(state); }

    /** Returns true if this NPC is owned by the local Minecraft player (client-only). */
    @OnlyIn(Dist.CLIENT)
    public boolean isOwnerLocal() { return false; }

    /** Returns the owner player's UUID, or null. Overridden in PlayerKoboldEntity. */
    @Nullable public java.util.UUID getOwnerUUID() { return null; }

    /** Returns the owner player, or null. */
    @Nullable public net.minecraft.world.entity.player.Player getOwnerPlayer() { return null; }

    /** Stores the animation follow-up key for sound keyframe use. */
    private String animFollowUp = "";
    public void setAnimationFollowUp(String key) { this.animFollowUp = key; }
    public String getAnimFollowUp() { return animFollowUp; }

    /** Displays a localised dialogue above the NPC (client-only). */
    @OnlyIn(Dist.CLIENT)
    public void displayDialogue(String langKey) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null)
            mc.player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(langKey));
    }

    /** Displays a subtitle NPC message. */
    @OnlyIn(Dist.CLIENT)
    public void displayNpcSubtitleMessage(String langKey, boolean subtitle) {
        displayDialogue(langKey);
    }

    /** Plays a random sound from an array. */
    public void playRandSound(net.minecraft.sounds.SoundEvent[] sounds) {
        if (sounds == null || sounds.length == 0) return;
        playNpcSound(sounds[random.nextInt(sounds.length)], 1.0F);
    }

    /** Plays a random sound from an array at given pitch. */
    public void playRandSound(net.minecraft.sounds.SoundEvent[] sounds, float pitch) {
        if (sounds == null || sounds.length == 0) return;
        playNpcSound(sounds[random.nextInt(sounds.length)], pitch);
    }

    /** Plays sound at given indices from an array. */
    public void playSound(net.minecraft.sounds.SoundEvent[] sounds, int[] indices) {
        if (sounds == null || indices == null) return;
        for (int idx : indices)
            if (idx < sounds.length && sounds[idx] != null) playNpcSound(sounds[idx], 1.0F);
    }

    /** Returns the sit/ride position relative to this NPC. Override per entity. */
    public net.minecraft.world.phys.Vec3 getSitPosition() { return position().add(0, 0, -0.5); }

    /** Returns bone world position (placeholder - override in renderer). */
    @OnlyIn(Dist.CLIENT)
    public net.minecraft.world.phys.Vec3 getBoneWorldPos(String boneName) {
        return net.minecraft.world.phys.Vec3.ZERO;
    }

    /** Sets the entity pitch (xRot). */
    public void setPitch(float pitch) { setXRot(pitch); }

    /** Sets yaw and pitch. */
    public void setRotation(float yaw, float pitch) { setYRot(yaw); setXRot(pitch); }

    /** Teleports this entity to an offset relative to current position at given yaw/pitch. */
    public void setOffsetPosition(double dx, double dy, double dz, float yaw, float pitch) {
        teleportTo(getX() + dx, getY() + dy, getZ() + dz);
        setYRot(yaw); setXRot(pitch);
    }

    /** Called when a sex session ends. Override per entity. */
    public void onSessionEnd() { cancelCurrentAction(); }

    /** Opens the action menu for a player. Override per entity. */
    @OnlyIn(Dist.CLIENT)
    public void openActionMenu(net.minecraft.world.entity.player.Player player) {}



    // -- Override targets for subclasses --------------------------------------

    /** Returns the display name of this NPC type. */
    public String getNpcName() { return "NPC"; }

    /** Returns the eye height offset for camera positioning. */
    public float getEyeHeightOffset() { return 1.0F; }

    /** Called when a player selects an action from the menu. */
    public void onActionSelected(String action, java.util.UUID playerUUID) {}

    /** Called when a player selects an action (alias). */
    public void onActionChosen(String action, java.util.UUID playerUUID) {
        onActionSelected(action, playerUUID);
    }

    /** Returns the cum AnimState that follows from the given state. */
    @Nullable protected AnimState getCumState(AnimState current) { return null; }

    /** Returns the faster variant of the given slow AnimState. */
    @Nullable protected AnimState getFastVariant(AnimState current) { return null; }

    /** Called when a fast animation round completes. */
    protected void onFastRoundComplete() {}


}