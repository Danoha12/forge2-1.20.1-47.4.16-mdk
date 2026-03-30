package com.trolmastercard.sexmod.entity.ai.goal;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.passive.horse.AbstractHorse;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * NpcCombatGoal — Portado a 1.20.1.
 * * IA principal de combate y compañía para los NPCs (NpcInventoryEntity).
 * * Gestiona los estados: ATTACK, FOLLOW, IDLE, RIDE y DOWNED.
 */
public class NpcCombatGoal extends NpcGoalBase {

    NpcInventoryEntity npc;
    LivingEntity attackTarget;
    Entity ridingVehicle;

    double lastOwnerDist = Double.MAX_VALUE;
    Vec3 lastOwnerPos = Vec3.ZERO;

    int noAttackTimer = 0;
    int attackCooldown = 0;
    int bowChargeTimer = 0;
    int followHoldTimer = 0;

    static final Random rand = new Random();

    public NpcCombatGoal(NpcInventoryEntity npc) {
        super(npc);
        this.npc = npc;
    }

    // ── NpcGoalBase Overrides ────────────────────────────────────────────────

    @Override
    public void onActivate() {
        super.onActivate();
        this.lastOwnerDist = this.npc.distanceTo(this.owner);
        this.lastOwnerPos = this.owner.position();
        if (this.npc.getAnimState() == AnimState.BOW) {
            this.npc.setAnimState(AnimState.NULL);
        }
    }

    @Override
    protected boolean isValidEnemy(LivingEntity candidate) {
        if (candidate instanceof BaseNpcEntity) return false;
        if (this.noAttackTimer > 0) return false;
        if (candidate == null || !candidate.isAlive()) return false;
        if (candidate.level() != this.npc.level()) return false;
        if (candidate.equals(this.npc) || candidate.equals(this.owner)) return false;

        // Limites de distancia
        if (this.npc.distanceToSqr(this.owner) >= 225.0D) return false; // 15^2
        if (this.npc.distanceToSqr(candidate) >= 400.0D) return false; // 20^2

        return true;
    }

    @Override
    protected void handleState(State state) {
        switch (state) {
            case ATTACK -> handleAttack();
            case FOLLOW -> handleFollow();
            case IDLE -> handleIdle();
            case RIDE -> handleRide();
            case DOWNED -> this.navigation.stop();
        }
    }

    @Override
    protected State computeNextState() {
        this.noAttackTimer--;

        // DOWNED si está herido gravemente y tiene dueño
        if (this.npc.isDowned && this.npc.hasMaster()) {
            return State.DOWNED;
        }

        // RIDE si el dueño se sube a un caballo domesticado
        if (this.owner.isPassenger()) {
            Entity vehicle = this.owner.getVehicle();
            if (!this.npc.isPassenger() && !this.npc.hasPassenger(vehicle)) {
                if (vehicle instanceof AbstractHorse horse && horse.isTamed()) {
                    this.ridingVehicle = vehicle;
                    return State.RIDE;
                }
            }
        }

        // Desmontar si el dueño ya no está montado
        if (this.npc.isPassenger() && this.currentState == State.RIDE && !this.owner.isPassenger()) {
            this.npc.setAnimState(AnimState.NULL);
            this.npc.stopRiding();
            this.npc.noPhysics = false;
            this.npc.setDeltaMovement(Vec3.ZERO);
        }

        // 1. Prioridad: Seguir atacando al objetivo actual
        if (this.attackTarget != null && isValidEnemy(this.attackTarget)) {
            return State.ATTACK;
        }

        // 2. Responder si el NPC es atacado
        DamageSource lastDmg = this.npc.getLastDamageSource();
        if (lastDmg != null && lastDmg.getEntity() instanceof LivingEntity attacker) {
            if (isValidEnemy(attacker)) {
                this.attackTarget = attacker;
                return State.ATTACK;
            }
        }

        // 3. Responder si el dueño es atacado (dentro de los últimos 7 segundos = 140 ticks)
        int ticksSinceOwnerHurt = this.owner.tickCount - this.owner.getLastHurtByMobTimestamp();
        if (ticksSinceOwnerHurt < 140) {
            LivingEntity ownerAttacker = this.owner.getLastHurtByMob();
            if (ownerAttacker != null && isValidEnemy(ownerAttacker)) {
                this.attackTarget = ownerAttacker;
                return State.ATTACK;
            }
        }

        // 4. Buscar enemigos agresivos de forma pasiva
        if (this.currentState != State.FOLLOW) {
            DamageSource playerDmg = this.owner.getLastDamageSource();
            if (playerDmg != null && playerDmg.getEntity() instanceof LivingEntity la) {
                if (isValidEnemy(la)) {
                    this.attackTarget = la;
                    return State.ATTACK;
                }
            }

            AABB searchBox = this.npc.getBoundingBox().inflate(5.0D, 2.0D, 5.0D);
            List<Monster> nearby = this.npc.level().getEntitiesOfClass(Monster.class, searchBox);
            nearby.sort(Comparator.comparingDouble(m -> m.distanceToSqr(this.npc)));

            for (Monster mob : nearby) {
                if (mob instanceof Creeper) continue; // ¡No ataques a los creepers, es un suicidio!
                if (isValidEnemy(mob)) {
                    this.attackTarget = mob;
                    return State.ATTACK;
                }
            }
        }

        // Lógica de distancia para seguir al dueño
        float distToOwner = this.npc.distanceTo(this.owner);
        boolean closeToOwner = distToOwner <= 5.0F;

        if (!closeToOwner && this.currentState == State.FOLLOW) {
            if (++this.followHoldTimer > 60) {
                followHoldTimer = 0;
                closeToOwner = false;
            } else {
                closeToOwner = true;
            }
        }

        if (closeToOwner && this.currentState == State.ATTACK) {
            this.noAttackTimer = 60;
        }

        return closeToOwner ? State.FOLLOW : State.IDLE;
    }

    // ── Manejadores de Estado ────────────────────────────────────────────────

    private void handleAttack() {
        this.npc.getLookControl().setLookAt(this.attackTarget, 30.0F, 30.0F);
        double dist = this.npc.distanceTo(this.attackTarget);

        this.navigation.stop();
        if (dist < 1.9D && --this.attackCooldown <= 0) {
            performMeleeAttack();
            return;
        }

        // Lógica del Arco
        ItemStack bow = this.npc.getInventory().getStackInSlot(1);
        if (bow.getItem() instanceof BowItem && this.npc.getSensing().hasLineOfSight(this.attackTarget)) {
            if (++this.bowChargeTimer > 0 && dist > 6.0D) {
                this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 2);
                this.npc.setAnimState(AnimState.BOW);

                if (++this.bowChargeTimer >= 32) {
                    this.bowChargeTimer = -20;
                    shootArrow(bow);
                    this.npc.setAnimState(AnimState.NULL);
                }
                return;
            }
        }

        // Acercarse al objetivo si no puede disparar
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 1);
        if (dist < 2.0D) {
            this.navigation.moveTo(this.attackTarget, 0.5D);
            this.npc.setWalkingMode(BaseNpcEntity.MovementMode.WALK);
        } else {
            this.navigation.moveTo(this.attackTarget, 0.7D);
            this.npc.setWalkingMode(BaseNpcEntity.MovementMode.RUN);
        }
    }

    private void handleFollow() {
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 0);
        double dist = this.npc.distanceTo(this.owner);

        if (this.navigation.getPath() != null && this.navigation.getPath().getDistToTarget() > dist) {
            this.navigation.stop();
            if (!this.npc.isDowned) {
                this.navigation.moveTo(this.owner, 0.5D);
                applyFallFix();
            }
        } else {
            retargetOwner();
        }
        this.noAttackTimer = 300;
    }

    private void handleIdle() {
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 0);
        if (!this.npc.isDowned) {
            if (++this.followHoldTimer > 200 + rand.nextInt(100)) {
                this.followHoldTimer = 0;
                Vec3 ownerPos = this.owner.position();
                Vec3 wander = ownerPos.add(
                        1.0D + rand.nextFloat() * 3.0F,
                        0,
                        1.0D + rand.nextFloat() * 3.0F);
                this.navigation.stop();
                this.navigation.moveTo(wander.x, wander.y, wander.z, 0.5D);
            }
        } else if (this.npc.distanceTo(this.owner) > 10.0F) {
            retargetOwner();
        }
    }

    private void handleRide() {
        if (this.owner.isPassenger()) {
            this.npc.setAnimState(AnimState.SIT);
        }
        this.npc.noPhysics = true;
        this.npc.setOnGround(true);
        Vec3 mountPos = this.owner.position().add(
                (this.owner.getLookAngle().x) * 0.5D,
                0.0D,
                (this.owner.getLookAngle().z) * 0.5D);
        this.npc.setPos(mountPos.x, mountPos.y, mountPos.z);
        this.npc.setDeltaMovement(Vec3.ZERO);
        this.npc.setAnimState(AnimState.RIDE);
    }

    // ── Utilidades de Combate (Arco / Melee) ─────────────────────────────────

    private void shootArrow(ItemStack bowStack) {
        Arrow arrow = new Arrow(this.npc.level(), this.npc);

        // Balística: Apuntar al pecho, no a los pies
        double dx = this.attackTarget.getX() - this.npc.getX();
        double dy = this.attackTarget.getY(0.3333333333333333D) - arrow.getY();
        double dz = this.attackTarget.getZ() - this.npc.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + hDist * 0.2D, dz, 1.6F, (float)(14 - this.npc.level().getDifficulty().getId() * 4));

        int powerLvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bowStack);
        int punchLvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bowStack);
        int flameLvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bowStack);

        if (powerLvl > 0) arrow.setBaseDamage(arrow.getBaseDamage() + powerLvl * 0.5D + 0.5D);
        if (punchLvl > 0) arrow.setKnockback(punchLvl);
        if (flameLvl > 0) arrow.setSecondsOnFire(100);

        this.npc.playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (rand.nextFloat() * 0.4F + 0.8F));
        this.npc.level().addFreshEntity(arrow);
    }

    private void performMeleeAttack() {
        this.npc.setAnimState(AnimState.ATTACK);
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 1);

        // Usamos el motor nativo de doHurtTarget de Mob para que aplique escudos, armaduras y encantamientos automáticamente.
        this.npc.doHurtTarget(this.attackTarget);

        // Cooldown de ataque basado en la velocidad de ataque
        float attackSpeedMod = (float) this.npc.getAttributeValue(Attributes.ATTACK_SPEED);
        this.attackCooldown = Math.round(Math.abs(attackSpeedMod) / 3.373494F * 20.0F);

        // Limite de seguridad
        if (this.attackCooldown < 10) this.attackCooldown = 10;
    }

    private void retargetOwner() {
        this.navigation.stop();
        this.navigation.moveTo(this.owner, 0.5D);
        applyFallFix();
    }

    private void applyFallFix() {
        if (!this.npc.onGround() && !this.npc.isInWater()
                && this.npc.getDeltaMovement().x + this.npc.getDeltaMovement().z == 0.0D
                && this.npc.getDeltaMovement().y > 0.0D) {

            // Asumiendo que VectorMathUtil existe en tu mod
            // Vec3 nudge = VectorMathUtil.rotateYaw(new Vec3(0, 0, 0.1D), this.npc.getYRot());
            // this.npc.setDeltaMovement(nudge.x, this.npc.getDeltaMovement().y, nudge.z);
        }
    }

    // ── EventHandler Anidado (Protección de Muerte) ──────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventHandler {

        @SubscribeEvent
        public static void onHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;

            if (npc.isDowned) {
                event.setCanceled(true);
                return;
            }

            // Si el daño es letal y tiene dueño, lo salvamos
            if (npc.getHealth() - event.getAmount() <= 0.0F) {
                if (npc.hasMaster()) {
                    npc.isDowned = true;
                    npc.setAnimState(AnimState.DOWNED);
                    event.setAmount(npc.getHealth() - 1.0F); // Deja 1 HP

                    if (npc.getNavigation() != null) {
                        npc.getNavigation().stop();
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onHeal(LivingHealEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;

            if (npc.isDowned && (npc.getHealth() + event.getAmount() >= npc.getMaxHealth())) {
                npc.isDowned = false;
                npc.setAnimState(AnimState.NULL);
            }
        }

        @SubscribeEvent
        public static void onDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;

            if (!npc.level().isClientSide()) {
                // Soltar inventario al morir
                for (int i = 0; i < 6; i++) {
                    ItemStack stack = npc.getInventory().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        npc.spawnAtLocation(stack);
                    }
                }
            }
        }
    }
}