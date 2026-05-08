package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.ModEntities;
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
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * KoboldEgg — Portado a 1.20.1 / GeckoLib 4.
 */
public class KoboldEgg extends Mob implements GeoEntity {

  public static int HATCH_TICKS = 8400;

  private static final EntityDataAccessor<Integer> DATA_SIZE =
          SynchedEntityData.defineId(KoboldEgg.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DATA_AGE_TICKS =
          SynchedEntityData.defineId(KoboldEgg.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<String> DATA_BODY_COLOR =
          SynchedEntityData.defineId(KoboldEgg.class, EntityDataSerializers.STRING);

  public float squish;
  public float squishTarget;
  public float squishOld;
  private boolean wasOnGround;
  private UUID tribeId;

  public static final List<KoboldEgg> clientEggs = new ArrayList<>();

  // ── Caché de GeckoLib 4 ──────────────────────────────────────────────────
  private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

  public KoboldEgg(EntityType<? extends KoboldEgg> type, Level level) {
    super(type, level);
    this.moveControl = new EggMoveControl(this);
    if (level.isClientSide()) clientEggs.add(this);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(DATA_SIZE, 1);
    this.entityData.define(DATA_AGE_TICKS, 0);
    this.entityData.define(DATA_BODY_COLOR, "default");
  }

  // ── Conectores para el Item ──────────────────────────────────────────────

  public void setBodyColor(String color) { this.entityData.set(DATA_BODY_COLOR, color); }
  public String getBodyColor() { return this.entityData.get(DATA_BODY_COLOR); }
  public void setTribeId(UUID uuid) { this.tribeId = uuid; }
  public UUID getTribeId() { return this.tribeId; }

  // ─────────────────────────────────────────────────────────────────────────

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(1, new EggFloatGoal(this));
    this.goalSelector.addGoal(5, new EggHopGoal(this));
  }

  public void setSize(int size, boolean resetHealth) {
    this.entityData.set(DATA_SIZE, Mth.clamp(size, 1, 127));
    this.refreshDimensions();
    this.reapplyPosition();
    this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(size * size);
    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2F + 0.1F * size);
    if (resetHealth) this.setHealth(this.getMaxHealth());
  }

  public int getSize() { return this.entityData.get(DATA_SIZE); }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    float s = 0.52F * getSize();
    return EntityDimensions.scalable(s, s);
  }

  @Override
  public void tick() {
    int age = this.entityData.get(DATA_AGE_TICKS) + 1;
    this.entityData.set(DATA_AGE_TICKS, age);

    if (this.level().isClientSide()) {
      this.spawnGrowthParticles(age);
    } else if (age > HATCH_TICKS) {
      this.hatch();
      return;
    }

    this.squishOld = this.squish;
    this.squish += (this.squishTarget - this.squish) * 0.5F;

    super.tick();

    if (this.onGround() && !this.wasOnGround) {
      this.handleLanding();
    } else if (!this.onGround() && this.wasOnGround) {
      this.squishTarget = 1.0F;
    }

    this.wasOnGround = this.onGround();
    this.squishTarget *= 0.6F;
  }

  private void hatch() {
    KoboldEntity kobold = ModEntities.KOBOLD.get().create(this.level());
    if (kobold != null) {
      kobold.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
      this.level().addFreshEntity(kobold);
      this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.0F);
    }
    this.discard();
  }

  private void handleLanding() {
    int size = this.getSize();
    for (int i = 0; i < size * 8; i++) {
      float angle = this.getRandom().nextFloat() * ((float)Math.PI * 2F);
      float radius = this.getRandom().nextFloat() * 0.5F + 0.5F;
      this.level().addParticle(ParticleTypes.ITEM_SLIME,
              this.getX() + Mth.sin(angle) * size * 0.5F * radius,
              this.getBoundingBox().minY,
              this.getZ() + Mth.cos(angle) * size * 0.5F * radius,
              0.0, 0.0, 0.0);
    }
    this.playSound(this.getJumpSound(), this.getSoundVolume(), this.getVoicePitch());
    this.squishTarget = -0.5F;
  }

  private void spawnGrowthParticles(int age) {
    if (age > HATCH_TICKS * 0.95) {
      this.spawnParticle(ParticleTypes.CLOUD, 1);
    } else if (age > HATCH_TICKS * 0.7 && this.tickCount % 10 == 0) {
      this.spawnParticle(ParticleTypes.HAPPY_VILLAGER, 1);
    }
  }

  private void spawnParticle(ParticleOptions type, int count) {
    for (int i = 0; i < count; i++) {
      this.level().addParticle(type,
              this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
              0.0, 0.0, 0.0);
    }
  }

  @Override
  public void die(DamageSource cause) {
    int size = this.getSize();
    if (!this.level().isClientSide() && size > 1 && this.getHealth() <= 0.0F) {
      int splitCount = 2 + this.getRandom().nextInt(3);
      for (int i = 0; i < splitCount; i++) {

        // ¡El truco de fábrica! En lugar de 'new', le pedimos al motor que cree una copia de nuestro propio tipo
        KoboldEgg child = (KoboldEgg) this.getType().create(this.level());

        if (child != null) {
          child.setSize(size / 2, true);
          child.setBodyColor(this.getBodyColor());
          child.setTribeId(this.getTribeId());
          child.moveTo(this.getX(), this.getY() + 0.5, this.getZ(), this.getRandom().nextFloat() * 360.0F, 0.0F);
          this.level().addFreshEntity(child);
        }
      }
    }
    super.die(cause);
  }

  @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT; }
  @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH; }
  protected SoundEvent getJumpSound() { return SoundEvents.SLIME_JUMP; }

  // ── Persistencia (NBT) ───────────────────────────────────────────────────

  @Override
  public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);
    nbt.putInt("Size", this.getSize() - 1);
    nbt.putInt("AgeTicks", this.entityData.get(DATA_AGE_TICKS));
    nbt.putString("BodyColor", this.getBodyColor());
    if (this.tribeId != null) {
      nbt.putUUID("TribeID", this.tribeId);
    }
  }

  @Override
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);
    this.setSize(nbt.getInt("Size") + 1, false);
    this.entityData.set(DATA_AGE_TICKS, nbt.getInt("AgeTicks"));
    if (nbt.contains("BodyColor")) {
      this.setBodyColor(nbt.getString("BodyColor"));
    }
    if (nbt.hasUUID("TribeID")) {
      this.tribeId = nbt.getUUID("TribeID");
    }
  }

  // ── Implementación Obligatoria GeckoLib 4 ────────────────────────────────

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // Como el huevo se mueve por código, no necesitamos registrar controladores de animación
  }

  @Override
  public AnimatableInstanceCache getAnimatableInstanceCache() {
    return this.cache;
  }

  // ── Clases Internas de IA ───────────────────────────────────────────────

  static class EggMoveControl extends MoveControl {
    private float yRotTarget;
    private int jumpDelay;
    private final KoboldEgg egg;

    public EggMoveControl(KoboldEgg egg) {
      super(egg);
      this.egg = egg;
      this.yRotTarget = 180.0F * egg.getYRot() / (float)Math.PI;
    }

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
        if (this.jumpDelay-- <= 0) {
          this.jumpDelay = egg.getRandom().nextInt(100) + 50;
          this.egg.getJumpControl().jump();
        }
      }
    }

    public void setDirection(float yRot) { this.yRotTarget = yRot; }
  }

  static class EggHopGoal extends Goal {
    private final KoboldEgg egg;
    private int timer;

    public EggHopGoal(KoboldEgg egg) {
      this.egg = egg;
      this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() { return egg.onGround() || egg.isInWater(); }

    @Override
    public void tick() {
      if (--this.timer <= 0) {
        this.timer = 40 + egg.getRandom().nextInt(60);
        ((EggMoveControl)egg.getMoveControl()).setDirection(egg.getRandom().nextInt(360));
      }
    }
  }

  static class EggFloatGoal extends Goal {
    private final KoboldEgg egg;

    public EggFloatGoal(KoboldEgg egg) {
      this.egg = egg;
      this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
      ((GroundPathNavigation)egg.getNavigation()).setCanFloat(true);
    }

    @Override public boolean canUse() { return egg.isInWater() || egg.isInLava(); }

    @Override
    public void tick() {
      if (egg.getRandom().nextFloat() < 0.8F) egg.getJumpControl().jump();
    }
  }
}