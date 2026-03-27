package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.ModConstants;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Random;

/**
 * EnergyBallEntity - Portado a 1.20.1.
 * * Proyectil disparado por GalathEntity.
 * * Si el jugador lo golpea, se redirige. Si impacta, explota.
 * * También invoca un WitherSkeleton tras un tiempo si no es destruido.
 */
public class EnergyBallEntity extends Mob {

    // =========================================================================
    //  Constantes
    // =========================================================================

    public static final float RADIUS         = 0.4F;
    public static final float HALF_SIZE      = 0.3F;
    public static final int   LIFETIME       = 200;
    public static final int   SUMMON_TICKS   = 100;
    public static final float FLY_SPEED      = 0.5F;
    public static final float DRAG           = 0.15F;
    public static final float EXPLOSION_RADIUS = 0.75F;

    // =========================================================================
    //  Campos de Instancia
    // =========================================================================

    public double scale = 1.0D;
    Vec3 velocity = Vec3.ZERO;
    boolean shouldExplode = false;
    boolean canSummon = true;
    GalathEntity owner;

    // =========================================================================
    //  Constructores
    // =========================================================================

    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type, Level level, GalathEntity owner) {
        this(type, level);
        this.owner = owner;
    }

    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type, Level level, GalathEntity owner, Vec3 velocity) {
        this(type, level);
        this.velocity = velocity;
        this.owner    = owner;
    }

    // =========================================================================
    //  Overrides de Mob (Comportamiento)
    // =========================================================================

    @Override
    public boolean isPersistenceRequired() { return false; }

    @Override
    protected void checkDespawn() { /* Nunca desaparece por distancia normal */ }

    // =========================================================================
    //  Tick (Lógica principal)
    // =========================================================================

    @Override
    public void tick() {
        if (isRemoved()) return;

        this.noPhysics = true;
        this.setDeltaMovement(velocity);
        super.tick();

        if (level().isClientSide()) {
            spawnMovingParticlesClient();
        }

        tickExplosion();

        if (!level().isClientSide() && level().isEmptyBlock(blockPosition())) return;

        tickSummon();
        this.discard(); // En 1.20.1 usamos discard() en lugar de setDead() o remove()
    }

    // =========================================================================
    //  Mecánicas de Combate
    // =========================================================================

    void tickExplosion() {
        if (level().isClientSide() || !shouldExplode) return;

        AABB box = new AABB(
                position().subtract(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS),
                position().add(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS));

        List<GalathEntity> galaths = level().getEntitiesOfClass(GalathEntity.class, box);
        if (galaths.isEmpty()) return;

        level().explode(this, getX(), getY(), getZ(), 1.0F, false, Level.ExplosionInteraction.NONE);

        for (GalathEntity g : galaths) {
            g.onEnergyBallHit(position()); // Asegúrate de tener este método en GalathEntity
        }
        this.discard();
    }

    void tickSummon() {
        if (level().isClientSide() || isRemoved() || !canSummon) return;

        Vec3 spawnPos = new Vec3(getX(), blockPosition().getY() + 1, getZ());

        if (!isOwnerNearby(spawnPos)) {
            level().explode(this, spawnPos.x, spawnPos.y, spawnPos.z, 2.0F, false, Level.ExplosionInteraction.NONE);
            canSummon = false;
            return;
        }

        WitherSkeleton skeleton = new WitherSkeleton(EntityType.WITHER_SKELETON, level());
        skeleton.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        skeleton.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level().addFreshEntity(skeleton);

        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                new SpawnEnergyBallParticlesPacket(spawnPos, true));

        // Conecta con la lista de minions que configuramos en GalathAttackState
        if (owner != null) owner.skeletons.add(skeleton);
    }

    boolean isOwnerNearby(Vec3 pos) {
        if (owner == null) return true;
        net.minecraft.world.entity.LivingEntity target = owner.getTarget();
        if (target == null) return true;
        return target.distanceToSqr(pos.x, pos.y, pos.z) < 15.0D;
    }

    // =========================================================================
    //  Sistema de Daño (Actualizado a 1.20.1)
    // =========================================================================

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // 1.20.1: Caída fuera del mundo
        if (src.equals(damageSources().fellOutOfWorld())) {
            this.setHealth(0);
            canSummon = false;
            this.discard();
            return true;
        }

        // 1.20.1: Impacto de flecha
        if (!level().isClientSide() && src.getDirectEntity() instanceof AbstractArrow arrow) {
            this.setHealth(0);
            canSummon = false;
            ModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new SpawnEnergyBallParticlesPacket(position(), false));

            arrow.discard();
            this.discard();
            return true;
        }

        // El jugador le da un puñetazo: devuelve la bola de energía
        Entity attacker = src.getEntity();
        if (!(attacker instanceof net.minecraft.world.entity.player.Player)) return false;

        this.velocity = attacker.getLookAngle(); // Redirige hacia donde mira el jugador
        this.shouldExplode = true;
        return true;
    }

    // =========================================================================
    //  NBT Guardado
    // =========================================================================

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        this.discard(); // Es un proyectil volátil, no se guarda en disco al salir
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}

    @Override
    protected void defineSynchedData() { super.defineSynchedData(); }

    // =========================================================================
    //  Partículas (Cliente)
    // =========================================================================

    void spawnMovingParticlesClient() {
        double px = net.minecraft.util.Mth.lerp(0.5D, this.xOld, this.getX());
        double py = net.minecraft.util.Mth.lerp(0.5D, this.yOld, this.getY());
        double pz = net.minecraft.util.Mth.lerp(0.5D, this.zOld, this.getZ());
        spawnAmbientParticle(px, py, pz);
        spawnAmbientParticle(getX(), getY(), getZ());
    }

    void spawnAmbientParticle(double x, double y, double z) {
        Random rng = ModConstants.RANDOM; // Asegúrate de que RANDOM esté en ModConstants
        level().addParticle(ParticleTypes.DRAGON_BREATH,
                x + rng.nextDouble() * 0.3D,
                y + 0.25D + rng.nextDouble() * 0.3D,
                z + rng.nextDouble() * 0.3D,
                0.0D, 0.0D, 0.0D);
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnRingParticles(Vec3 pos) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;

        float step = 1.8F;
        Random rng = ModConstants.RANDOM;

        for (float angle = 0.0F; angle < Math.PI * 2.0D; angle += step) {
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            world.addParticle(
                    ParticleTypes.SMOKE,
                    pos.x + sin * 0.5D, pos.y, pos.z + cos * 0.5D,
                    sin * 0.15D, rng.nextDouble() * 0.15D, cos * 0.15D);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnDeathParticles(Vec3 pos) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;

        Random rng = ModConstants.RANDOM;
        for (int i = 0; i < 100; i++) {
            world.addParticle(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z,
                    rng.nextDouble() * 0.15D, rng.nextDouble() * 0.15D, rng.nextDouble() * 0.15D);
        }
        world.playLocalSound(pos.x, pos.y, pos.z,
                ModSounds.MISC_SHATTER[0], SoundSource.AMBIENT, 0.7F, 1.0F, false);
    }
}