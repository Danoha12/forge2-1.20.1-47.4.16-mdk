package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.EnergyBallImpactPacket; // <-- Import actualizado
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
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

/**
 * EnergyBallEntity — Portado a 1.20.1.
 * * Proyectil invocado por Galath que viaja en línea recta.
 * * Hereda de Entity para evitar crashes por falta de atributos de Mob.
 */
public class EnergyBallEntity extends Entity {

    public static final float EXPLOSION_RADIUS = 0.75F;
    private static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData.defineId(EnergyBallEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 flightVelocity = Vec3.ZERO;
    private boolean shouldExplode = false;
    private boolean canSummon = true;
    private GalathEntity owner;

    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type, Level level, GalathEntity owner, Vec3 velocity) {
        this(type, level);
        this.owner = owner;
        this.flightVelocity = velocity;
        this.setCharge(1.0F); // Carga completa al nacer
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_CHARGE, 0.0F);
    }

    public float getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    public void setCharge(float charge) {
        this.entityData.set(DATA_CHARGE, charge);
    }

    // ── Lógica de Vuelo y Colisión ───────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        // 1. Aplicar movimiento manual (porque noPhysics es true)
        this.setDeltaMovement(this.flightVelocity);
        Vec3 delta = this.getDeltaMovement();
        this.setPos(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z);

        if (this.level().isClientSide()) {
            spawnMovingParticlesClient();
            return; // El cliente solo dibuja partículas, no calcula colisiones
        }

        // 2. Lógica de explosión si fue redirigida
        if (this.shouldExplode) {
            checkExplosionContact();
        }

        // 3. Colisión con el mundo (Si el bloque no es aire, choca)
        if (!this.level().getBlockState(this.blockPosition()).isAir()) {
            executeSummonSequence();
            this.discard();
        }
    }

    private void checkExplosionContact() {
        AABB box = this.getBoundingBox().inflate(EXPLOSION_RADIUS);
        List<GalathEntity> galaths = this.level().getEntitiesOfClass(GalathEntity.class, box);

        if (!galaths.isEmpty()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, Level.ExplosionInteraction.NONE);
            for (GalathEntity g : galaths) {
                // Aquí deberías tener tu método para hacerle daño/stun a Galath
                // g.onEnergyBallHit(this.position());
            }
            this.discard();
        }
    }

    // ── Invocación de Esbirros ───────────────────────────────────────────────

    private void executeSummonSequence() {
        if (!this.canSummon) return;

        Vec3 spawnPos = new Vec3(this.getX(), this.blockPosition().getY() + 1, this.getZ());

        // Control de distancia (Evita invocar si está muy lejos del objetivo)
        if (owner != null && owner.getTarget() != null) {
            if (owner.getTarget().distanceToSqr(spawnPos) > 225.0D) {
                this.level().explode(this, spawnPos.x, spawnPos.y, spawnPos.z, 2.0F, Level.ExplosionInteraction.NONE);
                return;
            }
        }

        WitherSkeleton skeleton = EntityType.WITHER_SKELETON.create(this.level());
        if (skeleton != null) {
            skeleton.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            skeleton.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            if (this.level().addFreshEntity(skeleton)) {
                // <-- PAQUETE CORREGIDO AQUÍ
                ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new EnergyBallImpactPacket(spawnPos, true));

                if (this.owner != null) {
                    this.owner.skeletons.add(skeleton);
                }
            }
        }
    }

    // ── Interacción de Daño (Redirección y Destrucción) ──────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.isRemoved()) return false;

        // 1. Despawn por el vacío
        if (source.is(DamageTypes.OUT_OF_WORLD)) {
            this.discard();
            return true;
        }

        // 2. Destrucción por flechas
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            // <-- PAQUETE CORREGIDO AQUÍ
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new EnergyBallImpactPacket(this.position(), false));
            arrow.discard();
            this.discard();
            return true;
        }

        // 3. Redirección estilo Ghast por golpe del jugador
        if (source.getEntity() instanceof Player player) {
            this.flightVelocity = player.getLookAngle().scale(0.5);
            this.shouldExplode = true; // Ahora es letal para Galath
            // Asegúrate de que este sonido exista en tu registro
            // this.level().playSound(null, this.blockPosition(), ModSounds.MISC_BEEW[2].get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    // ── Visuales y Persistencia ──────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    private void spawnMovingParticlesClient() {
        this.level().addParticle(ParticleTypes.DRAGON_BREATH,
                this.getX() + (this.random.nextDouble() - 0.5) * 0.2,
                this.getY() + 0.3,
                this.getZ() + (this.random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnRingParticles(Vec3 pos) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;
        for (float angle = 0; angle < Math.PI * 2; angle += 0.4F) {
            world.addParticle(ParticleTypes.SMOKE,
                    pos.x + Math.sin(angle) * 0.5, pos.y, pos.z + Math.cos(angle) * 0.5,
                    Math.sin(angle) * 0.1, 0.05, Math.cos(angle) * 0.1);
        }
    }

    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { this.discard(); }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
}