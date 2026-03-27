package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.ModEntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * KoboldEgg - Huevo de la Tribu.
 * Portado a 1.20.1.
 * * Entidad similar a un Slime que crece con el tiempo.
 * * Al llegar a su edad máxima, eclosiona y spawnea un KoboldEntity.
 * * Si es destruido, se divide en fragmentos menores.
 */
public class KoboldEgg extends Mob {

    // =========================================================================
    //  Temporizador de Eclosión
    // =========================================================================

    /** Ticks necesarios para eclosionar (7 minutos aprox). */
    public static int HATCH_TICKS = 8400;

    // =========================================================================
    //  Datos Sincronizados
    // =========================================================================

    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(KoboldEgg.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_AGE_TICKS =
            SynchedEntityData.defineId(KoboldEgg.class, EntityDataSerializers.INT);

    // =========================================================================
    //  Estado de Animación (Físicas de rebote)
    // =========================================================================

    public float squish;
    public float squishTarget;
    public float squishOld;
    private boolean wasOnGround;

    /** Registro global de huevos para búsqueda rápida en cliente. */
    public static List<KoboldEgg> clientEggs = new ArrayList<>();

    // =========================================================================
    //  Constructor y Atributos
    // =========================================================================

    public KoboldEgg(EntityType<? extends KoboldEgg> type, Level level) {
        super(type, level);
        this.moveControl = new EggMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_SIZE, 1);
        entityData.define(DATA_AGE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new EggFloatGoal(this));
        this.goalSelector.addGoal(5, new EggHopGoal(this));
    }

    // =========================================================================
    //  Gestión de Tamaño y Dimensiones
    // =========================================================================

    public void setSize(int sizeTier, boolean resetHP) {
        this.entityData.set(DATA_SIZE, sizeTier);
        this.refreshDimensions();
        this.setPos(getX(), getY(), getZ());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)(sizeTier * sizeTier));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2F + 0.1F * sizeTier);
        if (resetHP) setHealth(getMaxHealth());
        this.xpReward = sizeTier;
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean isSmallEgg() {
        return getSize() <= 1;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = 0.51000005F * getSize();
        return EntityDimensions.scalable(s, s);
    }

    // =========================================================================
    //  Lógica de Tick (Crecimiento y Eclosión)
    // =========================================================================

    @Override
    public void tick() {
        int age = this.entityData.get(DATA_AGE_TICKS) + 1;
        this.entityData.set(DATA_AGE_TICKS, age);

        if (level().isClientSide()) {
            if (age > HATCH_TICKS * 0.95) {
                spawnParticle(ParticleTypes.CLOUD);
            } else if (age > HATCH_TICKS * 0.7 && tickCount % 10 == 0) {
                spawnParticle(ParticleTypes.HAPPY_VILLAGER);
            }
        } else {
            // Lógica de eclosión
            if (age > HATCH_TICKS) {
                // Spawneamos el Kobold final (Asegúrate de que este registro exista)
                KoboldEntity kobold = new KoboldEntity(ModEntityRegistry.KOBOLD_ENTITY.get(), level());
                kobold.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                level().addFreshEntity(kobold);

                this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.0F);
                this.discard();
                return;
            }
        }

        // Animación de "Squish" heredada de Slime
        squishOld = squish;
        squish += (squishTarget - squish) * 0.5F;

        super.tick();

        if (onGround() && !wasOnGround) {
            int size = getSize();
            if (!isNoParticles()) {
                for (int i = 0; i < size * 8; i++) {
                    float angle = random.nextFloat() * Mth.TWO_PI;
                    float radius = random.nextFloat() * 0.5F + 0.5F;
                    float dx = Mth.sin(angle) * size * 0.5F * radius;
                    float dz = Mth.cos(angle) * size * 0.5F * radius;
                    level().addParticle(getParticleType(), getX() + dx, getBoundingBox().minY, getZ() + dz, 0.0, 0.0, 0.0);
                }
            }
            playSound(getJumpSound(), getSoundVolume(), getSoundPitch());
            squishTarget = -0.5F;
        } else if (!onGround() && wasOnGround) {
            squishTarget = 1.0F;
        }

        wasOnGround = onGround();
        decaySquish();
    }

    protected void decaySquish() {
        squishTarget *= 0.6F;
    }

    // =========================================================================
    //  Muerte y División
    // =========================================================================

    @Override
    public void die(DamageSource cause) {
        int size = getSize();
        if (!level().isClientSide() && size > 1 && getHealth() <= 0.0F) {
            int splitCount = 2 + random.nextInt(3);
            for (int i = 0; i < splitCount; i++) {
                float ox = ((i % 2) - 0.5F) * size / 4.0F;
                float oz = ((i / 2) - 0.5F) * size / 4.0F;
                KoboldEgg child = new KoboldEgg(ModEntityRegistry.KOBOLD_EGG.get(), level());
                if (hasCustomName()) child.setCustomName(getCustomName());
                child.setSize(size / 2, true);
                child.moveTo(getX() + ox, getY() + 0.5, getZ() + oz, random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(child);
            }
        }
        super.die(cause);
    }

    // =========================================================================
    //  Persistencia NBT
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getSize() - 1);
        tag.putBoolean("wasOnGround", wasOnGround);
        tag.putInt("ageInTicks", this.entityData.get(DATA_AGE_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int size = tag.getInt("Size");
        setSize(Math.max(1, size + 1), false);
        wasOnGround = tag.getBoolean("wasOnGround");
        this.entityData.set(DATA_AGE_TICKS, tag.getInt("ageInTicks"));
    }

    // =========================================================================
    //  Sonidos y Partículas
    // =========================================================================

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isSmallEgg() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isSmallEgg() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
    }

    protected SoundEvent getJumpSound() {
        return isSmallEgg() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
    }

    @Nullable
    @Override
    protected Item getDropItem() {
        return (getSize() == 1) ? Items.SLIME_BALL : null;
    }

    protected ParticleOptions getParticleType() {
        return ParticleTypes.SLIME;
    }

    private void spawnParticle(ParticleOptions type) {
        level().addParticle(type,
                getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
                getY() + 0.15 + random.nextFloat() * getBbHeight(),
                getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
                0, 0, 0);
    }

    // =========================================================================
    //  IA de Movimiento (Clases Internas)
    // =========================================================================

    static class EggFloatGoal extends Goal {
        private final KoboldEgg egg;
        EggFloatGoal(KoboldEgg egg) {
            this.egg = egg;
            setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
            ((GroundPathNavigation) egg.getNavigation()).setCanFloat(true);
        }
        @Override
        public boolean canUse() { return egg.isInWater() || egg.isInLava(); }
        @Override
        public void tick() {
            if (egg.getRandom().nextFloat() < 0.8F) egg.getJumpControl().jump();
            ((EggMoveControl) egg.getMoveControl()).setWantedMovement(1.2);
        }
    }

    static class EggHopGoal extends Goal {
        private final KoboldEgg egg;
        private float targetAngle;
        private int countdown;
        EggHopGoal(KoboldEgg egg) { this.egg = egg; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override
        public boolean canUse() { return egg.getTarget() == null && (egg.onGround() || egg.isInWater()); }
        @Override
        public void tick() {
            if (--countdown <= 0) {
                countdown = 40 + egg.getRandom().nextInt(60);
                targetAngle = egg.getRandom().nextInt(360);
            }
            ((EggMoveControl) egg.getMoveControl()).setDirection(targetAngle);
        }
    }

    static class EggMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private float yRotTarget;
        private int jumpDelay;
        private final KoboldEgg egg;

        EggMoveControl(KoboldEgg egg) {
            super(egg);
            this.egg = egg;
            this.yRotTarget = 180.0F * egg.getYRot() / (float)Math.PI;
        }

        public void setDirection(float yRot) { this.yRotTarget = yRot; }
        public void setWantedMovement(double speed) { this.speedModifier = speed; this.operation = Operation.MOVE_TO; }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRotTarget, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
                return;
            }

            this.operation = Operation.WAIT;
            if (this.mob.onGround()) {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (--jumpDelay <= 0) {
                    jumpDelay = egg.random.nextInt(100) + 50;
                    this.egg.getJumpControl().jump();
                } else {
                    this.egg.xxa = 0.0F;
                    this.egg.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }
}