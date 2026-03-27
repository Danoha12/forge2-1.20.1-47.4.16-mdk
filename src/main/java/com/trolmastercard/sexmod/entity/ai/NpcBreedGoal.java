package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

import java.util.EnumSet;
import java.util.List;

/**
 * NpcBreedGoal (cw) - AI goal: villager seeks a nearby villager partner,
 * approaches, then after 300 ticks calls getBreedOffspring and fires
 * BabyEntitySpawnEvent.
 */
public class NpcBreedGoal extends Goal {

    private final Villager villager;
    private Villager partner;
    private final Level level;
    private int timer;

    public NpcBreedGoal(Villager villager) {
        this.villager = villager;
        this.level = villager.level();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (timer != 0) return false;
        List<Villager> nearby = level.getEntitiesOfClass(
            Villager.class,
            villager.getBoundingBox().inflate(8, 3, 8),
            e -> e != villager);
        if (nearby.isEmpty()) return false;
        partner = nearby.get(0);
        return true;
    }

    @Override
    public void start() {
        timer = 300;
    }

    @Override
    public void stop() {}

    @Override
    public boolean canContinueToUse() { return true; }

    @Override
    public void tick() {
        timer--;
        villager.getLookControl().setLookAt(partner, 10f, 30f);
        if (villager.distanceToSqr(partner) > 2.25) {
            villager.getNavigation().moveTo(partner, 0.25);
        }
        if (timer <= 0) {
            breed();
            villager.goalSelector.removeGoal(this);
        }
        if (villager.getRandom().nextInt(35) == 0) {
            level.broadcastEntityEvent(villager, (byte) 12);
        }
    }

    private void breed() {
        AgeableMob baby = null;
        if (level instanceof ServerLevel sl) {
            baby = villager.getBreedOffspring(sl, partner);
        }
        partner.setAge(6000);
        villager.setAge(6000);

        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(villager, partner, baby);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            AgeableMob child = event.getChild();
            if (child != null) {
                child.setAge(-24000);
                child.moveTo(villager.getX(), villager.getY(), villager.getZ(), 0f, 0f);
                level.addFreshEntity(child);
                level.broadcastEntityEvent(child, (byte) 12);
            }
        }
    }
}
