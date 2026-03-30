package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.util.KoboldColorVariant; // Asumiendo este nombre
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.UUID;

/**
 * KoboldEggEntity — Portado a 1.20.1 / GeckoLib 4.
 * * Representa el huevo de un Kobold en gestación.
 * * Se balancea progresivamente más rápido hasta que eclosiona tras 12,000 ticks.
 */
public class KoboldEggEntity extends LivingEntity implements GeoEntity {

    public static final int MAX_AGE = 12000;

    public static final EntityDataAccessor<String> EGG_COLOR =
            SynchedEntityData.defineId(KoboldEggEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> EGG_AGE =
            SynchedEntityData.defineId(KoboldEggEntity.class, EntityDataSerializers.INT);

    private UUID tribeId = null;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public KoboldEggEntity(EntityType<? extends KoboldEggEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EGG_COLOR, "purple"); // Color base
        this.entityData.define(EGG_AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();

        int age = this.entityData.get(EGG_AGE);

        if (!this.level().isClientSide()) {
            this.entityData.set(EGG_AGE, age + 1);
            if (age >= MAX_AGE) {
                this.hatch();
            }
        }
    }

    // ── Lógica de Eclosión ────────────────────────────────────────────────────

    private void hatch() {
        // Partículas de éxito (Poof)
        if (this.level().isClientSide()) {
            for (int i = 0; i < 20; i++) {
                this.level().addParticle(ParticleTypes.POOF, this.getX(), this.getY() + 0.3, this.getZ(),
                        random.nextGaussian() * 0.02D, random.nextGaussian() * 0.02D, random.nextGaussian() * 0.02D);
            }
            return;
        }

        if (tribeId == null) tribeId = UUID.randomUUID();

        // Crear al nuevo miembro de la tribu
        KoboldEntity kobold = new KoboldEntity(this.level()); // Asumiendo constructor con level
        kobold.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);

        // Heredar propiedades de la tribu mediante el TribeManager
        TribeManager.initializeNewMember(tribeId, kobold);

        this.level().addFreshEntity(kobold);
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 1.2F);

        this.notifyTribeOwner(kobold);
        this.discard();
    }

    private void notifyTribeOwner(KoboldEntity kobold) {
        Player owner = kobold.getTribeOwner();
        if (owner instanceof ServerPlayer sp) {
            Component msg = Component.translatable("mod.tribe.new_member", kobold.getDisplayName())
                    .withStyle(ChatFormatting.GOLD);
            sp.sendSystemMessage(msg);

            // Sonidos de recompensa
            sp.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
    }

    // ── Daño y Persistencia ───────────────────────────────────────────────────

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            this.discard();
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (tribeId != null) nbt.putUUID("TribeId", tribeId);
        nbt.putInt("EggAge", this.entityData.get(EGG_AGE));
        nbt.putString("EggColor", this.entityData.get(EGG_COLOR));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.hasUUID("TribeId")) this.tribeId = nbt.getUUID("TribeId");
        this.entityData.set(EGG_AGE, nbt.getInt("EggAge"));
        this.entityData.set(EGG_COLOR, nbt.getString("EggColor"));
    }

    // ── Boilerplate de LivingEntity ──────────────────────────────────────────

    @Override
    public Iterable<ItemStack> getArmorSlots() { return Collections.emptyList(); }
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "hatch_controller", 5, state -> {
            int age = this.entityData.get(EGG_AGE);
            float progress = (float) age / MAX_AGE;

            if (MAX_AGE - age < 25) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.egg.hatch"));
            }
            if (progress > 0.95f) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.egg.rock_extreme"));
            if (progress > 0.80f) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.egg.rock_fast"));
            if (progress > 0.60f) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.egg.rock_medium"));
            if (progress > 0.40f) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.egg.rock_slow"));

            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.egg.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}