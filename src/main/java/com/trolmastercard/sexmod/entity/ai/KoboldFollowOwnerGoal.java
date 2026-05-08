package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;

public class KoboldFollowOwnerGoal extends Goal {
    private final KoboldEntity kobold;
    private Player owner;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;

    public KoboldFollowOwnerGoal(KoboldEntity kobold, float startDistance, float stopDistance) {
        this.kobold = kobold;
        this.speed = 0.7D; // Velocidad de carrera
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player master = this.kobold.getTribeOwner();
        if (master == null) return false;
        if (this.kobold.distanceToSqr(master) < (double)(this.startDistance * this.startDistance)) return false;

        this.owner = master;
        return true;
    }

    @Override
    public void tick() {
        this.kobold.getLookControl().setLookAt(this.owner, 10.0F, (float)this.kobold.getMaxHeadXRot());
        if (this.kobold.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance)) {
            this.kobold.getNavigation().moveTo(this.owner, this.speed);
        } else {
            this.kobold.getNavigation().stop();
        }
    }

    public void setFollowTarget(Object target) {
        // Método stub por si lo necesitas llamar desde el tick de la entidad
    }
}