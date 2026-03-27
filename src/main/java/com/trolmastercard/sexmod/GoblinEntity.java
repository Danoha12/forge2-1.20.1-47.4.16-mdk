package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModEntityRegistry;
import com.trolmastercard.sexmod.NpcModelCodeEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import java.util.*;

/**
 * GoblinEntity - ported from e3.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * The primary goblin NPC. Extends {@link NpcModelCodeEntity} (e4) and implements
 * the {@link GoblinInterface} (ai).
 *
 * Key features:
 *   - Carry mechanic: player right-clicks to pick up goblin (PICK_UP state).
 *     While carried, the goblin renders at the player's shoulder.
 *   - Throw mechanic: START_THROWING - THROWN; goblin is launched from shoulder.
 *   - Paizuri / Nelson / Breeding sex animation chains.
 *   - Queen goblin variant (aX=true): sits on throne, manages guards list,
 *     can become pregnant (aV=true).
 *   - Model code: encoded in 10 segments via NpcModelCodeEntity helpers.
 *
 * DataParameters (in addition to inherited ones):
 *   Q   (id 122, String)  - owner/carrier UUID string
 *   aK  (id 123, String)  - queen UUID string
 *   a0  (id 124, ItemStack)- item held (caught item or hand prop)
 *   aC  (id 125, Boolean)  - isTamed/isCarried
 *   aV  (id 126, Boolean)  - isPregnant (queens only)
 *
 * Dimensions: 0.5 - 0.99
 * Eye height: 0.75
 *
 * Animation states (from animation.goblin.*):
 *   IDLE, NULL, WALK, FLY, SIT, RUN, CATCH (1st/3rd person), THROW, THROWN,
 *   SHOULDER_IDLE, PICK_UP, AWAIT_PICK_UP, STAND_UP,
 *   PAIZURI_START/SLOW/FAST/CUM/IDLE,
 *   NELSON_INTRO/SLOW/FAST/CUM,
 *   BREEDING_INTRO/SLOW/FAST/CUM (variants 1-3), BREEDING_2,
 *   JUMP_1/2/3, HEAD_PAT (blink eyes controller)
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - extends e4 - extends NpcModelCodeEntity
 *   - implements ai - implements GoblinInterface
 *   - EntityDataManager - SynchedEntityData
 *   - EntityAIBase - Goal; EntityAISwimming - FloatGoal
 *   - df - NpcFollowPlayerGoal; hz - NpcOpenDoorGoal
 *   - EntityLiving.func_70105_a - entity dimensions
 *   - func_70047_e - getEyeHeight
 *   - func_184651_r - registerGoals
 *   - func_70619_bc - tick
 *   - func_70067_L - isPathFinding (custom)
 *   - func_70014_b / func_70037_a - addAdditionalSaveData / readAdditionalSaveData
 *   - func_184645_a - interactMob
 *   - func_70106_y - onRemovedFromWorld
 *   - b6.a - MathUtil.lerp; be.b - Mth.clamp
 *   - h field - BaseNpcEntity.CUSTOM_NAME_PARAM (id unknown in e4 parent)
 *   - em.ad() - BaseNpcEntity.getAllNpcs()
 *   - dy.c() - GoblinEntityRenderer.resetModelCache()
 *   - fh.b() / d3.a(bool) - TransitionScreen.show() / ClientStateManager.setFreeze(bool)
 *   - ge.b.sendToAllAround - ModNetwork multicast helpers
 *   - c.GIRLS_GOBLIN_* - ModSounds.GIRLS_GOBLIN_*
 *   - by - GoblinColor enum; g5 - HairColor enum; eh - EyeColor enum
 *   - AnimationEvent - AnimationState (GeckoLib4)
 *   - AnimationBuilder - RawAnimation
 */
public class GoblinEntity extends NpcModelCodeEntity implements GoblinInterface, GeoEntity {

    // -- Static constants -------------------------------------------------------

    /** Default goblin color. */
    public static final GoblinColor DEFAULT_COLOR = GoblinColor.DARK_GREEN;

    /** Throne reference size (unused in gameplay, kept for reference). */
    public static final Vec3 THRONE_SIZE = new Vec3(11, 6, 11);

    /** Sex position offsets per slot (0-3 directions - BJ orientation slot). */
    public static final Vec3[] SEX_OFFSETS = {
            new Vec3(5, 1, 9),  new Vec3(3, -1, 6), new Vec3(1, 1, 5),
            new Vec3(-6, -1, 3), new Vec3(5, 1, 1), new Vec3(-3, -1, -6),
            new Vec3(9, 1, 5),  new Vec3(0, -1, -4), new Vec3(1, -1, -3),
            new Vec3(-1, -1, -3), new Vec3(6, -1, -3)
    };

    // Timing constants
    static final int STAND_UP_TICKS = 37;
    static final int PICK_UP_ANGLE  = 45;
    static final int CARRY_COOLDOWN = 100;
    static final int BREEDING_TICKS = 8400;
    static final int ROB_DEFAULT    = 31520;

    // Entity dimensions
    static final Vector2f DIMS = new Vector2f(0.5F, 0.99F);

    // Items goblins can steal/hold
    static final Set<Item> STEALABLE_ITEMS = new HashSet<>(Arrays.asList(
            Items.IRON_SWORD, Items.GOLD_SWORD, Items.STONE_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_SWORD, Items.IRON_HOE, Items.STONE_HOE, Items.DIAMOND_HOE,
            Items.WOODEN_HOE, Items.BOW, Items.CROSSBOW,
            Items.GLASS_BOTTLE, Items.FLOWER_POT
    ));

    // -- Synched data -----------------------------------------------------------

    /** Carrier/owner UUID string (empty = nobody). */
    public static final EntityDataAccessor<String>    CARRIER_UUID =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    /** Queen UUID string. */
    public static final EntityDataAccessor<String>    QUEEN_UUID =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    /** Held/caught item. */
    public static final EntityDataAccessor<ItemStack> HELD_ITEM =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Is tamed / being carried. */
    public static final EntityDataAccessor<Boolean>   IS_TAMED =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);
    /** Is pregnant (queens only). */
    public static final EntityDataAccessor<Boolean>   IS_PREGNANT =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);

    // -- Instance fields --------------------------------------------------------

    /** True if this is a queen goblin. */
    public boolean isQueen = false;
    /** Throne rotation (queen only). */
    public float throneRot = 0.0F;
    /** Throne position (queen only). */
    public Vec3 thronePos = Vec3.ZERO;
    /** Impregnation countdown tick (queen, -1 = not pregnant). */
    public long impregnationTick = -1L;
    /** List of guard goblin UUIDs (queen only). */
    public List<UUID> guardIds = new ArrayList<>();

    // Movement/combat state
    public int standUpTimer = 0;
    public int pickupAngleTimer = -1;
    public int robTimer = ROB_DEFAULT;
    public int catchTimer = -1;
    public int throwTimer = -1;
    public boolean isSexActive2 = false;
    @Nullable
    public BlockPos lastKnownBedPos = null;
    public int breedingTimer = 0;
    public boolean walkingToBed = false;
    public int attackCounter = 0;
    public long lastPickupTime = 0L;
    public List<GoblinEntity> children = new ArrayList<>();
    public int frameIdx = -1;
    public int subFrame = -1;
    @Nullable
    public AnimState pendingState = null;
    public float renderScale = 1.0F;
    public boolean collidable = true;
    public boolean gravityEnabled = true;
    public boolean interactable = false;
    public String customNameStr = "";
    public boolean isThrowing = false;

    // GeckoLib
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // -- Constructor ------------------------------------------------------------

    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type, level);
    }

    /** Creates a goblin with a specific model code and color. */
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level,
                        String queenUUID, int colorIndex) {
        this(type, level);
        getEntityData().set(QUEEN_UUID, queenUUID);
        getEntityData().set(MODEL_CODE, buildModelCode(new StringBuilder(), colorIndex));
    }

    /** Creates a sitting throne queen at the given position. */
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level,
                        boolean queen, float rotation, Vec3 pos) {
        this(type, level);
        if (!queen) return;
        getEntityData().set(MODEL_CODE, buildRandomQueenCode(new StringBuilder()));
        this.throneRot  = rotation;
        this.thronePos  = pos;
        this.isQueen    = true;
        setHomePos(pos);
        setYRot(rotation);
        setAnimState(AnimState.SIT);
        setAggressive(true);
        moveTo(pos.x, pos.y, pos.z);
    }

    // -- Entity setup -----------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Random eye color for variety
        EyeColor eyeColor = EyeColor.values()[random.nextInt(EyeColor.values().length)];
        getEntityData().define(HOME_POS,    new BlockPos(eyeColor.getColorVec()));
        getEntityData().define(BODY_COLOR,  DEFAULT_COLOR.name());
        getEntityData().define(CARRIER_UUID, "");
        getEntityData().define(QUEEN_UUID,  "");
        getEntityData().define(HELD_ITEM,   ItemStack.EMPTY);
        getEntityData().define(IS_TAMED,    false);
        getEntityData().define(IS_PREGNANT, false);
    }

    @Override
    protected void registerGoals() {
        this.followPlayerGoal = new NpcFollowPlayerGoal(this, Player.class, 2.0F, 1.0F);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new NpcOpenDoorGoal(this));
        goalSelector.addGoal(5, followPlayerGoal);
    }

    // -- NpcModelCodeEntity abstracts -------------------------------------------

    @Override
    public String getDefaultName() { return "Goblin"; }

    @Override
    public float getEyeHeight(Pose pose) { return 0.75F; }

    @Override
    public float getYOffset() { return 0.1F; }

    // -- Model code generation --------------------------------------------------

    /** Random queen model code: fixed crown setting, random other params. */
    private String buildRandomQueenCode(StringBuilder sb) {
        appendRandom(sb, 3); appendRandom(sb, 2); appendRandom(sb, 2);
        appendFixed(sb, 7);  appendFixed(sb, 7);  appendRandom(sb, 5);
        appendRandom(sb, HairColor.values().length - 1);
        appendRandom(sb, GoblinColor.values().length - 1);
        appendRandom(sb, EyeColor.values().length - 1);
        appendFixed(sb, 1);
        return sb.toString();
    }

    /** Model code with a specific color index. */
    private String buildModelCode(StringBuilder sb, int colorIndex) {
        appendRandom(sb, 3); appendRandom(sb, 2); appendRandom(sb, 2);
        appendRandom(sb, 7); appendRandom(sb, 7); appendRandom(sb, 5);
        appendRandom(sb, HairColor.values().length - 1);
        appendFixed(sb, colorIndex);
        appendRandom(sb, EyeColor.values().length - 1);
        appendFixed(sb, 0);
        return sb.toString();
    }

    // -- Interaction ------------------------------------------------------------

    @Override
    public boolean mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level.isClientSide()) return true;
        if (isQueen) return true;

        // If in RUN state and player is close, start catch (BJ)
        if (getAnimState() == AnimState.RUN) {
            if (distanceTo(player) > 3.5) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("get a bit closer..."), true);
            } else {
                setHomePos(player.position());
                setYRot(player.getYRot());
                setAnimState(AnimState.CATCH);
                getEntityData().set(CUSTOM_NAME_PARAM, "bj");
                setOwnerUUID(player.getUUID());
                setPartnerUUID(player.getUUID());
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
            }
            return true;
        }

        // Normal pickup
        if (isCarriedBy(player.getUUID())) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("you are already carrying a Goblin"), true);
        } else {
            setOwnerUUID(player.getUUID());
            setAnimState(AnimState.PICK_UP);
            pickupAngleTimer = PICK_UP_ANGLE;
            setAggressive(false);
            getEntityData().set(IS_TAMED, true);
            getNavigation().stop();
        }
        return true;
    }

    // -- Tick -------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        tickCarry();
        tickStandUp();
        tickThrown();
        tickBreeding();
    }

    /** Handles STAND_UP countdown. */
    private void tickStandUp() {
        if (getAnimState() != AnimState.STAND_UP) return;
        if (++standUpTimer >= STAND_UP_TICKS) {
            standUpTimer = 0;
            setAnimState(AnimState.NULL);
        }
    }

    /** Handles THROWN landing logic. */
    private void tickThrown() {
        if (getAnimState() != AnimState.THROWN) return;
        if (!isOnGround()) return;
        int counter = getThrowDamageCounter() + 1;
        setThrowDamageCounter(counter);
        if (counter >= 3) {
            setThrowDamageCounter(0);
            setAnimState(AnimState.AWAIT_PICK_UP);
        }
    }

    /** Handles queen pregnancy impregnation countdown. */
    private void tickBreeding() {
        if (!isQueen) return;
        if (impregnationTick < 0) return;
        if (level.getGameTime() - impregnationTick < BREEDING_TICKS) return;
        impregnationTick = -1L;
        getEntityData().set(IS_PREGNANT, false);
        spawnBabyGoblin();
    }

    /** Spawns a baby goblin near the queen. */
    private void spawnBabyGoblin() {
        if (level.isClientSide()) return;
        GoblinEntity baby = new GoblinEntity(ModEntityRegistry.GOBLIN.get(), level,
                getQueenUUID() != null ? getQueenUUID().toString() : "",
                GoblinColor.values()[random.nextInt(GoblinColor.values().length)].ordinal());
        baby.moveTo(getX() + (random.nextBoolean() ? 1 : -1) * random.nextFloat(),
                    getY(), getZ() + (random.nextBoolean() ? 1 : -1) * random.nextFloat());
        level.addFreshEntity(baby);
    }

    // -- Carry helpers ----------------------------------------------------------

    private void tickCarry() {
        AnimState state = getAnimState();
        if (state != AnimState.PICK_UP && state != AnimState.SHOULDER_IDLE) return;

        UUID carrierId = getCarrierUUID();
        if (carrierId == null) {
            setAnimState(AnimState.NULL);
            return;
        }

        Player carrier = level.getPlayerByUUID(carrierId);
        if (carrier == null) {
            setAnimState(AnimState.NULL);
            return;
        }
    }

    public boolean isCarriedBy(UUID playerId) {
        return playerId.equals(getCarrierUUID());
    }

    // -- Action triggers (client-side) ------------------------------------------

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "take ur stuff back" -> setAnimState(AnimState.START_THROWING);
            case "use her"            -> startSexMode(playerId, true);
        }
    }

    public void startSexMode(UUID playerId, boolean front) {
        if (front) {
            frameIdx = 0;
            TransitionScreen.show();
            ClientStateManager.setFreeze(false);
            setPartnerUUID(playerId);
        } else {
            subFrame = 0;
            TransitionScreen.show();
            ClientStateManager.setFreeze(false);
            setPartnerUUID(playerId);
        }
    }

    @Override
    public void startFollowing(UUID playerId) {
        setOwnerUUID(playerId);
        setAnimState(AnimState.WALK);
    }

    @Override
    public void stopFollowing(UUID playerId) {
        resetSexState();
        setAnimState(AnimState.NULL);
    }

    // -- isSexState check -------------------------------------------------------

    @Override
    public boolean isPathFindingAllowed() {
        AnimState s = getAnimState();
        if (s == AnimState.THROWN)      return false;
        if (s == AnimState.RUN)         return true;
        if (s == AnimState.AWAIT_PICK_UP) return true;
        if (getCarrierUUID() != null)   return false;
        return s == AnimState.NULL;
    }

    // -- Static: check if player is carrying a goblin ---------------------------

    public static boolean isPlayerCarryingGoblin(UUID playerId) {
        if (playerId == null) return false;
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (!(npc instanceof GoblinInterface)) continue;
            if (npc.level.isClientSide()) continue;
            if (npc.isRemoved()) continue;
            UUID carrier = ((GoblinEntity) npc).getCarrierUUID();
            if (playerId.equals(carrier)) return true;
        }
        return false;
    }

    // -- NBT --------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("bodyColor",  getEntityData().get(BODY_COLOR));
        tag.putInt("eyeColorX", getEntityData().get(HOME_POS).getX());
        tag.putInt("eyeColorY", getEntityData().get(HOME_POS).getY());
        tag.putInt("eyeColorZ", getEntityData().get(HOME_POS).getZ());
        tag.putString("model",  getEntityData().get(MODEL_CODE));
        tag.putString("girlID", getEntityData().get(NPC_UUID_STRING));
        tag.putString("queen",  getEntityData().get(QUEEN_UUID));
        tag.putBoolean("isQueen", isQueen);
        tag.putBoolean("isTamed", getEntityData().get(IS_TAMED));
        tag.putInt("robTicks", robTimer);

        if (!isQueen) return;
        tag.putBoolean("preggo", getEntityData().get(IS_PREGNANT));
        tag.putFloat("throneRot", throneRot);
        tag.putDouble("thronePosX", thronePos.x);
        tag.putDouble("thronePosY", thronePos.y);
        tag.putDouble("thronePosZ", thronePos.z);
        tag.putLong("impregnationTick", impregnationTick);
        for (int i = 0; i < guardIds.size(); i++) {
            tag.putString("guard" + i, guardIds.get(i).toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("bodyColor")) getEntityData().set(BODY_COLOR, tag.getString("bodyColor"));
        if (tag.contains("eyeColorX")) {
            getEntityData().set(HOME_POS, new BlockPos(
                    tag.getInt("eyeColorX"), tag.getInt("eyeColorY"), tag.getInt("eyeColorZ")));
        }
        if (tag.contains("model"))   getEntityData().set(MODEL_CODE, tag.getString("model"));
        if (tag.contains("girlID"))  getEntityData().set(NPC_UUID_STRING, tag.getString("girlID"));
        if (tag.contains("queen"))   getEntityData().set(QUEEN_UUID, tag.getString("queen"));
        if (tag.contains("isQueen")) isQueen   = tag.getBoolean("isQueen");
        if (tag.contains("isTamed")) getEntityData().set(IS_TAMED, tag.getBoolean("isTamed"));
        if (tag.contains("robTicks")) robTimer = tag.getInt("robTicks");

        if (!isQueen) return;
        if (tag.contains("preggo")) getEntityData().set(IS_PREGNANT, tag.getBoolean("preggo"));
        if (tag.contains("throneRot")) throneRot = tag.getFloat("throneRot");
        thronePos = new Vec3(tag.getDouble("thronePosX"),
                             tag.getDouble("thronePosY"),
                             tag.getDouble("thronePosZ"));
        impregnationTick = tag.getLong("impregnationTick");
        guardIds.clear();
        for (int i = 0; tag.contains("guard" + i); i++) {
            try { guardIds.add(UUID.fromString(tag.getString("guard" + i))); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    // -- Accessors --------------------------------------------------------------

    @Nullable
    public UUID getCarrierUUID() {
        String s = getEntityData().get(CARRIER_UUID);
        if (s.isEmpty()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    public void setCarrierUUID(@Nullable UUID id) {
        getEntityData().set(CARRIER_UUID, id != null ? id.toString() : "");
    }

    @Nullable
    public UUID getQueenUUID() {
        String s = getEntityData().get(QUEEN_UUID);
        if (s.isEmpty()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    // -- GeckoLib 4 ------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var eyes    = new AnimationController<>(this, "eyes",     5, this::eyeController);
        var movement= new AnimationController<>(this, "movement", 5, this::movementController);
        var action  = new AnimationController<>(this, "action",   0, this::actionController);

        action.setSoundKeyframeHandler(e -> {
            if ("cumSound".equals(e.getKeyframeData().getSound())) {
                playNpcSound(ModSounds.GIRLS_GOBLIN_CUM);
            }
        });

        registrar.add(eyes, movement, action);
    }

    private PlayState eyeController(AnimationState<GoblinEntity> state) {
        AnimState anim = getAnimState();
        if (anim == AnimState.NULL || anim == null) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.blink"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.null"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState movementController(AnimationState<GoblinEntity> state) {
        AnimState anim = getAnimState();
        if (anim == AnimState.NULL || anim == null) {
            if (state.isMoving()) {
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.walk"));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.idle"));
            }
        } else {
            switch (anim) {
                case WALK  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.walk"));
                case RUN   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.running"));
                case FLY   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.fly"));
                default    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.null"));
            }
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<GoblinEntity> state) {
        AnimState anim = getAnimState();
        if (anim == null) return PlayState.CONTINUE;

        boolean firstPerson = level.isClientSide() &&
                net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                level.getPlayerByUUID(getCarrierUUID() != null ? getCarrierUUID() : new UUID(0,0)) ==
                        net.minecraft.client.Minecraft.getInstance().player;

        String person = firstPerson ? "first" : "third";

        switch (anim) {
            case NULL         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.null"));
            case SIT          -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.sit"));
            case SHOULDER_IDLE-> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.shoulder_idle"));
            case PICK_UP      -> state.setAndContinue(RawAnimation.begin().thenPlay(String.format("animation.goblin.pick_up_%sperson", person)));
            case AWAIT_PICK_UP-> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.await_pick_up"));
            case STAND_UP     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.stand_up"));
            case CATCH        -> {
                String suffix = "bj".equals(getEntityData().get(CUSTOM_NAME_PARAM)) ? "Bj" : "";
                state.setAndContinue(RawAnimation.begin().thenPlay(
                        String.format("animation.goblin.catch_%sperson%s", person, suffix)));
            }
            case START_THROWING -> state.setAndContinue(RawAnimation.begin().thenPlay(
                        String.format("animation.goblin.throw_%sperson", person)));
            case THROWN       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.thrown"));
            case PAIZURI_START -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_start"));
            case PAIZURI_SLOW  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_slow"));
            case PAIZURI_FAST  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast"));
            case PAIZURI_FAST2 -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast_countinues"));
            case PAIZURI_IDLE  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_idle"));
            case PAIZURI_CUM   -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_cum"));
            case NELSON_INTRO  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_intro"));
            case NELSON_SLOW   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_slow"));
            case NELSON_FAST   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_fast"));
            case NELSON_CUM    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_cum"));
            case BREEDING_1    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.breeding_intro_1"));
            case BREEDING_2    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.breeding_2"));
            case BREEDING_3    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.breeding_intro_3"));
            case BREEDING_SLOW -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.breeding_slow_1"));
            case BREEDING_FAST -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.breeding_fast_1"));
            case BREEDING_CUM  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.breeding_cum_1"));
            case JUMP          -> {
                int j = (frameIdx % 3) + 1;
                state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.jump_" + j));
            }
            default -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    // -- Vec2i helpers (mirrors e4 static methods) ------------------------------

    /** Returns the customise-screen icon position for model code segment {@code idx}. */
    public Vec2i getIconPos(int idx) {
        return switch (idx) {
            case 0 -> new Vec2i(40, 130);  case 1 -> new Vec2i(60, 130);
            case 2 -> new Vec2i(80, 130);  case 3 -> new Vec2i(100, 130);
            case 4 -> new Vec2i(120, 130); case 5 -> new Vec2i(140, 130);
            case 6 -> new Vec2i(160, 130); case 7 -> new Vec2i(180, 130);
            case 8 -> new Vec2i(200, 0);   case 9 -> new Vec2i(200, 130);
            default -> Vec2i.ZERO;
        };
    }

    /** The variant segment index that should be fixed (for customise). */
    public List<Integer> getFixedVariants() {
        return java.util.Collections.singletonList(2);
    }

    // -- Model code helpers -----------------------------------------------------

    private void appendRandom(StringBuilder sb, int max) {
        NpcModelCodeEntity.appendRandom(sb, max);
    }

    private void appendFixed(StringBuilder sb, int val) {
        NpcModelCodeEntity.appendFixed(sb, val);
    }

    private int getThrowDamageCounter() { return frameIdx < 0 ? 0 : frameIdx; }
    private void setThrowDamageCounter(int v) { frameIdx = v; }
}
