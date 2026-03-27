package com.trolmastercard.sexmod.entity.ai;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.passive.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Main combat / companion AI goal for all NpcInventoryEntity NPCs.
 *
 * Handles five states (from NpcGoalBase.State):
 *   ATTACK  - melee + ranged (bow) attack on target
 *   FOLLOW  - follow owner, look for threats
 *   IDLE    - wander near owner
 *   RIDE    - NPC mounts a horse the owner is riding
 *   DOWNED  - NPC is incapacitated (hp < 1)
 *
 * Inner class {@link EventHandler} cancels lethal damage and triggers
 * the DOWNED state instead of death.
 *
 * Obfuscated name: g
 */
public class NpcCombatGoal extends NpcGoalBase {

    NpcInventoryEntity npc;

    LivingEntity attackTarget;
    Entity       ridingVehicle;

    double lastOwnerDist = Double.MAX_VALUE;
    Vec3   lastOwnerPos  = Vec3.ZERO;

    int noAttackTimer = 0;
    int arrowCooldown = 0;
    int attackCooldown = 0;
    int bowChargeTimer = 0;
    int followHoldTimer = 0;

    static final Random rand = new Random();

    public NpcCombatGoal(NpcInventoryEntity npc) {
        super(npc);
        this.npc = npc;
    }

    // -- NpcGoalBase overrides ------------------------------------------------

    @Override
    public void onActivate() {
        super.onActivate();
        this.lastOwnerDist = this.npc.distanceTo(this.owner);
        this.lastOwnerPos  = this.owner.position();
        if (this.npc.getAnimState() == AnimState.BOW) this.npc.setAnimState(AnimState.NULL);
    }

    @Override
    protected boolean isValidEnemy(LivingEntity candidate) {
        if (candidate instanceof BaseNpcEntity) return false;
        if (this.noAttackTimer > 0)             return false;
        if (candidate == null)                   return false;
        if (candidate.level() == null)           return false;
        if (candidate.equals(this.npc))         return false;
        if (!candidate.isAlive())                return false;
        if (this.npc.position().distanceTo(this.owner.position()) >= 15.0D) return false;
        if (this.npc.position().distanceTo(candidate.position()) >= 20.0D) return false;
        if (candidate.equals(this.owner))       return false;
        return true;
    }

    @Override
    protected void handleState(State state) {
        switch (state) {
            case ATTACK  -> handleAttack();
            case FOLLOW  -> handleFollow();
            case IDLE    -> handleIdle();
            case RIDE    -> handleRide();
            case DOWNED  -> this.navigation.stop();
        }
    }

    @Override
    protected State computeNextState() {
        this.noAttackTimer--;

        // DOWNED if wounded and has an owner
        if (this.npc.isDowned && this.npc.getOwnerUUID() != null) {
            return State.DOWNED;
        }

        // RIDE if owner or npc is riding a tamed horse
        if (this.owner.isPassenger()) {
            Entity vehicle = this.owner.getVehicle();
            if (!this.npc.isPassenger() && !this.npc.hasPassenger(vehicle)) {
                if (vehicle instanceof AbstractHorse horse && horse.isTamed()) {
                    this.ridingVehicle = vehicle;
                    return State.RIDE;
                }
            }
        }
        if (this.npc.isPassenger() && this.currentState == State.RIDE && !this.owner.isPassenger()) {
            this.npc.setAnimState(AnimState.NULL);
            this.npc.unRide();
            this.npc.noPhysics = false;
            this.npc.setDeltaMovement(Vec3.ZERO);
        }

        // Check attack conditions
        if (this.attackTarget != null && isValidEnemy(this.attackTarget)) {
            return State.ATTACK;
        }

        // Respond to damage the NPC itself received
        DamageSource lastDmg = this.npc.getLastDamageSource();
        if (lastDmg != null && lastDmg.getEntity() instanceof LivingEntity attacker) {
            if (isValidEnemy(attacker)) {
                this.attackTarget = attacker;
                return State.ATTACK;
            }
        }

        // Respond to owner being attacked (within recent ticks)
        int ticksSinceOwnerHurt = this.owner.tickCount - this.owner.getLastHurtByMobTimestamp();
        if (ticksSinceOwnerHurt < 140) {
            LivingEntity ownerAttacker = this.owner.getLastHurtByMob();
            if (ownerAttacker != null && isValidEnemy(ownerAttacker)) {
                this.attackTarget = ownerAttacker;
                return State.ATTACK;
            }
        }

        float distToOwner = this.npc.distanceTo(this.owner);

        // In FOLLOW state - look for nearby monsters to attack
        if (this.currentState != State.FOLLOW) {
            DamageSource playerDmg = this.owner.getLastDamageSource();
            if (playerDmg != null && playerDmg.getEntity() instanceof LivingEntity la) {
                if (isValidEnemy(la)) {
                    this.attackTarget = la;
                    return State.ATTACK;
                }
            }

            Vec3 npcPos = this.npc.position();
            AABB searchBox = new AABB(npcPos.x - 5, npcPos.y - 2, npcPos.z - 5,
                                      npcPos.x + 5, npcPos.y + 2, npcPos.z + 5);
            List<Monster> nearby = this.npc.level().getEntitiesOfClass(Monster.class, searchBox);
            nearby.sort(Comparator.comparingDouble(m -> m.distanceToSqr(this.npc)));
            for (Monster mob : nearby) {
                if (mob instanceof Creeper) continue;
                if (isValidEnemy(mob)) {
                    this.attackTarget = mob;
                    return State.ATTACK;
                }
            }
        }

        // FOLLOW if close enough to owner
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

    // -- State handlers --------------------------------------------------------

    private void handleAttack() {
        this.npc.getLookControl().setLookAt(this.attackTarget, 30.0F, 30.0F);
        double dist = this.npc.distanceTo(this.attackTarget);

        this.navigation.stop();
        if (dist < 1.9D && --this.attackCooldown <= 0) {
            performMeleeAttack();
            return;
        }

        // Try bow if equipped and target in range
        ItemStack bow = this.npc.equipmentHandler.getStackInSlot(1);
        if (bow.getItem() instanceof BowItem &&
                this.npc.getSensing().hasLineOfSight(this.attackTarget)) {
            if (++this.bowChargeTimer > 0 && dist > 6.0D) {
                this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 2);
                this.npc.setAnimState(AnimState.BOW);
                if (++this.bowChargeTimer >= 32) {
                    this.bowChargeTimer = -20;
                    shootArrow();
                    this.npc.setAnimState(AnimState.NULL);
                }
                this.lastOwnerDist = this.npc.distanceTo(this.owner);
                this.lastOwnerPos  = this.owner.position();
                return;
            }
        }

        if (dist < 2.0D) {
            this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 1);
            this.navigation.moveTo(this.attackTarget, 0.5D);
            this.npc.setWalkingMode(BaseNpcEntity.MovementMode.WALK);
        } else {
            this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 1);
            this.navigation.moveTo(this.attackTarget, 0.7D);
            this.npc.setWalkingMode(BaseNpcEntity.MovementMode.RUN);
        }
    }

    private void handleFollow() {
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 0);
        double dist = this.npc.distanceTo(this.owner);
        if (this.navigation.getTargetPos() != null &&
                this.navigation.getPath() != null &&
                this.navigation.getPath().getDistToTarget() > dist) {
            this.navigation.stop();
            if (!this.npc.isDowned) {
                this.navigation.moveTo(this.owner, 0.5D);
                applyFallFix();
            }
        } else {
            retargetOwner();
        }
        this.noAttackTimer = 300;
        checkBowAiming();
    }

    private void handleIdle() {
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 0);
        if (!this.npc.isDowned) {
            if (++this.followHoldTimer > 200 + rand.nextInt(100)) {
                this.followHoldTimer = 0;
                Vec3 ownerPos = this.owner.position();
                Vec3 wander   = ownerPos.add(
                        1.0D + rand.nextFloat() * 3.0F,
                        0,
                        1.0D + rand.nextFloat() * 3.0F);
                this.navigation.stop();
                this.navigation.moveTo(wander.x, wander.y, wander.z, 0.5D);
            }
        } else if (this.npc.distanceTo(this.owner) > 10.0F) {
            retargetOwner();
        }
        checkBowAiming();
    }

    private void handleRide() {
        if (this.owner.isPassenger()) {
            this.npc.setAnimState(AnimState.SIT);
        }
        this.npc.noPhysics  = true;
        this.npc.setOnGround(true);
        Vec3 mountPos = this.owner.position().add(
                (this.owner.getLookAngle().x) * 0.5D,
                0.0D,
                (this.owner.getLookAngle().z) * 0.5D);
        this.npc.setPos(mountPos.x, mountPos.y, mountPos.z);
        this.npc.setDeltaMovement(Vec3.ZERO);
        this.npc.setAnimState(AnimState.RIDE);
    }

    // -- Bow / melee helpers ---------------------------------------------------

    private void shootArrow() {
        Arrow arrow = new Arrow(this.npc.level(), this.npc);
        double dx = this.attackTarget.getX() - this.npc.getX();
        double dy = this.attackTarget.getBoundingBox().minY + this.attackTarget.getBbHeight() / 3.0F
                  - arrow.getY();
        double dz = this.attackTarget.getZ() - this.npc.getZ();
        double hDist = Mth.sqrt((float)(dx * dx + dz * dz));
        arrow.shoot(dx, dy + hDist * 0.2D, dz, 1.6F, 2.0F);

        ItemStack bowStack = this.npc.equipmentHandler.getStackInSlot(1);
        int powerLvl  = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bowStack);
        int punchLvl  = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bowStack);
        int flameLvl  = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bowStack);
        if (powerLvl > 0) arrow.setBaseDamage(arrow.getBaseDamage() + powerLvl * 0.5D + 0.5D);
        if (punchLvl > 0) arrow.setKnockback(punchLvl);
        if (flameLvl > 0) arrow.setSecondsOnFire(100);

        this.npc.playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 1.0F,
                1.0F / (rand.nextFloat() * 0.4F + 0.8F));
        this.npc.level().addFreshEntity(arrow);
    }

    private void performMeleeAttack() {
        this.npc.setAnimState(AnimState.ATTACK);
        this.npc.getEntityData().set(NpcInventoryEntity.DATA_WEAPON_ANIM, 1);

        ItemStack weapon = this.npc.equipmentHandler.getStackInSlot(0);
        float damage  = (float) this.npc.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchDmg = EnchantmentHelper.getDamageBonus(weapon, this.attackTarget.getMobType());
        float knockback = 0.5F * EnchantmentHelper.getKnockbackBonus(this.npc);
        int   fireTicks = EnchantmentHelper.getFireAspect(this.npc) * 4;
        int   sweepLvl  = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, weapon);

        this.attackTarget.knockback(knockback,
                Mth.sin(this.npc.getYRot() * (float)Math.PI / 180F),
               -Mth.cos(this.npc.getYRot() * (float)Math.PI / 180F));
        if (fireTicks > 0) this.attackTarget.setSecondsOnFire(fireTicks);

        if (sweepLvl > 0) {
            float sweepRatio = sweepLvl == 1 ? 0.5F : sweepLvl == 2 ? 0.67F : 0.75F;
            AABB sweepBox = this.attackTarget.getBoundingBox().inflate(1.0D, 0.25D, 1.0D);
            for (LivingEntity nearby : this.npc.level().getEntitiesOfClass(LivingEntity.class, sweepBox)) {
                if (nearby == this.npc || nearby == this.owner || nearby == this.attackTarget) continue;
                if (this.npc.isAlliedTo(nearby)) continue;
                if (this.npc.distanceToSqr(nearby) >= 9.0D) continue;
                nearby.knockback(0.4F,
                        Mth.sin(this.npc.getYRot() * (float)Math.PI / 180F),
                       -Mth.cos(this.npc.getYRot() * (float)Math.PI / 180F));
                nearby.hurt(this.npc.damageSources().mobAttack(this.npc),
                        (damage + enchDmg) * sweepRatio);
            }
        }

        this.attackTarget.hurt(this.npc.damageSources().mobAttack(this.npc), damage + enchDmg);
        float attackSpeedMod = (float) this.npc.getAttributeValue(Attributes.ATTACK_SPEED);
        this.attackCooldown = Math.round(Math.abs(attackSpeedMod) / 3.373494F * 20.0F);
    }

    // -- Utility ---------------------------------------------------------------

    private void retargetOwner() {
        this.navigation.stop();
        this.navigation.moveTo(this.owner, 0.5D);
        applyFallFix();
    }

    private void checkBowAiming() {
        // Nothing - bow aiming is in handleAttack
    }

    /**
     * Nudges the NPC slightly forward when airborne with no horizontal velocity,
     * preventing hover-glitches on slopes.
     */
    private void applyFallFix() {
        if (!this.npc.isOnGround() && !this.npc.isInWater()
                && this.npc.getDeltaMovement().x + this.npc.getDeltaMovement().z == 0.0D
                && this.npc.getDeltaMovement().y > 0.0D) {

            Vec3 nudge = VectorMathUtil.rotateYaw(new Vec3(0, 0, 0.1D), this.npc.getYRot());
            this.npc.setDeltaMovement(nudge.x, this.npc.getDeltaMovement().y, nudge.z);
        }
    }

    // -- Inner event-handler class --------------------------------------------

    /**
     * Forge event listener that intercepts lethal damage on {@link NpcInventoryEntity}
     * instances and triggers the DOWNED state instead of death.
     */
    public static class EventHandler {

        @SubscribeEvent
        public void onHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;
            if (npc.isDowned) {
                event.setCanceled(true);
                return;
            }
            if (npc.getHealth() - event.getAmount() < 0.0F) {
                String ownerStr = npc.getEntityData().get(BaseNpcEntity.DATA_OWNER_UUID);
                if (!ownerStr.isEmpty()) {
                    npc.isDowned = true;
                    npc.setAnimState(AnimState.DOWNED);
                    event.setAmount(npc.getHealth() - 1.0F);
                    npc.getNavigation().stop();
                }
            }
        }

        @SubscribeEvent
        public void onHeal(LivingHealEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;
            if (npc.isDowned && npc.getHealth() + event.getAmount() >= npc.getMaxHealth()) {
                npc.isDowned = false;
                npc.setAnimState(AnimState.NULL);
            }
        }

        @SubscribeEvent
        public void onDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof NpcInventoryEntity npc)) return;
            if (!npc.level().isClientSide()) {
                // Drop equipment items on death
                for (int i = 0; i < 6; i++) {
                    ItemStack stack = npc.equipmentHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) npc.spawnAtLocation(stack);
                }
            }
        }
    }
}
