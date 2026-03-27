package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.UUID;

/**
 * KoboldEggEntity - ported from i.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A living entity (non-mob) representing a kobold egg. After {@code MAX_AGE}
 * ticks it hatches by spawning a KoboldEntity and removing itself.
 *
 * Data parameters:
 *   - EGG_COLOR  (String)  - color name matching KoboldColorVariant
 *   - EGG_AGE    (Integer) - ticks alive (0 - MAX_AGE)
 *
 * Animation states (driven by eggAge / MAX_AGE progress):
 *   0.0-0.5   - idle (no animation)
 *   0.5-0.75  - slow rocking
 *   0.75-0.85 - medium rocking
 *   0.85-0.98 - fast rocking
 *   -0.98     - very fast rocking
 *   last 20t  - hatching
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityLivingBase - LivingEntity; EntityDataManager - SynchedEntityData
 *   - DataSerializers.field_187194_d - EntityDataSerializers.STRING
 *   - DataSerializers.field_187192_b - EntityDataSerializers.INT
 *   - func_70088_a() - defineSynchedData()
 *   - func_70071_h_() - tick()
 *   - func_70106_y() - discard()
 *   - func_70097_a(DamageSource, float) - hurt(DamageSource, float)
 *   - func_70014_b / func_70037_a - addAdditionalSaveData / readAdditionalSaveData
 *   - AnimatedGeoModel - GeoModel; IAnimatable - GeoEntity; AnimationFactory - AnimatableInstanceCache
 *   - AnimationBuilder.addAnimation - RawAnimation.begin().thenLoop/.thenPlay
 *   - AnimationEvent - AnimationState
 *   - world.func_175688_a(EnumParticleTypes.EXPLOSION_NORMAL,...) - level.addParticle(ParticleTypes.POOF,...)
 *   - world.func_72838_d - level.addFreshEntity
 *   - SoundEvents.field_187539_bB - SoundEvents.GENERIC_EXPLODE
 *   - SoundEvents.field_187734_u - SoundEvents.PLAYER_LEVELUP
 *   - SoundEvents.field_187640_br - SoundEvents.ITEM_PICKUP
 *   - EnumHandSide.LEFT - HumanoidArm.LEFT
 *   - ItemStack.field_190927_a - ItemStack.EMPTY
 *   - EntityEquipmentSlot - EquipmentSlot
 *   - ff - KoboldEntity; ax - TribeManager; em - BaseNpcEntity
 *   - r.f.nextBoolean/nextFloat - random.nextBoolean/nextFloat
 */
public class KoboldEggEntity extends LivingEntity implements GeoEntity {

    // -- Constants --------------------------------------------------------------
    static final int MAX_AGE = 12000;

    // -- Synched data -----------------------------------------------------------
    public static final EntityDataAccessor<String>  EGG_COLOR =
            SynchedEntityData.defineId(KoboldEggEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> EGG_AGE   =
            SynchedEntityData.defineId(KoboldEggEntity.class, EntityDataSerializers.INT);

    // -- Instance state ---------------------------------------------------------
    /** UUID of the tribe this egg belongs to. */
    public UUID tribeId = null;

    // -- GeckoLib 4 ------------------------------------------------------------
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    public static AnimationController<KoboldEggEntity> sharedController;

    // -- Constructors -----------------------------------------------------------

    public KoboldEggEntity(EntityType<? extends KoboldEggEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.5F, 0.5F);
    }

    // -- Synched data -----------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EGG_COLOR, KoboldEntity.DEFAULT_COLOR_NAME);
        this.entityData.define(EGG_AGE,   0);
    }

    // -- Tick -------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        int age = this.entityData.get(EGG_AGE);
        if (age >= MAX_AGE) {
            hatch();
        }
        if (!this.level.isClientSide()) {
            this.entityData.set(EGG_AGE, age + 1);
        }
    }

    // -- Damage -----------------------------------------------------------------

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (!damaged) return false;
        discard();
        return true;
    }

    // -- Hatch ------------------------------------------------------------------

    /**
     * Called when the egg's age reaches MAX_AGE.
     * Spawns explosion particles, then (server-side) hatches a KoboldEntity.
     */
    private void hatch() {
        // Particle burst (both sides)
        for (int i = 0; i < 30; i++) {
            float sx = (random.nextBoolean() ? 1 : -1) * random.nextFloat();
            float sy = (random.nextBoolean() ? 1 : -1) * random.nextFloat();
            float sz = (random.nextBoolean() ? 1 : -1) * random.nextFloat();
            this.level.addParticle(ParticleTypes.POOF,
                    getX() + 0.5, getY() + 0.5, getZ() + 0.5,
                    sx, sy, sz);
        }

        if (this.level.isClientSide()) return;

        // Ensure tribe ID exists
        if (tribeId == null) tribeId = UUID.randomUUID();

        // Create / get the kobold for this tribe
        KoboldEntity kobold = KoboldEntity.getOrCreate(this.level, tribeId);
        TribeManager.setOwnerOf(tribeId, kobold);

        // Inherit partner UUID if one is assigned
        UUID partnerId = TribeManager.getPartnerOf(tribeId);
        if (partnerId != null) {
            kobold.getEntityData().set(KoboldEntity.OWNER_UUID_STRING, partnerId.toString());
        }

        // Copy skin from an existing tribe member that has one
        String inheritedSkin = null;
        for (KoboldEntity existing : KoboldEntity.getKoboldsInTribe(tribeId)) {
            String skin = existing.getEntityData().get(KoboldEntity.SKIN_UUID);
            if (!skin.isEmpty()) {
                inheritedSkin = skin;
                break;
            }
        }
        if (inheritedSkin != null) {
            kobold.getEntityData().set(KoboldEntity.SKIN_UUID, inheritedSkin);
        }

        // Spawn at egg position
        kobold.moveTo(getX() + 0.5, getY(), getZ() + 0.5);
        this.level.addFreshEntity(kobold);

        // Notify tribe owner
        notifyOwner(kobold);

        // Sound & cleanup
        this.level.playSound(null, blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 1.0F);
        discard();
    }

    /** Sends a chat message to the tribe owner player. */
    private void notifyOwner(KoboldEntity newKobold) {
        Player owner = newKobold.getTribeOwnerPlayer();
        if (owner == null) return;

        KoboldColorVariant color = TribeManager.getColorForTribe(tribeId);
        String colorCode = color != null ? color.getTextColor() : "f";

        MutableComponent msg = Component.literal(
                colorCode + newKobold.getName().getString() +
                " fhas become a cnew tribe memberf!");
        owner.sendSystemMessage(msg);

        if (owner instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    SoundSource.NEUTRAL,
                    owner.getX(), owner.getY(), owner.getZ(), 1.0F, 1.0F, 0L));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    SoundSource.NEUTRAL,
                    owner.getX(), owner.getY(), owner.getZ(), 1.0F, 1.0F, 0L));
        }
    }

    // -- NBT --------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (tribeId != null) tag.putString("tribeID", tribeId.toString());
        tag.putString("egg_color", this.entityData.get(EGG_COLOR));
        tag.putInt("eggAge",      this.entityData.get(EGG_AGE));
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String id = tag.getString("tribeID");
        if (!id.isEmpty()) {
            try { tribeId = UUID.fromString(id); } catch (IllegalArgumentException ignored) {}
        }
        this.entityData.set(EGG_COLOR, tag.getString("egg_color"));
        this.entityData.set(EGG_AGE,   tag.getInt("eggAge"));
    }

    // -- Equipment (required by LivingEntity) -----------------------------------

    @Override
    public Iterable<ItemStack> getArmorSlots() { return new ArrayList<>(); }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.LEFT; }

    // -- Misc -------------------------------------------------------------------

    @Override
    public boolean canTrample(net.minecraft.world.level.block.state.BlockState state, BlockPos pos, float fallDistance) {
        return false;
    }

    // -- GeckoLib 4 -------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var controller = new AnimationController<>(this, "controller", 5, this::animController);
        sharedController = controller;
        registrar.add(controller);
    }

    private PlayState animController(AnimationState<KoboldEggEntity> state) {
        int age      = this.entityData.get(EGG_AGE);
        float progress = (float) age / MAX_AGE;

        if (MAX_AGE - age < 20) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.model.hatch"));
            return PlayState.CONTINUE;
        }
        if (progress > 0.98f) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.model.veryfast"));
            return PlayState.CONTINUE;
        }
        if (progress > 0.85f) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.model.fast"));
            return PlayState.CONTINUE;
        }
        if (progress > 0.75f) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.model.medium"));
            return PlayState.CONTINUE;
        }
        if (progress > 0.5f) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.model.slow"));
            return PlayState.CONTINUE;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
