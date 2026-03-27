package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.ModEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FishingHookEntity - Anzuelo de Pesca Personalizado.
 * Portado a 1.20.1.
 * * Proyectil lanzado por el NPC Gato para pescar ítems o enganchar entidades.
 */
public class FishingHookEntity extends Entity {

    // =========================================================================
    //  DataParameters Sincronizados
    // =========================================================================

    /** ID de la entidad enganchada + 1 (0 = Ninguna). */
    private static final EntityDataAccessor<Integer> DATA_HOOKED_ID =
            SynchedEntityData.defineId(FishingHookEntity.class, EntityDataSerializers.INT);

    /** UUID del NPC Gato dueño del anzuelo. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(FishingHookEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // =========================================================================
    //  Máquina de Estados
    // =========================================================================

    public enum HookState { FLYING, HOOKED_IN_ENTITY, BOBBING }

    @Nullable private static CatEntity pendingOwner = null;

    public static void setPendingOwner(@Nullable CatEntity owner) {
        pendingOwner = owner;
    }

    // =========================================================================
    //  Campos de Instancia
    // =========================================================================

    private HookState state    = HookState.FLYING;
    @Nullable
    public Entity     hookedEntity;
    private boolean   inGround = false;
    private int       groundTimer  = 0;
    private int       collisionAge = 0;

    public int  nibble     = 0;
    private int waitTimer  = 0;
    private int approachTimer = 0;
    private float fishAngle = 0.0f;

    private int lureLevel = 0;
    private int luckLevel = 0;

    // =========================================================================
    //  Constructores
    // =========================================================================

    public FishingHookEntity(EntityType<? extends FishingHookEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public FishingHookEntity(Level level, CatEntity owner, double speed) {
        this(ModEntityRegistry.FISHING_HOOK.get(), level);
        initFromOwner(owner);
        launch(speed);
    }

    private void initFromOwner(CatEntity owner) {
        setBoundingBox(getBoundingBox()); // Mantiene las dimensiones del EntityType
        this.noCulling = true;
        // Asumiendo que CatEntity tiene un método setFishingHook
        owner.setFishingHook(this);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HOOKED_ID,  0);
        entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_HOOKED_ID.equals(key)) {
            int id = entityData.get(DATA_HOOKED_ID);
            hookedEntity = (id > 0) ? level().getEntity(id - 1) : null;
        }
        super.onSyncedDataUpdated(key);
    }

    // =========================================================================
    //  Resolución del Dueño
    // =========================================================================

    @Nullable
    public CatEntity getOwnerClient() {
        Optional<UUID> opt = entityData.get(DATA_OWNER_UUID);
        if (opt.isEmpty()) return null;
        Entity e = BaseNpcEntity.getClientEntity(opt.get());
        return e instanceof CatEntity cat ? cat : null;
    }

    @Nullable
    public CatEntity getOwnerServer() {
        Optional<UUID> opt = entityData.get(DATA_OWNER_UUID);
        if (opt.isEmpty()) return null;
        Entity e = BaseNpcEntity.getServerEntity(opt.get());
        return e instanceof CatEntity cat ? cat : null;
    }

    // =========================================================================
    //  Lanzamiento (Físicas)
    // =========================================================================

    private void launch(double speed) {
        CatEntity owner = getOwnerServer();
        if (owner == null && pendingOwner != null) owner = pendingOwner;
        if (owner == null) return;

        float yawRad   = -(owner.getYRot() * Mth.DEG_TO_RAD) - (float)Math.PI;
        float pitchDeg = -22.5f + 45.0f * (float)(owner.position().distanceTo(Vec3.atCenterOf(owner.blockPosition())) / 7.0f);
        float pitchRad = pitchDeg * Mth.DEG_TO_RAD;

        float cosYaw   = Mth.cos(yawRad);
        float sinYaw   = Mth.sin(yawRad);
        float cosP     = -Mth.cos(-pitchRad);
        float sinP     =  Mth.sin(-pitchRad);

        double ox = owner.getX() - sinYaw * 0.3;
        double oy = owner.getEyeY();
        double oz = owner.getZ() - cosYaw * 0.3;
        setPos(ox, oy, oz);

        float clampedRatio = Mth.clamp(-(sinP / cosP), -5.0f, 5.0f);
        this.setDeltaMovement(speed * -sinYaw, speed * clampedRatio, speed * -cosYaw);

        double len = getDeltaMovement().length();
        double nx  = getDeltaMovement().x * (0.6 / len + 0.5 + random.nextGaussian() * 0.0045);
        double ny  = getDeltaMovement().y * (0.6 / len + 0.5 + random.nextGaussian() * 0.0045);
        double nz  = getDeltaMovement().z * (0.6 / len + 0.5 + random.nextGaussian() * 0.0045);
        setDeltaMovement(nx, ny, nz);

        double hLen = Math.sqrt(nx * nx + nz * nz);
        setYRot((float)(Mth.atan2(nx, nz) * Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(ny, hLen) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    // =========================================================================
    //  Lógica de Tick (Vuelo y Pesca)
    // =========================================================================

    @Override
    public void tick() {
        super.tick();

        CatEntity owner = level().isClientSide ? getOwnerClient() : getOwnerServer();
        if (owner == null) { discard(); return; }
        if (level().isClientSide && !checkServer()) return;

        if (inGround) {
            groundTimer++;
            if (groundTimer >= 1200) { discard(); return; }
        }

        tickStateMachine(owner);
    }

    private boolean checkServer() { return true; }

    private void tickStateMachine(CatEntity owner) {
        switch (state) {
            case FLYING -> tickFlying(owner);
            case HOOKED_IN_ENTITY -> tickHookedInEntity();
            case BOBBING -> tickBobbing(owner);
        }

        if (state != HookState.HOOKED_IN_ENTITY) {
            boolean inWater = level().getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER);
            if (!inWater) {
                setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            }
            move(MoverType.SELF, getDeltaMovement());
            updateAngles();

            setDeltaMovement(getDeltaMovement().scale(0.92));
            setPos(getX(), getY(), getZ());
        }
    }

    private void tickFlying(CatEntity owner) {
        if (hookedEntity != null) {
            setDeltaMovement(0, 0, 0);
            state = HookState.HOOKED_IN_ENTITY;
            return;
        }

        boolean inWater = level().getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER);
        if (inWater) {
            setDeltaMovement(getDeltaMovement().multiply(0.3, 0.2, 0.3));
            state = HookState.BOBBING;
            return;
        }

        if (onGround()) {
            inGround = true;
            collisionAge = 0;
            setDeltaMovement(0, 0, 0);
            if (!level().isClientSide) {
                // Asumiendo que CatEntity tiene este método
                owner.onFishingHookLanded();
            }
            return;
        }

        if (!level().isClientSide) performEntityCollision(owner);
    }

    private void performEntityCollision(CatEntity owner) {
        AABB searchBox = getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0);
        List<Entity> nearby = level().getEntities(this, searchBox);
        Entity closest = null;
        double closestDist = 0.0;

        Vec3 from = position();
        Vec3 to   = position().add(getDeltaMovement());

        for (Entity candidate : nearby) {
            if (!candidate.isPickable()) continue;
            if (candidate == owner && collisionAge < 5) continue;
            AABB box = candidate.getBoundingBox().inflate(0.3);
            var hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (closest == null || d < closestDist) {
                    closest = candidate;
                    closestDist = d;
                }
            }
        }

        if (closest != null) {
            hookedEntity = closest;
            entityData.set(DATA_HOOKED_ID, closest.getId() + 1);
            state = HookState.HOOKED_IN_ENTITY;
        }
    }

    private void tickHookedInEntity() {
        if (hookedEntity == null || hookedEntity.isRemoved()) {
            hookedEntity = null;
            state = HookState.FLYING;
            return;
        }
        setPos(hookedEntity.getX(),
                hookedEntity.getBoundingBox().minY + hookedEntity.getBbHeight() * 0.8,
                hookedEntity.getZ());
    }

    private void tickBobbing(CatEntity owner) {
        setDeltaMovement(getDeltaMovement().multiply(0.9, 1.0, 0.9));

        boolean inWater = level().getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER);

        if (!inWater) {
            setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            return;
        }

        if (!level().isClientSide) {
            tickBobbingServer(owner);
        }
    }

    private void tickBobbingServer(CatEntity owner) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (nibble > 0) {
            nibble--;
            if (nibble <= 0) {
                waitTimer = 0;
                approachTimer = 0;
            } else {
                setDeltaMovement(getDeltaMovement().add(0, -0.2 * random.nextFloat() * random.nextFloat(), 0));
            }
            return;
        }

        if (approachTimer > 0) {
            approachTimer -= 1 + lureLevel;
            if (approachTimer > 0) {
                fishAngle += (float)(random.nextGaussian() * 4.0);
                float rad  = fishAngle * Mth.DEG_TO_RAD;
                float sn   = Mth.sin(rad);
                float cs   = Mth.cos(rad);
                double bx  = getX() + sn * approachTimer * 0.1f;
                double by  = Mth.floor(getBoundingBox().minY) + 1.0;
                double bz  = getZ() + cs * approachTimer * 0.1f;

                var blockState = serverLevel.getBlockState(new BlockPos((int)bx, (int)by - 1, (int)bz));
                if (blockState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                    if (random.nextFloat() < 0.15f) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                                bx, by - 0.1, bz, 1, sn, 0.1, cs, 0.0);
                    }
                }
            } else {
                setDeltaMovement(getDeltaMovement().add(0, -0.4f * Mth.nextFloat(random, 0.6f, 1.0f), 0));
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.FISHING_BOBBER_SPLASH, net.minecraft.sounds.SoundSource.NEUTRAL,
                        0.25f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.4f);
                nibble = Mth.nextInt(random, 20, 40);
            }
            return;
        }

        if (waitTimer > 0) {
            waitTimer--;
            float chance = 0.15f;
            if (waitTimer < 20)       chance += (20 - waitTimer) * 0.05f;
            else if (waitTimer < 40)  chance += (40 - waitTimer) * 0.02f;
            else if (waitTimer < 60)  chance += (60 - waitTimer) * 0.01f;

            if (random.nextFloat() < chance) {
                fishAngle    = Mth.nextFloat(random, 0.0f, 360.0f);
                approachTimer = (int) Mth.nextFloat(random, 25.0f, 60.0f);
            }
            if (waitTimer <= 0) {
                fishAngle     = Mth.nextFloat(random, 0.0f, 360.0f);
                approachTimer = Mth.nextInt(random, 20, 80);
            }
            return;
        }

        waitTimer = Mth.nextInt(random, 100, 600) - lureLevel * 20 * 5;
    }

    private void updateAngles() {
        Vec3 vel = getDeltaMovement();
        double hLen = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float newYaw   = (float)(Mth.atan2(vel.x, vel.z) * Mth.RAD_TO_DEG);
        float newPitch = (float)(Mth.atan2(vel.y, hLen)  * Mth.RAD_TO_DEG);

        while (newPitch - xRotO < -180f) xRotO -= 360f;
        while (newPitch - xRotO >= 180f) xRotO += 360f;
        while (newYaw - yRotO < -180f)   yRotO -= 360f;
        while (newYaw - yRotO >= 180f)   yRotO += 360f;

        setXRot(xRotO + (newPitch - xRotO) * 0.2f);
        setYRot(yRotO + (newYaw   - yRotO) * 0.2f);
    }

    // =========================================================================
    //  Recogida del Anzuelo (Retrieve)
    // =========================================================================

    public int retrieve(ItemStack rodStack) {
        if (level().isClientSide) return 0;
        CatEntity owner = getOwnerServer();
        if (owner == null) return 0;

        int damage = 0;
        if (hookedEntity != null) {
            pullHookedEntity(owner);
            damage = (hookedEntity instanceof ItemEntity) ? 3 : 5;
        } else if (nibble > 0) {
            // Dar Botín (Actualizado a API 1.20.1)
            if (level() instanceof ServerLevel serverLevel) {
                LootTable table = serverLevel.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);

                LootParams params = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, this.position()) // Parámetro requerido
                        .create(LootContextParamSets.EMPTY);

                List<ItemStack> loot = table.getRandomItems(params);
                for (ItemStack stack : loot) {
                    owner.receiveItem(stack); // Asumiendo que CatEntity tiene receiveItem
                }
            }
            nibble = 9999;
            damage = 1;
        }

        if (inGround) damage = 2;
        discard();
        owner.setFishingHook(null);
        return damage;
    }

    private void pullHookedEntity(CatEntity owner) {
        if (hookedEntity == null) return;
        double dx = owner.getX() - getX();
        double dy = owner.getY() - getY();
        double dz = owner.getZ() - getZ();
        hookedEntity.setDeltaMovement(
                hookedEntity.getDeltaMovement().add(dx * 0.1, dy * 0.1, dz * 0.1));
    }

    // =========================================================================
    //  NBT Transitorio
    // =========================================================================

    @Override public void addAdditionalSaveData(CompoundTag tag) {}
    @Override public void readAdditionalSaveData(CompoundTag tag) {}

    // Este método lo usas si extiendes o serializas por fuera
    public CompoundTag serializeNBT() { return new CompoundTag(); }

    @Override protected boolean shouldBeSaved() { return false; }

    // =========================================================================
    //  Misceláneo
    // =========================================================================

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) { return dist < 4096.0; }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {}

    public void setLure(int level) { this.lureLevel = level; }
    public void setLuck(int level) { this.luckLevel = level; }

    @Nullable
    public Entity getHookedEntity() { return hookedEntity; }

    public HookState getHookState() { return state; }
}