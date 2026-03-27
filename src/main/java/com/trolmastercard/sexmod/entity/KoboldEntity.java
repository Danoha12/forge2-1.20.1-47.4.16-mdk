package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.item.ModItems;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.tribe.Task;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.tribe.TribePhase;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.util.KoboldNames;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
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
 * KoboldEntity - Miembro de la Tribu.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Maneja IA de tribu, recolección de recursos, defensa de territorio
 * e interacciones sociales complejas con el jugador.
 */
public class KoboldEntity extends BaseKoboldEntity implements GeoEntity {

    // =========================================================================
    //  Parámetros de Datos Sincronizados
    // =========================================================================

    public static final EntityDataAccessor<Float> BODY_SIZE = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> KOBOLD_NAME = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_ALARMED = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_DEFENDING = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TRIBE_NAME = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_TRIBE_ATTACKING = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_MINING_TREE = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> TRIBE_ID = SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // =========================================================================
    //  Constantes y Campos de IA
    // =========================================================================

    private static final int GREETING_COOLDOWN = 300;
    private static final float GREETING_DIST = 2.0F;
    private static final float ATTACK_RANGE = 2.0F;
    private static final float ATTACK_DAMAGE = 5.0F;
    private static final int RESOLUTION_COUNTER_MAX = 132; // Antes CUM_COUNTER_MAX

    public final ItemStackHandler inventory = new ItemStackHandler(27);

    private int attackTick = 0;
    private int resolutionFrameCounter = -1;
    private long lastGreetingWorldTime = Long.MIN_VALUE;
    private float prevDistToMaster = Float.MAX_VALUE;
    private int idleAttackTimer = 0;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // =========================================================================
    //  Constructor y Atributos
    // =========================================================================

    public KoboldEntity(EntityType<? extends KoboldEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRIBE_ID, Optional.empty());
        this.entityData.define(BODY_SIZE, 0.0F);
        this.entityData.define(KOBOLD_NAME, KoboldNames.random(random));
        this.entityData.define(IS_ALARMED, false);
        this.entityData.define(IS_DEFENDING, false);
        this.entityData.define(TRIBE_NAME, "null");
        this.entityData.define(IS_TRIBE_ATTACKING, false);
        this.entityData.define(IS_MINING_TREE, false);
    }

    // =========================================================================
    //  Interacciones
    // =========================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (getInteractionTarget() != null) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);

        // Sistema de Nombres
        if (stack.getItem() instanceof NameTagItem) {
            if (isMaster(player)) {
                this.entityData.set(KOBOLD_NAME, stack.getHoverName().getString());
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        // Silbato de Tribu
        if (stack.getItem() == ModItems.WHISTLE.get()) {
            if (level().isClientSide) {
                Optional<UUID> tribeId = this.entityData.get(TRIBE_ID);
                tribeId.ifPresent(this::openTribeScreen);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Inicio de Interacción Social
        if (level().isClientSide) {
            if (isTamed() && isMaster(player)) openInteractionMenu("GIRLS_KOBOLD_MASTER");
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
    //  Lógica de Tick y IA de Tribu
    // =========================================================================

    @Override
    public void baseTick() {
        super.baseTick();
        Optional<UUID> tribeIdOpt = this.entityData.get(TRIBE_ID);

        if (tribeIdOpt.isPresent()) {
            UUID tribeId = tribeIdOpt.get();
            tickInteractionReward(tribeId);
            TribeManager.heartbeat(tribeId);
        }

        if (tickInteractionAnimation()) return;

        // Auto-curación fuera de peligro
        if (!this.entityData.get(IS_ALARMED) && getHealth() < getMaxHealth()) {
            if (tickCount % 100 == 0) {
                setHealth(getHealth() + 2.0F);
                spawnHeartParticles();
            }
        }

        if (getAnimState() == AnimState.ATTACK && tribeIdOpt.isPresent()) {
            tickAttack(tribeIdOpt.get());
            return;
        }

        // Actualización de estado de alerta
        if (tribeIdOpt.isPresent()) {
            UUID id = tribeIdOpt.get();
            boolean threatened = hasThreatNearby(id, false);
            this.entityData.set(IS_ALARMED, threatened);
            this.entityData.set(IS_TRIBE_ATTACKING, TribeManager.hasEnemies(id));
            tickTribeAI(id);
        }
    }

    private void tickAttack(UUID tribeId) {
        getNavigation().stop();
        attackTick++;

        if (attackTick == 22) playAttackSound();

        if (attackTick == 32) {
            Set<LivingEntity> enemies = TribeManager.getEnemies(tribeId);
            for (LivingEntity enemy : enemies) {
                if (distanceTo(enemy) <= ATTACK_RANGE) {
                    enemy.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE);
                }
            }
        }

        if (attackTick >= 84) {
            setAnimState(AnimState.NULL);
            this.entityData.set(FROZEN, false);
            attackTick = 0;
        }
    }

    private void tickInteractionReward(UUID tribeId) {
        if (resolutionFrameCounter == -1) return;
        if (++resolutionFrameCounter < RESOLUTION_COUNTER_MAX) return;
        resolutionFrameCounter = -1;

        if (getAnimState() == AnimState.MATING_PRESS_CUM) {
            UUID target = getInteractionTarget();
            if (target != null) {
                Player player = level().getPlayerByUUID(target);
                if (player != null) {
                    // Entrega del huevo de la tribu como recompensa de vínculo
                    ItemStack eggStack = new ItemStack(ModItems.TRIBE_EGG.get());
                    CompoundTag tag = eggStack.getOrCreateTag();
                    tag.putString("tribeID", tribeId.toString());
                    player.getInventory().add(eggStack);
                }
            }
        }
    }

    // =========================================================================
    //  Máquina de Estados de Animación (Enmascarada)
    // =========================================================================

    @Override
    public void setAnimState(AnimState newState) {
        AnimState current = getAnimState();
        // Bloqueo de interrupciones en fases finales
        if (current == AnimState.MATING_PRESS_CUM && (newState == AnimState.MATING_PRESS_SOFT || newState == AnimState.MATING_PRESS_HARD)) return;
        if (current == AnimState.KOBOLD_ANAL_CUM && (newState == AnimState.KOBOLD_ANAL_SLOW || newState == AnimState.KOBOLD_ANAL_FAST)) return;

        if (newState == AnimState.MATING_PRESS_CUM) resolutionFrameCounter = 0;
        super.setAnimState(newState);
    }

    // =========================================================================
    //  Controladores GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "eyes", 0, state -> {
                    return state.setAndContinue(getAnimState() != AnimState.NULL
                            ? RawAnimation.begin().thenLoop("animation.kobold.null")
                            : RawAnimation.begin().thenLoop("animation.kobold.blink"));
                }),
                new AnimationController<>(this, "movement", 10, this::movementPredicate),
                new AnimationController<>(this, "action", 0, this::actionPredicate)
                        .setSoundKeyframeHandler(this::handleSoundKeyframe)
        );
    }

    private PlayState movementPredicate(AnimationState<KoboldEntity> state) {
        if (getAnimState() != AnimState.NULL) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.null"));
        if (isPassenger()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.sit"));

        if (!this.entityData.get(FROZEN) && state.isMoving()) {
            if (onGround()) {
                if (isCrouching()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.crouch_walk"));
                if (this.entityData.get(IS_ALARMED)) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.run_armed"));
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.run"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.fly"));
        }

        return state.setAndContinue(this.entityData.get(IS_ALARMED) ? RawAnimation.begin().thenLoop("animation.kobold.idle_armed") : RawAnimation.begin().thenLoop("animation.kobold.idle"));
    }

    private PlayState actionPredicate(AnimationState<KoboldEntity> state) {
        // Los strings de los JSON se mantienen idénticos para no romper los modelos
        RawAnimation anim = switch (getAnimState()) {
            case ATTACK              -> RawAnimation.begin().thenPlay("animation.kobold.attack");
            case SLEEP, PAYMENT      -> RawAnimation.begin().thenLoop("animation.kobold.sit");
            case MINE                -> RawAnimation.begin().thenLoop("animation.kobold.fall_tree");
            case STARTBLOWJOB        -> RawAnimation.begin().thenPlay("animation.kobold.blowjobStart");
            case SUCKBLOWJOB_BLINK   -> RawAnimation.begin().thenLoop("animation.kobold.blowjobSlowR");
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
            default                  -> RawAnimation.begin().thenLoop("animation.kobold.null");
        };
        return state.setAndContinue(anim);
    }

    private void handleSoundKeyframe(SoundKeyframeEvent<KoboldEntity> event) {
        String key = event.getKeyframeData().getSound();
        switch (key) {
            case "attackSound" -> this.playSound(ModSounds.KOBOLD_ATTACK.get(), 1F, getPitch());
            case "giggle" -> this.playSound(ModSounds.KOBOLD_GIGGLE.get(), 1F, getPitch());
            case "moan", "orgasm", "cum" -> this.playSound(ModSounds.KOBOLD_GIGGLE.get(), 1F, getPitch()); // Redirigido a risas
            case "pounding" -> this.playSound(ModSounds.MISC_POUNDING.get(), 1F, 1F);
        }
    }

    // =========================================================================
    //  Diálogos de Combate (Enmascarados)
    // =========================================================================

    private static final String[] COMBAT_LINES = {
            "¡No podréis contra la tribu!",
            "¡Sentid el acero de los Kobolds!",
            "¡Esta tierra nos pertenece!",
            "¡Retroceded, intrusos!"
    };

    // =========================================================================
    //  Boilerplate de GeckoLib y Helpers
    // =========================================================================

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
    public String getKoboldName() { return entityData.get(KOBOLD_NAME); }
    protected void openInteractionMenu(Object soundKey) {}
    protected boolean tickInteractionAnimation() { return false; }
    protected UUID getInteractionTarget() { return null; }

    private boolean isMaster(Player player) { return player.getStringUUID().equals(getMasterUUID()); }

    public enum AnimState {
        NULL, ATTACK, SLEEP, MINE, PAYMENT, PAYMENT_ANIM, STARTBLOWJOB, SUCKBLOWJOB_BLINK,
        THRUSTBLOWJOB, CUMBLOWJOB, KOBOLD_ANAL_START, KOBOLD_ANAL_SLOW, KOBOLD_ANAL_FAST,
        KOBOLD_ANAL_CUM, MATING_PRESS_START, MATING_PRESS_SOFT, MATING_PRESS_HARD, MATING_PRESS_CUM
    }
}