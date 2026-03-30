package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FishingHookEntity — Anzuelo personalizado para el NPC Luna.
 * Portado a 1.20.1 y enmascarado (SFW).
 * * Gestiona la física de vuelo, el flote en el agua y la generación de botín.
 */
public class FishingHookEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_HOOKED_ID =
            SynchedEntityData.defineId(FishingHookEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(FishingHookEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public enum HookState { FLYING, HOOKED_IN_ENTITY, BOBBING }

    @Nullable private static LunaEntity pendingOwner = null;
    private HookState state = HookState.FLYING;
    @Nullable public Entity hookedEntity;
    private boolean inGround = false;
    private int groundTimer = 0;

    public int nibble = 0;
    private int waitTimer = 0;
    private int approachTimer = 0;
    private float fishAngle = 0.0f;
    private int lureLevel = 0;
    private int luckLevel = 0;

    public FishingHookEntity(EntityType<? extends FishingHookEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public FishingHookEntity(Level level, LunaEntity owner, double speed) {
        this(ModEntityRegistry.FISHING_HOOK.get(), level);
        this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        owner.setFishingHook(this);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        launch(speed);
    }

    public static void setPendingOwner(@Nullable LunaEntity owner) {
        pendingOwner = owner;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HOOKED_ID, 0);
        entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    // ── Lógica de Estado (State Machine) ──────────────────────────────────────



    @Override
    public void tick() {
        super.tick();

        LunaEntity owner = getOwner();
        if (owner == null) {
            this.discard();
            return;
        }

        if (inGround) {
            if (++groundTimer >= 1200) { this.discard(); return; }
        }

        switch (state) {
            case FLYING -> tickFlying(owner);
            case HOOKED_IN_ENTITY -> tickHookedInEntity();
            case BOBBING -> tickBobbing(owner);
        }

        if (state != HookState.HOOKED_IN_ENTITY) {
            applyPhysics();
        }
    }

    private void applyPhysics() {
        Vec3 movement = this.getDeltaMovement();
        if (!this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER)) {
            this.setDeltaMovement(movement.add(0, -0.03, 0));
        }
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
    }

    private void tickFlying(LunaEntity owner) {
        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER)) {
            state = HookState.BOBBING;
            return;
        }

        if (this.onGround()) {
            inGround = true;
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide) owner.onFishingHookLanded();
            return;
        }
    }

    private void tickBobbing(LunaEntity owner) {
        if (this.level().isClientSide) return;

        if (nibble > 0) {
            nibble--;
            if (nibble <= 0) {
                waitTimer = 0;
                approachTimer = 0;
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.1 * random.nextFloat(), 0));
            }
        } else if (approachTimer > 0) {
            // Lógica de aproximación del "pez" (partículas de burbujas)
            processFishApproach();
        } else if (waitTimer > 0) {
            waitTimer--;
        } else {
            waitTimer = Mth.nextInt(random, 100, 600) - (lureLevel * 100);
        }
    }

    private void processFishApproach() {
        approachTimer--;
        if (approachTimer <= 0) {
            // ¡Pica el anzuelo!
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25f, 1.0f);
            nibble = Mth.nextInt(random, 20, 40);
        } else {
            // Spawn de burbujas para indicar que algo se acerca
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(),
                        1, 0, 0, 0, 0.01);
            }
        }
    }

    // ── Obtención de Recompensa (Loot) ────────────────────────────────────────

    public int retrieve(ItemStack rodStack) {
        if (this.level().isClientSide || getOwner() == null) return 0;

        int damage = 1;
        if (nibble > 0) {
            generateLoot(rodStack);
            damage = 2;
        }

        this.discard();
        getOwner().setFishingHook(null);
        return damage;
    }

    private void generateLoot(ItemStack rodStack) {
        if (this.level() instanceof ServerLevel serverLevel) {
            LootTable table = serverLevel.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
            LootParams params = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, this.position())
                    .withParameter(LootContextParams.TOOL, rodStack)
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .create(LootContextParamSets.FISHING);

            List<ItemStack> items = table.getRandomItems(params);
            for (ItemStack item : items) {
                getOwner().receiveItem(item);
            }
        }
    }

    @Nullable
    public LunaEntity getOwner() {
        Optional<UUID> uuid = entityData.get(DATA_OWNER_UUID);
        if (uuid.isPresent()) {
            Entity e = this.level().isClientSide ? BaseNpcEntity.getClientEntity(uuid.get()) : BaseNpcEntity.getServerEntity(uuid.get());
            return e instanceof LunaEntity ? (LunaEntity) e : null;
        }
        return pendingOwner;
    }

    private void launch(double speed) {
        // Implementación simplificada del lanzamiento basado en la mirada de Luna
        this.setDeltaMovement(new Vec3(0, 0.5 * speed, 0.5 * speed));
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}

    public void setLure(int level) { this.lureLevel = level; }
    public void setLuck(int level) { this.luckLevel = level; }
    private void tickHookedInEntity() {} // Implementación pendiente si es necesario
}