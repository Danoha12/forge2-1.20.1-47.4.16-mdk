package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.NpcInventoryBase;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entidad NPC Abeja Voladora (BeeEntity).
 * Portado a 1.20.1 / GeckoLib 4.
 * * Puede iniciar secuencias de interacción con un jugador cercano.
 * * Extiende NpcInventoryBase (tiene un inventario de 27 ranuras).
 */
public class BeeEntity extends NpcInventoryBase implements GeoEntity {

    // -- Datos Sincronizados ---------------------------------------------------

    /** Indica si la Abeja ha forjado un vínculo con el jugador. ID 112. */
    public static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(BeeEntity.class, EntityDataSerializers.BOOLEAN);

    // -- Campos ----------------------------------------------------------------

    /** Cuenta regresiva para el próximo intento de "buscar jugador". */
    public float seekCooldown = 3200.0F;

    /** Contador de partículas y de secuencia de vínculo. */
    int breedCounter = 0;

    static final float SEEK_INTERVAL  = 4800.0F;
    static final float SEEK_RANGE     = 10.0F;

    // -- Máquina de estados de animación (GeckoLib 4) --------------------------
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // -- Constructor -----------------------------------------------------------

    public BeeEntity(EntityType<? extends BeeEntity> type, Level world) {
        super(type, world);
        this.setBbWidth(0.3F);
        this.setBbHeight(1.5F);
    }

    @Override
    public String getNpcName() { return "Bee"; }

    @Override
    public float getNametagOffsetY() { return -0.1F; }

    // -- Inicialización de Datos Sincronizados ---------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TAMED, false);
    }

    // -- Navegación (Vuelo) ----------------------------------------------------

    @Override
    protected net.minecraft.world.level.pathfinder.PathNavigation createNavigation(Level world) {
        net.minecraft.world.entity.ai.navigation.FlyingPathNavigation nav =
                new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, world);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    // -- Atributos -------------------------------------------------------------

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.LivingEntity.createLivingAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,       12.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,   0.4D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE,     16.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,    0.2D);
    }

    // -- Metas (Goals) ---------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new NpcBreedGoal(this, this.owner, 3.0F, 1.0F));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new NpcCombatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
    }

    // -- Ticks de Lógica -------------------------------------------------------

    @Override
    public void aiStep() {
        try { super.aiStep(); } catch (RuntimeException e) { return; }

        // Reducción de cooldown si tiene el efecto mágico de atracción
        if (hasEffect(ModEffects.HORNY.get())) {
            if (seekCooldown < SEEK_INTERVAL && getOwnerUUID() == null) {
                removeEffect(ModEffects.HORNY.get());
                seekCooldown = 6.942018E7F; // Flag: efecto ya consumido
            }
        }

        updateAnimStateName();
        if (getAnimState() == AnimState.CITIZEN_CUM) breedCounter = Math.max(1, breedCounter);

        tickSeekPlayer();
        tickParticles();
    }

    @Override
    public void setAnimState(AnimState state) {
        // Bloqueo: no se puede interrumpir la resolución de interacción con otra variante temprana
        if (getAnimState() == AnimState.CITIZEN_CUM) {
            if (state == AnimState.CITIZEN_FAST || state == AnimState.COWGIRLSLOW) return;
        }
        super.setAnimState(state);
    }

    private void tickSeekPlayer() {
        if (getOwnerUUID() != null) return;
        if (isSitting()) return;

        seekCooldown++;
        if (seekCooldown < SEEK_INTERVAL) return;

        Player player = this.level().getNearestPlayer(this, SEEK_RANGE);
        if (player == null) return;
        if (getOwnerForPlayer(player) != null) return;
        if (PlayerKoboldEntity.hasNpc(player)) return;

        if (distanceTo(player) < 1.5F) {
            seekCooldown = 0.0F;
            setOwnerUUID(player.getUUID());
            this.entityData.set(DATA_TAMED, true);
            faceToward(player.getYRot() - 180.0F);
            getNavigation().stop();
            if (player instanceof ServerPlayer sp) {
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sp),
                        new CameraControlPacket(false));
            }
            setAnimState(AnimState.CITIZEN_START);
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
            if (breedCounter < 40) {
                spawnParticle(net.minecraft.core.particles.ParticleTypes.HEART);
            } else {
                breedCounter = 0;
            }
        } else {
            if (breedCounter < 200) {
                spawnParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT);
            } else if (breedCounter == 200) {
                this.entityData.set(DATA_TAMED, getRandom().nextBoolean());
            } else if (breedCounter < 250) {
                boolean success = this.entityData.get(DATA_TAMED);
                spawnParticle(success
                        ? net.minecraft.core.particles.ParticleTypes.HEART
                        : net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER);
            } else {
                breedCounter = 0;
            }
        }
        spawnParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, 10);
    }

    private void spawnParticle(net.minecraft.core.particles.ParticleType<?> type) {
        spawnParticle(type, 1);
    }

    private void spawnParticle(net.minecraft.core.particles.ParticleType<?> type, int count) {
        if (this.level().isClientSide()) return;
        ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                (net.minecraft.core.particles.SimpleParticleType) type,
                true,
                this.getX(), this.getY() + 0.3, this.getZ(),
                count, 0.2, 0.3, 0.2, 0.25);
    }

    // -- Prevención de daño por caída ------------------------------------------

    @Override
    public void causeFallDamage(float fallDistance, float multiplier,
                                net.minecraft.world.damagesource.DamageSource source) {}

    // -- Amortiguación de físicas de caída -------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (seekCooldown < SEEK_INTERVAL && !isOnGround()
                && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.4, 1));
        }
    }

    // -- Interacción de Usuario ------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean tamed = this.entityData.get(DATA_TAMED);
        boolean hasChest = this.entityData.get(DATA_HAS_CHEST);

        // Otorga el cofre/inventario si está domesticada y el jugador sostiene un diamante
        if (tamed && !hasChest && player.getItemInHand(hand).is(Items.DIAMOND)) {
            this.entityData.set(DATA_HAS_CHEST, true);
            player.getItemInHand(hand).shrink(1);
            return super.mobInteract(player, hand);
        }

        if (this.level().isClientSide() && tamed) {
            openChestScreen(player);
        }

        return super.mobInteract(player, hand);
    }

    @OnlyIn(Dist.CLIENT)
    private void openChestScreen(Player player) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new BeeQuickAccessScreen(this, player));
    }

    // -- Callbacks de Vínculo -------------------------------------------------

    public boolean canStartBreedInteraction(Player player) { return false; }

    // -- Secuencia de Animaciones ----------------------------------------------

    @Override
    protected AnimState getFollowUpAnim(AnimState current) {
        return switch (current) {
            case CITIZEN_SLOW -> AnimState.CITIZEN_FAST;
            default -> null;
        };
    }

    @Override
    protected AnimState getCumTransition(AnimState current) {
        return switch (current) {
            case CITIZEN_FAST -> AnimState.CITIZEN_CUM;
            case CITIZEN_SLOW -> AnimState.CITIZEN_CUM;
            default -> null;
        };
    }

    // -- Guardado (Persistencia) -----------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("isTamed",  this.entityData.get(DATA_TAMED));
        tag.putBoolean("hasChest", this.entityData.get(DATA_HAS_CHEST));
        tag.put("inventory", inventoryHandler.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("isTamed")) this.entityData.set(DATA_TAMED, tag.getBoolean("isTamed"));
        this.entityData.set(DATA_HAS_CHEST, tag.getBoolean("hasChest"));
        inventoryHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    // -- Controladores GeckoLib 4 ----------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
        registrar.add(new AnimationController<>(this, "action",   0, this::actionPredicate));
    }

    private PlayState movementPredicate(AnimationState<BeeEntity> state) {
        if (this.level() instanceof net.minecraft.client.multiplayer.ClientLevel &&
                this.level().getClass().getSimpleName().contains("Fake")) return PlayState.STOP;
        AnimState anim = getAnimState();
        if (anim != AnimState.NULL) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bee.null"));
        } else {
            boolean hasChest = this.entityData.get(DATA_HAS_CHEST);
            state.getController().setAnimation(RawAnimation.begin()
                    .thenLoop("animation.bee." + (hasChest ? "idle_has_chest" : "idle")));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionPredicate(AnimationState<BeeEntity> state) {
        AnimState anim = getAnimState();
        return switch (anim) {
            case CITIZEN_START -> {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.bee.sex_start"));
                yield PlayState.CONTINUE;
            }
            case CITIZEN_SLOW -> {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bee.sex_slow"));
                yield PlayState.CONTINUE;
            }
            case CITIZEN_FAST -> {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bee.sex_fast"));
                yield PlayState.CONTINUE;
            }
            case CITIZEN_CUM -> {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.bee.sex_cum"));
                yield PlayState.CONTINUE;
            }
            case THROW_PEARL -> {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bee.throw_pearl"));
                yield PlayState.CONTINUE;
            }
            default -> PlayState.CONTINUE;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    // -- Helpers ---------------------------------------------------------------

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
        return BaseNpcEntity.getByOwnerUUID(player.getUUID());
    }
}