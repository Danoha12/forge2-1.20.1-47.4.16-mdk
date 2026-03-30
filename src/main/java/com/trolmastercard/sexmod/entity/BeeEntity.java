package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
// import com.trolmastercard.sexmod.registry.ModEffects; // Descomentar cuando ModEffects esté porteado
// import com.trolmastercard.sexmod.network.ModNetwork; // Descomentar cuando ModNetwork esté porteado
// import com.trolmastercard.sexmod.network.CameraControlPacket; // Descomentar cuando el paquete esté porteado
import com.trolmastercard.sexmod.client.screen.BeeQuickAccessScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * BeeEntity — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * Entidad NPC voladora tipo abeja.
 * Extiende NpcInventoryBase (tiene un inventario de 27 slots).
 * Puede interactuar con un jugador cercano para iniciar secuencias animadas.
 */
public class BeeEntity extends NpcInventoryBase implements GeoEntity {

    // ── Synced data ───────────────────────────────────────────────────────────

    /** Indica si la abeja ha sido "domesticada" (inicio de interacción interactiva). ID 112. */
    public static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(BeeEntity.class, EntityDataSerializers.BOOLEAN);

    // Asumo que NpcInventoryBase define DATA_HAS_CHEST. Si no, debe agregarse allí.

    // ── Fields ────────────────────────────────────────────────────────────────

    public float seekCooldown = 3200.0F;
    int breedCounter = 0;

    static final float SEEK_INTERVAL  = 4800.0F;
    static final float SEEK_RANGE     = 10.0F;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── Constructor ───────────────────────────────────────────────────────────

    public BeeEntity(EntityType<? extends BeeEntity> type, Level level) {
        super(type, level);
    }

    // @Override // Descomentar si NpcInventoryBase define este método
    public String getNpcName() { return "Bee"; }

    // @Override // Descomentar si NpcInventoryBase define este método
    public float getNametagOffsetY() { return -0.1F; }

    // ── Synced data init ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TAMED, false);
    }

    // ── Navigation (flying) ───────────────────────────────────────────────────

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.FlyingPathNavigation nav =
                new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH,       12.0D)
                .add(Attributes.MOVEMENT_SPEED,   0.4D)
                .add(Attributes.FOLLOW_RANGE,     16.0D)
                .add(Attributes.ATTACK_DAMAGE,    0.2D)
                .add(Attributes.FLYING_SPEED,     0.4D); // Necesario en 1.20 para entidades voladoras
    }

    // ── Goals ────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // this.goalSelector.addGoal(0, new NpcBreedGoal(this, this.getOwnerUUID(), 3.0F, 1.0F)); // Descomentar cuando exista
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // this.goalSelector.addGoal(2, new NpcCombatGoal(this)); // Descomentar cuando exista
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void aiStep() {
        try { super.aiStep(); } catch (RuntimeException e) { return; }

        // Lógica del efecto (SFW: EXCITED en lugar de HORNY)
        /* Descomentar cuando ModEffects esté listo
        if (hasEffect(ModEffects.EXCITED.get())) {
            if (seekCooldown < SEEK_INTERVAL && getMasterUUID() == null) {
                removeEffect(ModEffects.EXCITED.get());
                seekCooldown = 6.942018E7F; // Bandera de consumido
            }
        }
        */

        // updateAnimStateName(); // Descomentar si existe en la clase base

        if (getAnimState() == AnimState.CITIZEN_FINISH) {
            breedCounter = Math.max(1, breedCounter);
        }

        tickSeekPlayer();
        tickParticles();
    }

    @Override
    public void setAnimStateFiltered(AnimState state) {
        // Prevenir interrupción durante la animación final
        if (getAnimState() == AnimState.CITIZEN_FINISH) {
            if (state == AnimState.CITIZEN_FAST || state == AnimState.RIDE_SLOW) return;
        }
        super.setAnimStateFiltered(state);
    }

    private void tickSeekPlayer() {
        if (getMasterUUID() != null) return;
        // if (isSitting()) return; // Descomentar si isSitting existe

        seekCooldown++;
        if (seekCooldown < SEEK_INTERVAL) return;

        Player player = this.level().getNearestPlayer(this, SEEK_RANGE);
        if (player == null) return;

        // if (getOwnerForPlayer(player) != null) return; // Descomentar cuando exista
        // if (PlayerKoboldEntity.hasNpc(player)) return; // Descomentar cuando exista

        if (distanceTo(player) < 1.5F) {
            seekCooldown = 0.0F;
            setMaster(player.getUUID().toString());
            this.entityData.set(DATA_TAMED, true);
            faceToward(player.getYRot() - 180.0F);
            getNavigation().stop();

            /* Descomentar cuando ModNetwork esté listo
            if (player instanceof ServerPlayer sp) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
            }
            */

            setAnimStateFiltered(AnimState.CITIZEN_START);
            Vec3 behind = getPositionBehind(0.2D);
            player.teleportTo(behind.x, behind.y, behind.z);
        } else {
            getNavigation().stop();
            getNavigation().moveTo(player, 1.0D);
        }
    }

    private void tickParticles() {
        if (breedCounter == 0) return;
        breedCounter++;
        boolean tamed = this.entityData.get(DATA_TAMED);

        if (tamed) {
            if (breedCounter < 40) spawnParticle(ParticleTypes.HEART);
            else breedCounter = 0;
        } else {
            if (breedCounter < 200) {
                spawnParticle(ParticleTypes.ENCHANT);
            } else if (breedCounter == 200) {
                this.entityData.set(DATA_TAMED, getRandom().nextBoolean());
            } else if (breedCounter < 250) {
                spawnParticle(this.entityData.get(DATA_TAMED) ? ParticleTypes.HEART : ParticleTypes.ANGRY_VILLAGER);
            } else {
                breedCounter = 0;
            }
        }
        spawnParticle(ParticleTypes.ENCHANT, 10);
    }

    private void spawnParticle(net.minecraft.core.particles.ParticleOptions type) {
        spawnParticle(type, 1);
    }

    private void spawnParticle(net.minecraft.core.particles.ParticleOptions type, int count) {
        if (this.level().isClientSide()) return;
        ((ServerLevel) this.level()).sendParticles(
                (SimpleParticleType) type,
                this.getX(), this.getY() + 0.3, this.getZ(),
                count, 0.2, 0.3, 0.2, 0.25);
    }

    // ── Prevent fall damage ───────────────────────────────────────────────────

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ── Fall physics dampening ────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (seekCooldown < SEEK_INTERVAL && !onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1.0, 0.4, 1.0));
        }
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean tamed = this.entityData.get(DATA_TAMED);

        // Asumiendo que DATA_HAS_CHEST se define en NpcInventoryBase. Si no, usar getPersistentData().
        // boolean hasChest = this.entityData.get(DATA_HAS_CHEST);
        boolean hasChest = false;

        if (tamed && !hasChest && player.getItemInHand(hand).is(Items.DIAMOND)) {
            // this.entityData.set(DATA_HAS_CHEST, true);
            player.getItemInHand(hand).shrink(1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (this.level().isClientSide() && tamed) {
            openChestScreen(player);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @OnlyIn(Dist.CLIENT)
    private void openChestScreen(Player player) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new BeeQuickAccessScreen(this, player));
    }

    // ── Breed callback ───────────────────────────────────────────────────────

    public boolean canStartBreedInteraction(Player player) { return false; }

    // ── AnimState chain helpers ───────────────────────────────────────────────

    // @Override // Descomentar si están en BaseNpcEntity
    protected AnimState getFollowUpAnim(AnimState current) {
        return switch (current) {
            case CITIZEN_SLOW -> AnimState.CITIZEN_FAST;
            default -> null;
        };
    }

    // @Override // Descomentar si están en BaseNpcEntity
    protected AnimState getCumTransition(AnimState current) {
        return switch (current) {
            case CITIZEN_FAST -> AnimState.CITIZEN_FINISH;
            case CITIZEN_SLOW -> AnimState.CITIZEN_FINISH;
            default -> null;
        };
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("isTamed", this.entityData.get(DATA_TAMED));
        // tag.putBoolean("hasChest", this.entityData.get(DATA_HAS_CHEST));
        // tag.put("inventory", inventoryHandler.serializeNBT()); // Descomentar cuando NpcInventoryBase esté listo
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("isTamed")) this.entityData.set(DATA_TAMED, tag.getBoolean("isTamed"));
        // this.entityData.set(DATA_HAS_CHEST, tag.getBoolean("hasChest"));
        // inventoryHandler.deserializeNBT(tag.getCompound("inventory")); // Descomentar cuando NpcInventoryBase esté listo
    }

    // ── GeckoLib 4 ───────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
        registrar.add(new AnimationController<>(this, "action",   0, this::actionPredicate));
    }

    private PlayState movementPredicate(AnimationState<BeeEntity> state) {
        if (this.level().isClientSide() && this.level().getClass().getSimpleName().contains("Fake"))
            return PlayState.STOP;

        AnimState anim = getAnimState();
        if (anim != AnimState.NULL) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.null"));
        } else {
            // boolean hasChest = this.entityData.get(DATA_HAS_CHEST);
            boolean hasChest = false;
            return state.setAndContinue(RawAnimation.begin()
                    .thenLoop("animation.bee." + (hasChest ? "idle_has_chest" : "idle")));
        }
    }

    private PlayState actionPredicate(AnimationState<BeeEntity> state) {
        AnimState anim = getAnimState();
        return switch (anim) {
            case CITIZEN_START -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_start"));
            case CITIZEN_SLOW  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_slow"));
            case CITIZEN_FAST  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_fast"));
            case CITIZEN_FINISH-> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_cum"));
            case THROW_PEARL   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.throw_pearl"));
            default            -> PlayState.CONTINUE;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private Vec3 getPositionBehind(double dist) {
        double angle = Math.toRadians(this.getYRot());
        return new Vec3(
                this.getX() + Math.sin(angle) * dist,
                this.getY(),
                this.getZ() - Math.cos(angle) * dist);
    }

    private void faceToward(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
    }

    private static BaseNpcEntity getOwnerForPlayer(Player player) {
        // Asume que BaseNpcEntity tendrá este método para buscar al NPC vinculado
        // return BaseNpcEntity.getByInteractionPartner(player.getUUID(), false);
        return null;
    }
}