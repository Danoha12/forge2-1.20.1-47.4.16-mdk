package com.trolmastercard.sexmod.entity;

import com.google.common.collect.ImmutableSet;
import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.registry.ModItems;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.registry.ModSounds;
// 🚨 COMENTADOS TEMPORALMENTE
//import com.trolmastercard.sexmod.tribe.Task;
//import com.trolmastercard.sexmod.tribe.TaskType;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.tribe.TribePhase;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.util.KoboldName;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

/**
 * KoboldEntity - ported from ff.class (Fapcraft 1.12.2 v1.1) to Forge 1.20.1.
 *
 * Extends BaseNpcEntity which holds the DataParameters shared with the parent
 * class (e4 in the original): MASTER_UUID, BODY_COLOR, EYE_COLOR, MODEL_DATA,
 * FROZEN, ANIMATION_FOLLOW_UP, and the animation-state machinery (fp / AnimState).
 */
public class KoboldEntity extends BaseNpcEntity implements GeoEntity {

  // =========================================================================
  //  SynchedEntityData
  // =========================================================================

  /** 0.0 - 0.25 body-size scalar (affects scale and pitch) */
  public static final EntityDataAccessor<Float> BODY_SIZE =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.FLOAT);

  /** In-world display name chosen from KoboldNames (not the vanilla CustomName) */
  public static final EntityDataAccessor<String> KOBOLD_NAME =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.STRING);

  /** True while the tribe is under threat / kobold is in combat posture */
  public static final EntityDataAccessor<Boolean> IS_ALARMED =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);

  /** True when this kobold is marked as a defender by the tribe AI */
  public static final EntityDataAccessor<Boolean> IS_DEFENDING =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);

  /** Cached tribe name (shown in GUI) */
  public static final EntityDataAccessor<String> TRIBE_NAME =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.STRING);

  /** True when the tribe has enemies in range */
  public static final EntityDataAccessor<Boolean> IS_TRIBE_ATTACKING =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);

  /** True while the kobold is in the tree-felling mining animation */
  public static final EntityDataAccessor<Boolean> IS_MINING_TREE =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);

  /** UUID of the tribe this kobold belongs to - empty for untamed strays */
  public static final EntityDataAccessor<Optional<UUID>> TRIBE_ID =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  // =========================================================================
  //  Constants
  // =========================================================================

  public static final EyeAndKoboldColor DEFAULT_BODY_COLOR = EyeAndKoboldColor.PURPLE;
  public static final float             MAX_BODY_SIZE       = 0.25F;

  // Timing (ticks)
  private static final int ATTACK_SOUND_TICK   = 22;
  private static final int ATTACK_HIT_TICK     = 32;
  private static final int ATTACK_TOTAL_TICKS  = 84;
  private static final int HEAL_INTERVAL       = 100;
  private static final float HEAL_AMOUNT       = 2.0F;
  private static final int GREETING_COOLDOWN   = 300;  // ticks between "hey master" greetings
  private static final float GREETING_DIST     = 2.0F; // blocks  triggers greeting
  private static final float ATTACK_RANGE      = 2.0F;
  private static final float ATTACK_DAMAGE     = 5.0F;
  private static final float TEMPT_SPEED       = 0.4F;
  private static final double FOLLOW_SPEED     = 0.35D;
  private static final double FOLLOW_SPEED_RUN = 0.7D;
  private static final int    CUM_COUNTER_MAX  = 132;  // ticks before spawning tribe egg
  private static final float  ENEMY_SCAN_RADIUS = 30.0F;
  private static final float  TRIBE_SCAN_RADIUS = 10.0F;

  /** Base max-health; affected by body_size on creation */
  public static double baseMaxHealth = 69.0D;

  // =========================================================================
  //  Fields
  // =========================================================================

  /** 27-slot inventory carried by the kobold */
  public final ItemStackHandler inventory = new ItemStackHandler(27);

  // --- combat ---
  private int attackTick     = 0;
  private int attackCooldown = 0;

  // --- healing ---
  private int healTick = 0;

  // --- mining / tree-felling ---
  @Nullable private BlockPos  mineTarget      = null;
  private int                 mineHitTimer    = 24;
  private int                 mineCountdown   = 0;
  @Nullable private ItemStack heldSapling     = null;

  // --- pathing memory ---
  private Vec3      lastPosition    = Vec3.ZERO;
  @Nullable private BlockPos wanderTarget     = null;
  private boolean   headingToWork   = true;

  // --- chest deposit ---
  private int depositAttemptCooldown = 0;

  // --- block save/restore (for bed placement) ---
  @Nullable private BlockState savedGroundState = null;
  @Nullable private BlockState savedBedState    = null;
  @Nullable private BlockPos   savedBedPos      = null;

  // --- sex-animation bookkeeping ---
  private int     cumFrameCounter     = -1;     // counts toward CUM_COUNTER_MAX
  private int     moanCounter         = 0;
  private boolean blowjobSideParity   = true;   // R/L alternation
  private boolean blowjobSwitching    = false;
  public  boolean showEgg             = false;  // rendered by client

  // --- greeting ---
  private static long lastGreetingWorldTime = Long.MIN_VALUE;
  private float       prevDistToMaster      = Float.MAX_VALUE;

  // --- misc ---
  public  boolean colorEditedManually = false;
  private int     idleAttackTimer     = 0;

  // =========================================================================
  //  GeckoLib
  // =========================================================================

  private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

  // =========================================================================
  //  Constructor
  // =========================================================================

  public KoboldEntity(EntityType<? extends KoboldEntity> type, Level level) {
    super(type, level);
  }

  // --- Factory methods ------------------------------------------------------

  /** Create a stray kobold that belongs to an existing tribe */
  public static KoboldEntity createForTribe(Level level, UUID tribeId) {
    return createForTribe(level, tribeId, randomBodySize());
  }

  public static KoboldEntity createForTribe(Level level, UUID tribeId, float bodySize) {
    baseMaxHealth = 10.0D - bodySize * 25.0D;
    KoboldEntity kobold = new KoboldEntity(ModEntities.KOBOLD.get(), level);
    kobold.entityData.set(TRIBE_ID, Optional.of(tribeId));
    kobold.entityData.set(BODY_SIZE, bodySize);
    return kobold;
  }

  public static float randomBodySize() {
    return (float)(Math.random() * MAX_BODY_SIZE);
  }

  // =========================================================================
  //  Attributes
  // =========================================================================

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,      baseMaxHealth)
            .add(Attributes.MOVEMENT_SPEED,  0.5D)
            .add(Attributes.FOLLOW_RANGE,    30.0D);
  }

  // =========================================================================
  //  SynchedEntityData init
  // =========================================================================

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData(); // registers MASTER_UUID, BODY_COLOR, EYE_COLOR, MODEL_DATA, FROZEN, etc.

    EyeAndKoboldColor randomColor =
            EyeAndKoboldColor.values()[random.nextInt(EyeAndKoboldColor.values().length)];

    this.entityData.define(TRIBE_ID,          Optional.empty());
    this.entityData.define(BODY_SIZE,          0.0F);
    // Asumimos que la clase utilitaria se llama KoboldName, cámbialo a KoboldNames si así se llama.
    this.entityData.define(KOBOLD_NAME,        KoboldName.randomName());
    this.entityData.define(IS_ALARMED,         false);
    this.entityData.define(IS_DEFENDING,       false);
    this.entityData.define(TRIBE_NAME,         "null");
    this.entityData.define(IS_TRIBE_ATTACKING, false);
    this.entityData.define(IS_MINING_TREE,     false);

    // These are initialised here (not in BaseNpcEntity.defineSynchedData)
    // because the random eye color needs to be set per-instance:
    this.entityData.set(EYE_COLOR, new BlockPos(randomColor.getMainColor()));
    this.entityData.set(BODY_COLOR, DEFAULT_BODY_COLOR.name());
  }

  // =========================================================================
  //  Goals
  // =========================================================================

  @Override
  protected void registerGoals() {
    this.followOwnerGoal = new KoboldFollowOwnerGoal(this, 3.0F, 1.0F);

    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new TemptGoal(this, TEMPT_SPEED, false,
            stack -> stack.getItem() == ModItems.KOBOLD_TREAT.get()));
    goalSelector.addGoal(3, new KoboldFollowLeaderGoal(this));
    goalSelector.addGoal(5, followOwnerGoal);
  }

  // =========================================================================
  //  Eye height / hitbox
  // =========================================================================

  @Override
  public float getEyeHeight(Pose pose) {
    return 0.94F;
  }

  // =========================================================================
  //  Right-click interaction  (func_184645_a - mobInteract)
  // =========================================================================

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {

    // While in a sex animation the kobold ignores interaction
    if (getSexTarget() != null) return InteractionResult.PASS;

    ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
    ItemStack offHand  = player.getItemInHand(InteractionHand.OFF_HAND);

    // -- Name-tag rename (only master can rename) --------------------------
    ItemStack nameStack = mainHand.getItem() instanceof net.minecraft.world.item.NameTagItem
            ? mainHand : offHand;
    if (nameStack.getItem() instanceof net.minecraft.world.item.NameTagItem) {
      if (isMaster(player)) {
        this.entityData.set(KOBOLD_NAME, nameStack.getHoverName().getString());
        nameStack.shrink(1);
        return InteractionResult.sidedSuccess(level().isClientSide);
      }
    }

    // -- Alarmed / sleeping - no interaction ------------------------------
    if (this.entityData.get(IS_ALARMED))           return InteractionResult.PASS;
    if (getAnimState() == AnimState.SLEEP)         return InteractionResult.PASS;

    ItemStack whistle = mainHand.getItem() == ModItems.WHISTLE.get()
            ? mainHand : offHand;

    // -- Not yet tamed: whistle opens tribe selection screen ---------------
    if (!isTamed()) {
      if (whistle.getItem() == ModItems.WHISTLE.get()) {
        if (!level().isClientSide) return InteractionResult.SUCCESS;

        Optional<UUID> tribeId = this.entityData.get(TRIBE_ID);
        if (tribeId.isEmpty() || !TribeManager.getPendingPositions().isEmpty()) {
          return InteractionResult.SUCCESS;
        }
        openTribeScreen(tribeId.get());
        return InteractionResult.SUCCESS;
      }
    } else {
      // -- Tamed: whistle opens inventory GUI (master only) --------------
      if (whistle.getItem() == ModItems.WHISTLE.get() && isMaster(player)) {
        if (!level().isClientSide) {
          player.openMenu(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
      }
    }

    // -- Default: start sex-scene selection -------------------------------
    if (level().isClientSide) {
      if (isTamed() && isMaster(player)) {
        openSexMenu(null); // SoundKey.GIRLS_KOBOLD_MASTER normally
      }
      openPlayerChoiceScreen(player);
    } else {
      setMasterUUID(player.getStringUUID());
      getNavigation().stop();
      facePlayer(player);
      this.entityData.set(FROZEN, true);
      setAnimState(AnimState.NULL);
    }

    return InteractionResult.sidedSuccess(level().isClientSide);
  }

  // =========================================================================
  //  Tick  (func_70619_bc - baseTick / func_70071_h_ - tick)
  // =========================================================================

  @Override
  public void baseTick() {
    super.baseTick();

    this.entityData.set(IS_TRIBE_ATTACKING, false);

    Optional<UUID> tribeIdOpt = this.entityData.get(TRIBE_ID);

    if (tribeIdOpt.isPresent()) {
      UUID tribeId = tribeIdOpt.get();
      tickCumReward(tribeId);
      TribeManager.heartbeat(tribeId);

      Player nearest = getNearestPlayer();
      if (nearest != null) {
        TribeManager.trackPlayer(tribeId, nearest.getUUID());
      }
    }

    // Sex-animation tick has priority
    if (tickSexAnimation()) return;

    // Sex target blocks further logic
    if (getSexTarget() != null) return;

    // Passive healing
    if (!this.entityData.get(IS_ALARMED)) {
      if (getHealth() < getMaxHealth()) {
        if (++healTick >= HEAL_INTERVAL) {
          setHealth(Math.min(getHealth() + HEAL_AMOUNT, getMaxHealth()));
          healTick = 0;
          spawnHeartParticles();
        }
      } else {
        healTick = 0;
      }
    }

    // Gravity while not frozen
    if (!this.entityData.get(FROZEN)) {
      setNoGravity(false);
    }

    if (tribeIdOpt.isEmpty()) return;

    UUID tribeId = tribeIdOpt.get();
    attackCooldown--;

    // Attack animation
    if (getAnimState() == AnimState.ATTACK) {
      tickAttack(tribeId);
      return;
    }

    // Sync combat flags
    boolean threatened = hasThreatNearby(tribeId, false);
    this.entityData.set(IS_ALARMED,          threatened);
    this.entityData.set(IS_DEFENDING,        TribeManager.isDefender(tribeId, this));
    this.entityData.set(IS_TRIBE_ATTACKING,  TribeManager.hasEnemies(tribeId));

    tickTribeAI(tribeId);
    tickGreeting();

    if (followOwnerGoal != null) {
      followOwnerGoal.setFollowTarget(getFollowTarget());
    }
  }

  @Override
  public void tick() {
    super.tick();
    tickGreetingClient();
    tickCombatIdleLines();
    tickWorldSync();
    tickBodyColorUpdate();
  }

  // --- Attack animation sub-tick --------------------------------------------

  private void tickAttack(UUID tribeId) {
    getNavigation().stop();
    this.yRot     = computeAngleToTarget();
    this.yBodyRot = this.yRot;
    attackTick++;

    if (attackTick == ATTACK_SOUND_TICK) {
      playAttackSound();
    }

    if (attackTick == ATTACK_HIT_TICK) {
      Set<LivingEntity> enemies  = TribeManager.getEnemies(tribeId);
      Set<LivingEntity> toRemove = new HashSet<>();
      for (LivingEntity enemy : enemies) {
        if (distanceTo(enemy) <= ATTACK_RANGE) {
          enemy.hurt(damageSources().mobAttack(this), ATTACK_DAMAGE);
          if (enemy.isDeadOrDying()) toRemove.add(enemy);
        }
      }
      toRemove.forEach(e -> TribeManager.removeEnemy(tribeId, e));
    }

    if (attackTick >= ATTACK_TOTAL_TICKS) {
      setAnimState(AnimState.NULL);
      this.entityData.set(FROZEN, false);
      attackTick = 0;
    }
  }

  // --- Greeting (client) ----------------------------------------------------

  private void tickGreetingClient() {
    if (!level().isClientSide) return;
    if (!isTamed() || getAnimState() != AnimState.NULL) return;
    if (this.entityData.get(IS_TRIBE_ATTACKING)) return;
    if (!getAnimationFollowUp().isEmpty()) return;

    String masterUUID = getMasterUUID();
    Player nearby = level().getNearestPlayer(this, TRIBE_SCAN_RADIUS);
    if (nearby == null) { prevDistToMaster = Float.MAX_VALUE; return; }
    if (!nearby.getStringUUID().equals(masterUUID)) return;

    float dist = (float) distanceTo(nearby);

    if (dist < GREETING_DIST && prevDistToMaster > GREETING_DIST) {
      long now = level().getGameTime();
      if (now - lastGreetingWorldTime > GREETING_COOLDOWN) {
        playSound(ModSounds.KOBOLD_YEP.get(), 1F, getPitch());
        sendChatBubble("Hey master!");
        lastGreetingWorldTime = now;
      }
    }

    prevDistToMaster = dist;
  }

  // --- Combat idle lines (client) -------------------------------------------

  private void tickCombatIdleLines() {
    if (!level().isClientSide) return;
    if (!isTamed()) return;
    if (getAnimState() != AnimState.NULL) return;
    if (!this.entityData.get(IS_TRIBE_ATTACKING)) return;

    Player master = getMasterPlayer();
    if (master != null && distanceTo(master) < TRIBE_SCAN_RADIUS) {
      if (++idleAttackTimer > GREETING_COOLDOWN) {
        idleAttackTimer = 0;
        String line = COMBAT_LINES[random.nextInt(COMBAT_LINES.length)];
        sendChatBubble(line);
      }
    }
  }

  // --- Tribe AI dispatcher --------------------------------------------------

  /** Top-level tribe AI - decides what the kobold does each tick. */
  private void tickTribeAI(UUID tribeId) {
    // Attack checks
    if (hasThreatNearby(tribeId, true)) return;

    // Determine current day/night phase
    TribePhase phase    = getCurrentPhase();
    TribePhase lastPhase = TribeManager.getPhase(tribeId);

    if (phase != lastPhase) {
      TribeManager.setPhase(tribeId, phase);
      switch (phase) {
        case REST   -> onPhaseChangeToRest(tribeId);
        case ACTIVE -> onPhaseChangeToActive(tribeId);
      }
    }

    switch (phase) {
      case REST   -> tickRestAI(tribeId);
      case ACTIVE -> tickActiveAI(tribeId);
    }
  }

  private void tickRestAI(UUID tribeId) {
    if (isLeader()) {
      tickLeaderRest(tribeId);
    } else {
      tickMemberRest(tribeId);
    }
  }

  private void tickActiveAI(UUID tribeId) {
    if (!hasThreatNearby(tribeId, true)) {
      if (isLeader()) {
        tickLeaderActive(tribeId);
      } else {
        tickMemberActive(tribeId);
      }
    }
  }

  // --- Leader active AI ----------------------------------------------------

  private void tickLeaderActive(UUID tribeId) {
    // 🚨 COMENTADO TEMPORALMENTE (Las tareas aún no existen en el mod)
    /*
    List<Task> tasks = TribeManager.getTasks(tribeId);
    if (tasks == null) return;

    for (Task task : tasks) {
      if (task.isAssignedTo(this)) {
        if (task.isDone(this)) {
          task.complete(this);
          setAnimState(AnimState.NULL);
          this.entityData.set(FROZEN, false);
        }
        executeTask(tribeId, task);
        return;
      }
    }
    dispatchTask(tribeId);
    */
  }

  // --- Deposit inventory into nearby chests --------------------------------

  /** Returns true if a deposit was initiated (kobold is busy). */
  private boolean tickDepositInventory(UUID tribeId) {
    if (isInventoryEmpty()) return false;

    Set<BlockPos> chestPositions = TribeManager.getChests(tribeId);
    if (chestPositions == null || chestPositions.isEmpty()) return false;

    BlockPos bestChest = null;
    for (BlockPos chestPos : chestPositions) {
      if (!hasSpaceInChest(chestPos)) continue;
      if (bestChest == null || distanceTo(chestPos) < distanceTo(bestChest)) {
        bestChest = chestPos;
      }
    }
    if (bestChest == null) return false;

    if (distanceTo(bestChest) < 2.0D) {
      depositToChest(bestChest);
      level().playSound(null, bestChest,
              net.minecraft.sounds.SoundEvents.CHEST_OPEN,
              net.minecraft.sounds.SoundSource.BLOCKS, 1F, 1F);
      return true;
    }

    // Too high - teleport; otherwise pathfind
    if (Math.abs(bestChest.getY() - blockPosition().getY()) > 4) {
      teleportTo(bestChest.getX() + 0.5, bestChest.getY(), bestChest.getZ() + 0.5);
    } else {
      BlockPos adj = findAdjacentWalkable(bestChest);
      getNavigation().moveTo(adj.getX(), adj.getY(), adj.getZ(), FOLLOW_SPEED);
    }
    return true;
  }

  // --- Phase transitions ----------------------------------------------------

  private void onPhaseChangeToRest(UUID tribeId) {
    wakeAllTribeMembers(tribeId);
    TribeManager.clearWorkTarget(tribeId);
  }

  private void onPhaseChangeToActive(UUID tribeId) {
    TribeManager.clearWorkTarget(tribeId);
    sendChatBubble("Time to work bitches!");
  }

  // --- Cum reward (egg spawn) -----------------------------------------------

  private void tickCumReward(UUID tribeId) {
    if (cumFrameCounter == -1) return;
    if (++cumFrameCounter < CUM_COUNTER_MAX) return;
    cumFrameCounter = -1;

    if (getAnimState() != AnimState.MATING_PRESS_CUM) return;

    UUID target = getSexTarget();
    if (target == null) return;

    Player player = level().getPlayerByUUID(target);
    if (player == null) return;

    EyeAndKoboldColor tribeColor = TribeManager.getTribeColor(tribeId);
    ItemStack eggStack = new ItemStack(ModItems.TRIBE_EGG.get(), 1);
    CompoundTag tag = eggStack.getOrCreateTag();
    tag.putString("tribeID",    tribeId.toString());
    tag.putString("tribeColor", tribeColor.toString());

    player.getInventory().add(eggStack);
  }

  // --- Heart-particle helper ------------------------------------------------

  private void spawnHeartParticles() {
    if (!(level() instanceof ServerLevel serverLevel)) return;
    serverLevel.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HEART,
            getX(), getY() + 1.0, getZ(),
            3, 0.2, 0.2, 0.2, 0.05
    );
  }

  // =========================================================================
  //  Sex-animation state machine
  // =========================================================================

  @Override
  protected void triggerFollowUpAnimation() {
    String followUp = getAnimationFollowUp();

    boolean hasPotionEffect = false; // hasEffect(ModEffects.LOVE_POTION.get());
    boolean isMasterTarget  = isTamed()
            && getSexTarget() != null
            && getMasterUUID().equals(getSexTarget().toString());
    boolean needsPayment    = !hasPotionEffect && !isMasterTarget;

    switch (followUp) {
      case "STARTBLOWJOB" -> {
        if (needsPayment && getAnimState() == AnimState.PAYMENT) {
          setAnimState(AnimState.PAYMENT);
        } else {
          setAnimState(AnimState.STARTBLOWJOB);
        }
      }
      case "KOBOLD_ANAL_START" -> {
        if (needsPayment && getAnimState() == AnimState.PAYMENT) {
          setAnimState(AnimState.PAYMENT);
        } else {
          setAnimState(AnimState.KOBOLD_ANAL_START);
        }
      }
      case "MATING_PRESS_START" ->
              setAnimState(AnimState.MATING_PRESS_START);
    }
  }

  @Override
  public void setAnimState(AnimState newState) {
    // Protect CUM states from being overridden mid-animation
    AnimState current = getAnimState();
    if (current == AnimState.MATING_PRESS_CUM
            && (newState == AnimState.MATING_PRESS_SOFT
            || newState == AnimState.MATING_PRESS_HARD)) return;
    if (current == AnimState.KOBOLD_ANAL_CUM
            && (newState == AnimState.KOBOLD_ANAL_SLOW
            || newState == AnimState.KOBOLD_ANAL_FAST)) return;
    if (current == AnimState.CUMBLOWJOB
            && (newState == AnimState.SUCKBLOWJOB_BLINK
            || newState == AnimState.THRUSTBLOWJOB)) return;

    if (newState == AnimState.MATING_PRESS_CUM) cumFrameCounter = 0;

    super.setAnimState(newState);
  }

  @Nullable
  public AnimState getLoopInterrupt(AnimState state) {
    return switch (state) {
      case SUCKBLOWJOB_BLINK -> AnimState.THRUSTBLOWJOB;
      case KOBOLD_ANAL_SLOW  -> AnimState.KOBOLD_ANAL_FAST;
      default                -> null;
    };
  }

  @Nullable
  public AnimState getCumState(AnimState state) {
    return switch (state) {
      case THRUSTBLOWJOB, SUCKBLOWJOB_BLINK -> AnimState.CUMBLOWJOB;
      case KOBOLD_ANAL_SLOW, KOBOLD_ANAL_FAST -> AnimState.KOBOLD_ANAL_CUM;
      case MATING_PRESS_HARD, MATING_PRESS_SOFT -> AnimState.MATING_PRESS_CUM;
      default -> null;
    };
  }

  // =========================================================================
  //  Approach animation (sex scene start - g() in original)
  // =========================================================================

  @Override
  protected boolean tickApproachAnimation() {
    if (!isApproaching()) return false;

    approachTick++;
    setNoAi(false);
    setNoGravity(false);

    if (approachTick > 40) {
      isApproachingFlag = false;
      approachTick = 0;

      Player partner = getPartnerPlayer();
      if (partner == null) return true;

      this.yRot = partner.yRot + 180.0F;
      this.entityData.set(FROZEN, true);
      partner.setNoGravity(true);
      setNoGravity(true);
      getNavigation().stop();
      onApproachComplete();
      return true;
    }

    this.yRot = computeAngleToTarget();
    setNoGravity(false);

    Vec3 midpoint = interpolateApproach(approachTick);
    moveTo(midpoint.x, Math.floor(midpoint.y), midpoint.z);
    setAnimState(AnimState.NULL);

    Optional<UUID> tribeIdOpt = this.entityData.get(TRIBE_ID);
    if (tribeIdOpt.isEmpty()) return true;

    // 🚨 COMENTADO TEMPORALMENTE
    // List<Task> tasks = TribeManager.getTasks(tribeIdOpt.get());
    // if (tasks != null) tasks.forEach(t -> t.completeForKobold(this));

    return true;
  }

  // =========================================================================
  //  Death  (func_70645_a - die)
  // =========================================================================

  @Override
  public void die(DamageSource cause) {
    super.die(cause);
    if (level().isClientSide) return;

    Optional<UUID> tribeIdOpt = this.entityData.get(TRIBE_ID);
    if (tribeIdOpt.isEmpty()) return;

    TribeManager.removeMember(tribeIdOpt.get(), this);

    if (isTamed()) {
      Player master = getMasterPlayer();
      if (master != null) {
        master.sendSystemMessage(Component.literal(
                String.format("\u00a7c%s\u00a7f has perished \u00a7cuwu", getKoboldName())
        ));
      }
    }
  }

  // =========================================================================
  //  NBT  (func_70014_b - addAdditionalSaveData / func_70037_a - readAdditionalSaveData)
  // =========================================================================

  @Override
  public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);

    nbt.putFloat  ("body_size",             this.entityData.get(BODY_SIZE));
    nbt.putInt    ("eyeColorX",             getEyeColorPos().getX());
    nbt.putInt    ("eyeColorY",             getEyeColorPos().getY());
    nbt.putInt    ("eyeColorZ",             getEyeColorPos().getZ());
    nbt.putString ("model",                 getModelData());
    nbt.putString ("name",                  this.entityData.get(KOBOLD_NAME));
    nbt.putString ("master",                getMasterUUID());
    nbt.put       ("inventory",             this.inventory.serializeNBT());
    nbt.putString ("bodyColor",             getBodyColor());
    nbt.putBoolean("editedColorManually",   this.colorEditedManually);

    this.entityData.get(TRIBE_ID).ifPresent(tribeId -> {
      nbt.putUUID   ("tribeId",   tribeId);
      nbt.putBoolean("isLeader",  TribeManager.isLeader(tribeId, this));
      nbt.putString ("tribeName", this.entityData.get(TRIBE_NAME));
    });
  }

  @Override
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);

    String model = nbt.getString("model");
    if (!model.isEmpty()) setModelData(model);

    int ex = nbt.getInt("eyeColorX");
    int ey = nbt.getInt("eyeColorY");
    int ez = nbt.getInt("eyeColorZ");
    BlockPos eyePos = new BlockPos(ex, ey, ez);
    if (!eyePos.equals(BlockPos.ZERO)) setEyeColorPos(eyePos);

    this.entityData.set(BODY_SIZE,   nbt.getFloat ("body_size"));
    this.entityData.set(KOBOLD_NAME, nbt.getString("name"));
    setMasterUUID(nbt.getString("master"));
    this.inventory.deserializeNBT(nbt.getCompound("inventory"));

    String bodyColor = nbt.getString("bodyColor");
    if (!bodyColor.isEmpty()) setBodyColor(bodyColor);

    this.colorEditedManually = nbt.getBoolean("editedColorManually");

    // Tribe membership
    if (nbt.hasUUID("tribeId") && !isRemoved()) {
      UUID tribeId = nbt.getUUID("tribeId");
      this.entityData.set(TRIBE_ID, Optional.of(tribeId));

      if (!TribeManager.tribeExists(tribeId)) {
        EyeAndKoboldColor color = EyeAndKoboldColor.safeValueOf(getBodyColor());
        TribeManager.createTribe(tribeId, color);
      }

      TribeManager.addMember(tribeId, this);
      if (nbt.getBoolean("isLeader")) TribeManager.setLeader(tribeId, this);
      this.entityData.set(TRIBE_NAME, nbt.getString("tribeName"));
    }
  }

  @Override
  public Vec3 getBonePosition(String boneName) {
    return null;
  }

  @Override
  public void triggerAction(String action, UUID playerId) {

  }

  // =========================================================================
  //  Inventory helpers
  // =========================================================================

  public boolean isInventoryEmpty() {
    for (int i = 0; i < inventory.getSlots(); i++) {
      if (!inventory.getStackInSlot(i).isEmpty()) return false;
    }
    return true;
  }

  public boolean addItemToInventory(ItemStack stack, boolean simulate, boolean dropIfFull) {
    return insertIntoHandler(inventory, stack, simulate, dropIfFull);
  }

  public boolean addItemToInventory(ItemStack stack) {
    return addItemToInventory(stack, false, true);
  }

  public boolean canFitItem(ItemStack stack) {
    return insertIntoHandler(inventory, stack, true, false);
  }

  private boolean insertIntoHandler(ItemStackHandler handler, ItemStack stack,
                                    boolean simulate, boolean dropIfFull) {
    // Phase 1: merge into existing matching stacks
    for (int i = 0; i < handler.getSlots(); i++) {
      ItemStack slot = handler.getStackInSlot(i);
      if (!slot.isEmpty()
              && ItemStack.isSameItem(slot, stack)
              && slot.getDamageValue() == stack.getDamageValue()) {
        int space = slot.getMaxStackSize() - slot.getCount();
        if (space > 0) {
          int toAdd = Math.min(space, stack.getCount());
          if (!simulate) slot.grow(toAdd);
          stack = stack.copyWithCount(stack.getCount() - toAdd);
          if (stack.isEmpty()) return true;
        }
      }
    }
    // Phase 2: place in first empty slot
    for (int i = 0; i < handler.getSlots(); i++) {
      if (handler.getStackInSlot(i).isEmpty()) {
        if (!simulate) handler.setStackInSlot(i, stack.copy());
        return true;
      }
    }
    // Full
    if (!simulate && dropIfFull) {
      ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), stack);
      level().addFreshEntity(drop);
    }
    return false;
  }

  // =========================================================================
  //  Chest deposit logic  (a(UUID, boolean) - tickDepositInventory)
  // =========================================================================

  private boolean hasSpaceInChest(BlockPos pos) {
    if (!(level().getBlockEntity(pos) instanceof
            net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return false;
    net.minecraftforge.items.IItemHandler handler =
            chest.getCapability(net.minecraftforge.items.ForgeCapabilities.ITEM_HANDLER).orElse(null);
    if (handler == null) return false;

    for (int i = 0; i < inventory.getSlots(); i++) {
      ItemStack held = inventory.getStackInSlot(i);
      if (held.isEmpty()) continue;
      for (int j = 0; j < handler.getSlots(); j++) {
        ItemStack result = handler.insertItem(j, held, true);
        if (result.getCount() != held.getCount()) return true;
      }
    }
    return false;
  }

  private void depositToChest(BlockPos pos) {
    if (!(level().getBlockEntity(pos) instanceof
            net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return;
    net.minecraftforge.items.IItemHandler handler =
            chest.getCapability(net.minecraftforge.items.ForgeCapabilities.ITEM_HANDLER).orElse(null);
    if (handler == null) return;

    for (int i = 0; i < inventory.getSlots(); i++) {
      ItemStack held = inventory.getStackInSlot(i);
      if (held.isEmpty()) continue;
      for (int j = 0; j < handler.getSlots(); j++) {
        ItemStack remainder = handler.insertItem(j, held, false);
        if (remainder.getCount() <= 0) {
          inventory.setStackInSlot(i, ItemStack.EMPTY);
          break;
        }
        inventory.setStackInSlot(i, remainder);
        held = remainder;
      }
    }
  }

  private double distanceTo(BlockPos pos) {
    return Math.sqrt(blockPosition().distSqr(pos));
  }

  // =========================================================================
  //  Pathfinding helpers
  // =========================================================================

  private BlockPos findAdjacentWalkable(BlockPos target) {
    BlockPos candidate = blockPosition().subtract(target);
    int distX = Math.abs(candidate.getX());
    int distZ = Math.abs(candidate.getZ());
    int total = distX + distZ;
    if (total < 20) return target;

    double ratio  = (double) Math.min(distX, distZ) / (distX + distZ);
    int    signX  = candidate.getX() > 0 ? 1 : -1;
    int    signZ  = candidate.getZ() > 0 ? 1 : -1;
    int    offsetX = (int)((signX * 20) * (distX == Math.min(distX, distZ) ? ratio : 1 - ratio));
    int    offsetZ = (int)((signZ * 20) * (distZ == Math.min(distX, distZ) ? ratio : 1 - ratio));
    BlockPos result = blockPosition().offset(offsetX, 0, offsetZ);
    int surfaceY = getSurfaceY(result.getX(), result.getZ());
    return new BlockPos(result.getX(), surfaceY + 1, result.getZ());
  }

  private int getSurfaceY(int x, int z) {
    return level().getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
  }

  // =========================================================================
  //  Phase detection
  // =========================================================================

  private TribePhase getCurrentPhase() {
    long dayTime = level().getDayTime();
    return (dayTime < 12000L) ? TribePhase.ACTIVE : TribePhase.REST;
  }

  // =========================================================================
  //  Sound helpers
  // =========================================================================

  public float getPitch() {
    float sizeRatio = (0.25F - getBodySize()) / 0.25F;
    return (float) Mth.lerp((double) sizeRatio, 0.9D, 1.1D);
  }

  // =========================================================================
  //  GeckoLib4 animation
  // =========================================================================

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(
            new AnimationController<>(this, "eyes",     0,  this::eyesPredicate),
            new AnimationController<>(this, "movement", 10, this::movementPredicate),
            new AnimationController<>(this, "action",   0,  this::actionPredicate)
                    .setSoundKeyframeHandler(this::handleSoundKeyframe)
    );
  }

  // --- Eyes controller -----------------------------------------------------

  private PlayState eyesPredicate(software.bernie.geckolib.core.animation.AnimationState<KoboldEntity> state) {
    state.getController().setAnimation(
            getAnimState() != AnimState.NULL
                    ? RawAnimation.begin().thenLoop("animation.kobold.null")
                    : RawAnimation.begin().thenLoop("animation.kobold.blink")
    );
    return PlayState.CONTINUE;
  }

  // --- Movement controller -------------------------------------------------

  private PlayState movementPredicate(software.bernie.geckolib.core.animation.AnimationState<KoboldEntity> state) {
    AnimState animState = getAnimState();

    // Any non-NULL action overrides movement
    if (animState != AnimState.NULL) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.null"));
      return PlayState.CONTINUE;
    }

    // Sitting / riding
    if (isPassenger()) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.sit"));
      return PlayState.CONTINUE;
    }

    double delta = Math.abs(xo - getX()) + Math.abs(zo - getZ());

    if (!this.entityData.get(FROZEN) && delta > 0.0D) {
      if (onGround() && Math.abs(Math.abs(yo) - Math.abs(getY())) < 0.1D) {
        this.yRot = yBodyRot;

        if (isCrouching()) {
          state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.crouch_walk"));
          return PlayState.CONTINUE;
        }
        if (this.entityData.get(IS_ALARMED)) {
          state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.run_armed"));
          return PlayState.CONTINUE;
        }
        if (delta > 0.2D) {
          state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.run"));
          return PlayState.CONTINUE;
        }
        state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.walk"));
        return PlayState.CONTINUE;
      }
      // Airborne
      state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.fly"));
      return PlayState.CONTINUE;
    }

    // Idle
    if (isCrouching()) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.kobold.crouch_idle"));
      return PlayState.CONTINUE;
    }
    state.getController().setAnimation(
            RawAnimation.begin().thenLoop(
                    this.entityData.get(IS_ALARMED)
                            ? "animation.kobold.idle_armed"
                            : "animation.kobold.idle"
            )
    );
    return PlayState.CONTINUE;
  }

  // --- Action controller ----------------------------------------------------

  private PlayState actionPredicate(software.bernie.geckolib.core.animation.AnimationState<KoboldEntity> state) {
    RawAnimation anim = switch (getAnimState()) {
      case NULL                -> RawAnimation.begin().thenLoop("animation.kobold.null");
      case ATTACK              -> RawAnimation.begin().thenPlay("animation.kobold.attack");
      case SLEEP, PAYMENT      -> RawAnimation.begin().thenLoop("animation.kobold.sit");
      case MINE                -> RawAnimation.begin().thenLoop("animation.kobold.fall_tree");
      case PAYMENT_ANIM        -> RawAnimation.begin().thenLoop("animation.kobold.paymentBackpack");
      case STARTBLOWJOB        -> RawAnimation.begin().thenPlay("animation.kobold.blowjobStart");
      case SUCKBLOWJOB_BLINK   -> {
        String suffix = (blowjobSideParity ? "R" : "L") + (blowjobSwitching ? "Switch" : "");
        yield RawAnimation.begin().thenLoop("animation.kobold.blowjobSlow" + suffix);
      }
      case THRUSTBLOWJOB       -> RawAnimation.begin().thenLoop("animation.kobold.blowjobFast");
      case CUMBLOWJOB          -> RawAnimation.begin().thenPlay("animation.kobold.blowjobCum");
      case KOBOLD_ANAL_START   -> RawAnimation.begin().thenPlay("animation.kobold.analStart");
      case KOBOLD_ANAL_SLOW    -> RawAnimation.begin().thenLoop("animation.kobold.analSoft");
      case KOBOLD_ANAL_FAST    -> RawAnimation.begin().thenLoop("animation.kobold.analHard");
      case KOBOLD_ANAL_CUM     -> RawAnimation.begin().thenLoop("animation.kobold.analCum");
      case MATING_PRESS_START  -> RawAnimation.begin().thenPlay("animation.kobold.mating_press_start");
      case MATING_PRESS_SOFT   -> RawAnimation.begin().thenLoop("animation.kobold.mating_press_soft");
      case MATING_PRESS_HARD   -> RawAnimation.begin().thenLoop("animation.kobold.mating_press_hard");
      case MATING_PRESS_CUM    -> RawAnimation.begin().thenLoop("animation.kobold.mating_press_cum");
    };
    state.getController().setAnimation(anim);
    return PlayState.CONTINUE;
  }

  // --- Sound keyframe handler -----------------------------------------------

  // 🚨 CORREGIDO EL IMPORT DEL EVENTO DE GECKOLIB
  private void handleSoundKeyframe(software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent<KoboldEntity> event) {
    String key = event.getKeyframeData().getSound();
    switch (key) {
      // Combat
      case "attackSound"    -> playSound(ModSounds.KOBOLD_ATTACK.get(),    1F, getPitch());
      // Generic
      case "plob"           -> playSound(ModSounds.MISC_PLOB.get(),        1F, 1F);
      case "touch"          -> playSound(ModSounds.MISC_TOUCH.get(),       1F, 1F);
      case "pounding"       -> playSound(ModSounds.MISC_POUNDING.get(),    1F, 1F);
      case "cum"            -> playSound(ModSounds.MISC_INSERTS.get(),     2F, 1F);
      case "cumLoud"        -> playSound(ModSounds.MISC_INSERTS.get(),     3F, 1F);
      case "cumQuiet"       -> playSound(ModSounds.MISC_INSERTS.get(),    1.5F,1F);
      // Kobold voice
      case "giggle"         -> playSound(ModSounds.GIRLS_KOBOLD_GIGGLE.get(),    1F, getPitch());
      case "moan"           -> playSound(ModSounds.GIRLS_KOBOLD_MOAN.get(),      1F, getPitch());
      case "orgasm"         -> playSound(ModSounds.GIRLS_KOBOLD_ORGASM.get(),    1F, getPitch());
      case "haa"            -> playSound(ModSounds.GIRLS_KOBOLD_HAA.get(),      0.7F,getPitch());
      case "breath"         -> playSound(ModSounds.GIRLS_KOBOLD_LIGHTBREATHING.get(),0.5F,getPitch());
      case "interested"     -> playSound(ModSounds.GIRLS_KOBOLD_INTERESTED.get(),1F, getPitch());
      case "yep"            -> playSound(ModSounds.GIRLS_KOBOLD_YEP.get(),       1F, getPitch());
      case "bjmoan"         -> playSound(ModSounds.GIRLS_KOBOLD_BJMOAN.get(),    1F, getPitch());
      // Moan counters
      case "moanMating" -> {
        if (--moanCounter <= 0) { moanCounter = 3; playSound(ModSounds.GIRLS_KOBOLD_MOAN.get(), 1F, getPitch()); }
      }
      case "analHardMSG1" -> {
        if (--moanCounter <= 0) { moanCounter = 4; playSound(ModSounds.GIRLS_KOBOLD_MOAN.get(), 1F, getPitch()); }
      }
      // Blowjob state transitions (client only)
      case "blowjobStartDone" -> {
        setAnimState(AnimState.SUCKBLOWJOB_BLINK);
        blowjobSwitching  = false;
        blowjobSideParity = true;
      }
      case "switch" -> {
        blowjobSwitching = random.nextBoolean();
        // clear GeckoLib animation cache via controller
      }
      case "endSwitch" -> {
        blowjobSwitching  = false;
        blowjobSideParity = !blowjobSideParity;
      }
      case "blowjobFastDone" -> {
        if (level().isClientSide)
          setAnimState(AnimState.SUCKBLOWJOB_BLINK);
      }
      case "blowjobCumDone" -> {
        if (level().isClientSide) { resetSexState(); }
      }
      // Anal state transitions
      case "analStartDone" -> {
        setAnimState(AnimState.KOBOLD_ANAL_SLOW);
      }
      case "analFastRapid" -> {
        if (level().isClientSide)
          setAnimState(AnimState.KOBOLD_ANAL_FAST);
      }
      case "analDone" -> {
        if (getAnimState() == AnimState.KOBOLD_ANAL_FAST)
          setAnimState(AnimState.KOBOLD_ANAL_SLOW);
      }
      case "analCumDone" -> {
        if (level().isClientSide) { resetSexState(); }
      }
      case "analHard" -> { }
      case "analSoft" -> { }
      // Mating press state transitions
      case "mating_press_startDone" -> { }
      case "mating_press_hardDone" -> {
        if (level().isClientSide) setAnimState(AnimState.MATING_PRESS_SOFT);
      }
      case "mating_press_softReady" -> {
        if (level().isClientSide) {
          setAnimState(AnimState.MATING_PRESS_HARD);
        }
      }
      case "mating_press_hardReady" -> {
        if (level().isClientSide) {
          stepUpMatingPress();
        }
      }
      case "mating_press_cumDone" -> {
        if (level().isClientSide) resetSexState();
      }
      // Camera events
      case "blackScreen"      -> { }
      case "paymentDone"      -> { if (level().isClientSide) triggerFollowUpAnimation(); }
      case "paymentMSG1"      -> {
        sendChatBubble("I'd like to use ur services owo");
        playSound(ModSounds.MISC_PLOB.get(), 1F, 1F);
      }
      case "blowjobStartMSG1" -> { if (level().isClientSide) positionCameraForBlowjob(0); }
      case "blowjobStartMSG2" -> { if (level().isClientSide) positionCameraForBlowjob(1); }
      case "analStartCam"     -> { if (level().isClientSide) positionCameraForAnal(); }
      case "matingCam"        -> { if (level().isClientSide) positionCameraForMating(0); }
      case "mating_cum_cam"   -> { if (level().isClientSide) positionCameraForMating(1); }
      // Egg spawn
      case "renderEgg" -> {
        showEgg = true;
        playSound(ModSounds.MISC_PLOB.get(), 0.5F, 1F);
      }
      // Dialogue
      case "cumMsg" -> {
        sendChatBubble("I.. hope I am satisfying you sir");
      }
    }
  }

  @Override
  public AnimatableInstanceCache getAnimatableInstanceCache() {
    return animCache;
  }

  // =========================================================================
  //  Accessors
  // =========================================================================

  public float getBodySize() { return entityData.get(BODY_SIZE); }
  public void  setBodySize(float v) { entityData.set(BODY_SIZE, v); }

  public String getKoboldName() { return entityData.get(KOBOLD_NAME); }
  public void   setKoboldName(String n) { entityData.set(KOBOLD_NAME, n); }

  public boolean isAlarmed()        { return entityData.get(IS_ALARMED); }
  public boolean isDefending()      { return entityData.get(IS_DEFENDING); }
  public boolean isTribeAttacking() { return entityData.get(IS_TRIBE_ATTACKING); }
  public boolean isMiningTree()     { return entityData.get(IS_MINING_TREE); }

  public Optional<UUID> getTribeId() { return entityData.get(TRIBE_ID); }

  // =========================================================================
  //  Combat-idle lines
  // =========================================================================

  private static final String[] COMBAT_LINES = {
          "suck my iron cock you worthless piece of shit!",
          "you'll die a fucking virgin!",
          "not even Johnny sins would wanna stick his cock up ur ass",
          "fuck you with ur borderline illegal fetishes!",
          "ur cum tastes terrible!",
          "I've always faked my orgasms when having sex with you!",
          "Not even Jenny would fuck you for 6 diamonds!",
          "U look like u'd use a shovel to mine diamonds, fucking idiot!",
          "Why tf does ur cock smell like my asshole???",
          "do all of us a favor and hit [ALT]+[F4]!",
          "I'm about to say the N word!",
          "you are under attack retard",
          "Eat my ass!",
          "my tongue is longer than ur fucking dick bitch!",
          "Ligma titties!",
          "touch some grass bitch!"
  };

  // =========================================================================
  //  Stub methods - implemented in BaseNpcEntity or separate helper classes
  // =========================================================================

  // NOTE: The following stubs reference methods that live in the parent entity class
  // (BaseNpcEntity, which maps to the original obfuscated "e4") or in
  // side-specific helpers.  Fill them in as you build out the rest of the mod.

  /** Returns the UUID of the entity in a sex animation with this kobold, or null. */
  @Nullable protected UUID getSexTarget() { return null; }

  /** Returns the partner player during an approach/sex animation. */
  @Nullable protected Player getPartnerPlayer() { return null; }

  /** Called when the approach-slide animation finishes. */
  protected void onApproachComplete() {}

  /** Returns whether the kobold is currently sliding toward the player. */
  protected boolean isApproaching() { return false; }
  protected boolean isApproachingFlag = false;
  protected int approachTick = 0;

  /** Interpolates the kobold's position during the approach-slide. */
  protected Vec3 interpolateApproach(int tick) { return position(); }

  /** True while the sex-animation is playing - suppresses normal AI. */
  protected boolean tickSexAnimation() { return false; }

  /** Returns the entity this kobold follows when tamed. */
  protected Vec3 getFollowTarget() { return position(); }

  /** Returns the nearest Player within scan range. */
  @Nullable
  protected Player getNearestPlayer() {
    return level().getNearestPlayer(this, TRIBE_SCAN_RADIUS);
  }

  /** Returns the master Player entity, or null. */
  @Nullable
  protected Player getMasterPlayer() {
    String uuid = getMasterUUID();
    if (uuid.isEmpty()) return null;
    try { return level().getPlayerByUUID(UUID.fromString(uuid)); }
    catch (IllegalArgumentException e) { return null; }
  }

  /** True if the given player is this kobold's master. */
  protected boolean isMaster(Player player) {
    return player.getStringUUID().equals(getMasterUUID());
  }

  /** True when a threatening entity is within range. */
  protected boolean hasThreatNearby(UUID tribeId, boolean engage) { return false; }

  /** True if this kobold is the tribe leader. */
  protected boolean isLeader() { return false; }

  /** Dispatches a work task to a tribe member. */
  protected void dispatchTask(UUID tribeId) {}

  // 🚨 COMENTADO TEMPORALMENTE
  // protected void executeTask(UUID tribeId, Task task) {}

  /** Sleep/wake logic during REST phase. */
  protected void tickLeaderRest(UUID tribeId) {}
  protected void tickMemberRest(UUID tribeId) {}
  protected void tickMemberActive(UUID tribeId) {}

  /** Wake all tribe kobolds. */
  protected void wakeAllTribeMembers(UUID tribeId) {}

  /** Updates the body-color synced data from the tribe color. */
  protected void tickBodyColorUpdate() {}

  /** Client-side: syncs world-time-dependent state. */
  protected void tickWorldSync() {}

  protected void tickGreeting() {}

  /** Compute angle (yRot) to face whatever the kobold is targeting. */
  protected float computeAngleToTarget() { return this.yRot; }

  protected void playAttackSound() {}
  protected void resetSexState() {}
  protected void stepUpMatingPress() {}
  protected void sendChatBubble(String text) {}

  protected void openTribeScreen(UUID tribeId) {}
  protected void openSexMenu(Object soundKey) {}
  protected void openPlayerChoiceScreen(Player player) {}

  protected void facePlayer(Player player) {
    double dx = this.getX() - player.getX();
    double dz = this.getZ() - player.getZ();
    this.yRot = (float)(Math.atan2(dz, dx) * (180D / Math.PI) + 90D);
  }

  protected void positionCameraForBlowjob(int phase) {}
  protected void positionCameraForAnal() {}
  protected void positionCameraForMating(int phase) {}

  // =========================================================================
  //  Animation state enum  (fp in original)
  // =========================================================================

  /**
   * All possible action-animation states for a kobold.
   * Maps directly to the original "fp" enum values.
   */
  public enum AnimState {
    NULL,
    ATTACK,
    SLEEP,
    MINE,
    PAYMENT,
    PAYMENT_ANIM,
    STARTBLOWJOB,
    SUCKBLOWJOB_BLINK,
    THRUSTBLOWJOB,
    CUMBLOWJOB,
    KOBOLD_ANAL_START,
    KOBOLD_ANAL_SLOW,
    KOBOLD_ANAL_FAST,
    KOBOLD_ANAL_CUM,
    MATING_PRESS_START,
    MATING_PRESS_SOFT,
    MATING_PRESS_HARD,
    MATING_PRESS_CUM
  }
}