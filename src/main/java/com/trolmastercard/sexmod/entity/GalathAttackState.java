package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ResetControllerPacket;
import com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import com.trolmastercard.sexmod.util.AngleUtil;
import com.trolmastercard.sexmod.util.Vec2D;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * GalathAttackState - Estados de Combate del Jefe.
 * Portado a 1.20.1.
 * * Define los 4 comportamientos principales de combate que Galath puede realizar.
 * * Maneja las matemáticas complejas de vuelo, invocación, teletransporte y captura.
 */
public enum GalathAttackState {

    // -- REPOSICIONAMIENTO ESTRATÉGICO ----------------------------------------
    CHANGE_POSITION(
            galath -> {
                var level   = galath.level();
                var curPos  = galath.blockPosition();
                var target  = galath.getTarget();
                if (target == null) return;
                var tgtPos  = target.blockPosition();

                List<net.minecraft.core.BlockPos> airCandidates    = new ArrayList<>();
                Map<net.minecraft.core.BlockPos, Integer> scored   = new java.util.TreeMap<>(Comparator.comparingInt(p -> p.distManhattan(tgtPos)));

                for (int dx = -10; dx < 10; dx++) {
                    for (int dz = -10; dz < 10; dz++) {
                        var candidate = curPos.offset(dx, 0, dz);
                        boolean solidBelow = !level.isEmptyBlock(candidate.below());
                        if (solidBelow) {
                            scored.put(candidate, Math.abs(dx) + Math.abs(dz));
                        } else {
                            airCandidates.add(candidate);
                        }
                    }
                }

                if (!scored.isEmpty()) {
                    var entries = new ArrayList<>(scored.entrySet());
                    entries.sort(Map.Entry.comparingByValue());
                    int pick = galath.getRandom().nextInt(Math.max(1, entries.size() - 1));
                    galath.interpTarget = Vec3.atCenterOf(entries.get(pick).getKey());
                } else {
                    if (airCandidates.isEmpty()) {
                        galath.interpTarget = new Vec3(
                                tgtPos.getX() + (galath.getRandom().nextBoolean() ? 1 : -1) * galath.getRandom().nextFloat() * 10,
                                tgtPos.getY(),
                                tgtPos.getZ() + (galath.getRandom().nextBoolean() ? 1 : -1) * galath.getRandom().nextFloat() * 10);
                    } else {
                        var chosen = airCandidates.get(galath.getRandom().nextInt(airCandidates.size()));
                        galath.interpTarget = Vec3.atCenterOf(chosen);
                    }
                }

                galath.prevRenderPos = null;
                galath.knockGroundTick = 0; // Usado como StateCounter temporal
                galath.setAnimState(AnimState.FLY);
                ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> galath),
                        new ResetControllerPacket(galath.getId()));
            },
            galath -> {
                Vec3 currentPos = galath.position();
                Vec3 targetPos  = galath.interpTarget;
                if (targetPos == null) return;

                galath.prevRenderPos = currentPos;
                int counter = galath.knockGroundTick;
                galath.knockGroundTick = counter + 1;
                if (counter != 0) return;

                Vec3 diff = targetPos.subtract(currentPos);
                Vec3 norm = diff.normalize();
                galath.setDeltaMovement(norm.x * 0.6, Mth.clamp(diff.y * 0.6, -0.6, 0.6), norm.z * 0.6);
            },
            galath -> galath.knockGroundTick > 23,
            galath -> {
                galath.setDeltaMovement(Vec3.ZERO);
                galath.knockGroundTick = 0;
                galath.prevRenderPos = null;
            },
            false, galath -> true, false
    ),

    // -- INVOCACIÓN DE ESQUELETOS (SUMMON_SKELETON) -----------------------------
    SUMMON_SKELETON(
            galath -> {
                galath.setAnimState(AnimState.SUMMON_SKELETON);
                galath.summonBoneTick = 0; // Skeleton spawn timer
                galath.getEntityData().set(GalathEntity.DATA_EB_R, true);
                galath.getEntityData().set(GalathEntity.DATA_EB_L, true);
                galath.getEntityData().set(GalathEntity.DATA_MIRRORED, galath.getRandom().nextBoolean());
                BaseNpcEntity.sendNpcSound(galath, ModSounds.GIRLS_GALATH_STRONGCHARGE, true);
            },
            galath -> {
                galath.setDeltaMovement(Vec3.ZERO);
                if (galath.summonBoneTick != 30) return;

                Vec3 galathPos = galath.position();
                var target = galath.getTarget();
                if (target == null) return;
                Vec3 targetPos = target.position();
                var rng        = galath.getRandom();
                boolean mirrored = galath.getEntityData().get(GalathEntity.DATA_MIRRORED);

                if (galath.getEntityData().get(GalathEntity.DATA_EB_R)) {
                    Vec3 boneOffset = VectorMathUtil.rotateAroundY(
                            mirrored ? VectorMathUtil.mirrorX(GalathEntity.ENERGY_BALL_R_OFFSET) : GalathEntity.ENERGY_BALL_R_OFFSET,
                            180.0F + galath.getYRot());
                    Vec3 fireFrom = galathPos.add(boneOffset);
                    Vec3 dir = targetPos.subtract(fireFrom).normalize()
                            .add(rng.nextDouble() * 0.3, rng.nextDouble() * 0.3, rng.nextDouble() * 0.3).normalize();

                    EnergyBallEntity ball1 = new EnergyBallEntity(ModEntityRegistry.ENERGY_BALL.get(), galath.level(), galath);
                    ball1.setDeltaMovement(dir.scale(0.4));
                    ball1.moveTo(fireFrom.x, fireFrom.y, fireFrom.z);
                    galath.level().addFreshEntity(ball1);
                }

                if (galath.getEntityData().get(GalathEntity.DATA_EB_L)) {
                    Vec3 boneOffset = VectorMathUtil.rotateAroundY(
                            mirrored ? VectorMathUtil.mirrorX(GalathEntity.ENERGY_BALL_L_OFFSET) : GalathEntity.ENERGY_BALL_L_OFFSET,
                            180.0F + galath.getYRot());
                    Vec3 fireFrom = galathPos.add(boneOffset);
                    Vec3 dir = targetPos.subtract(fireFrom).normalize()
                            .add(rng.nextDouble() * 0.3, rng.nextDouble() * 0.3, rng.nextDouble() * 0.3).normalize();

                    EnergyBallEntity ball2 = new EnergyBallEntity(ModEntityRegistry.ENERGY_BALL.get(), galath.level(), galath);
                    ball2.setDeltaMovement(dir.scale(0.4));
                    ball2.moveTo(fireFrom.x, fireFrom.y, fireFrom.z);
                    galath.level().addFreshEntity(ball2);
                }
            },
            galath -> galath.summonBoneTick >= 45,
            galath -> galath.summonBoneTick = 0,
            true, galath -> galath.skeletons.size() < 2, true
    ),

    // -- ATAQUE DE RÁFAGA (ATTACK_SWORD) ----------------------------------------
    ATTACK_SWORD(
            galath -> {
                galath.setAttackAnimIdx(0); // Sword Phase
                galath.setAnimState(AnimState.ATTACK_SWORD);
                galath.setDeltaMovement(Vec3.ZERO);

                Vec3 galathPos = galath.position();
                galath.renderPos = galathPos; // Charge start pos
                var target = galath.getTarget();
                if (target == null) return;
                Vec3 targetPos = target.position();

                Vec2D dir2d = new Vec2D(targetPos.x - galathPos.x, targetPos.z - galathPos.z);
                double yaw  = AngleUtil.toDegrees(Math.atan2(dir2d.x, dir2d.y)) - 90.0;
                galath.swordHitActive = true;
                galath.setYRot((float) yaw);
                BaseNpcEntity.sendNpcSound(galath, ModSounds.GIRLS_GALATH_STRONGCHARGE, true);
            },
            galath -> {
                var target = galath.getTarget();
                if (target == null) return;
                int phase  = galath.getAttackAnimIdx() + 1;
                galath.setAttackAnimIdx(phase);

                if (MathUtil.between(phase, 24, 32)) {
                    Vec3 tgtPos   = target.position().add(0, target.getEyeHeight(), 0);
                    Vec2D dir2d   = new Vec2D(tgtPos.x - galath.getX(), tgtPos.z - galath.getZ());
                    double yaw    = AngleUtil.toDegrees(Math.atan2(dir2d.x, dir2d.y)) - 90.0;
                    galath.setYRot((float) yaw);
                    Vec3 back     = VectorMathUtil.rotateAroundY(new Vec3(0, 0, 3), (float)(yaw + 180));
                    Vec3 wantPos  = tgtPos.add(back);
                    float t       = (phase - 24) / 8.0F;
                    galath.moveTo(MathUtil.lerpVec3(galath.renderPos, wantPos, t));

                } else if (MathUtil.between(phase, 32, 54)) {
                    Vec3 behind = VectorMathUtil.rotateAroundY(new Vec3(0, 0, 1.5), galath.getYRot() + 180);
                    galath.moveTo(target.position().add(behind));

                    GalathCombatDamageSource dmgSrc = new GalathCombatDamageSource(galath.damageSources(), galath);
                    target.invulnerableTime = 0;
                    if (phase == 36) target.hurt(dmgSrc, 5.0F);
                    if (phase == 40) target.hurt(dmgSrc, 5.0F);

                } else if (phase == 54) {
                    galath.swordHitActive = false;
                    galath.setAnimState(AnimState.FLY);
                    Vec3 away = galath.renderPos.subtract(galath.position()).normalize();
                    galath.setDeltaMovement(away.x * 0.6, away.y * 0.6, away.z * 0.6);
                    galath.knockGroundTick = 1; // State counter
                } else {
                    galath.knockGroundTick++;
                }
            },
            galath -> galath.knockGroundTick > 23,
            galath -> {
                galath.knockGroundTick = 0;
                galath.setDeltaMovement(Vec3.ZERO);
                galath.setAttackAnimIdx(-1);
                galath.swordHitActive = false;
            },
            true, galath -> true, false
    ),

    // -- ATAQUE SORPRESA / CAPTURA (Original: RAPE) -----------------------------
    SURPRISE_ATTACK(
            galath -> {
                galath.setAnimState(AnimState.SURPRISE_PREPARE);
                galath.knockGroundTick = 0; // surprise timer
                galath.prevRenderPos   = null; // surprise approach start
                galath.interpTarget    = null; // target pos
                galath.getEntityData().set(GalathEntity.DATA_FLOAT, 0.0F); // progress
            },
            galath -> {
                if (++galath.knockGroundTick < 48) return;

                galath.setAnimState(AnimState.SURPRISE_CHARGE);
                var target = galath.getTarget();
                if (target == null) return;

                if (galath.prevRenderPos == null) {
                    galath.interpTarget  = target.position().add(0, target.getEyeHeight() / 2.0, 0);
                    galath.prevRenderPos = galath.position();
                    Vec3 toTarget = target.position().subtract(galath.position()).normalize();
                    galath.setYRot((float)(AngleUtil.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0));
                }

                // AABB grab-check
                Vec3 center = galath.position();
                AABB grabBox = new AABB(
                        center.x - 0.65, center.y - 0.65, center.z - 0.65,
                        center.x + 0.65, center.y + 0.65, center.z + 0.65);

                for (Player player : galath.level().getEntitiesOfClass(Player.class, grabBox)) {
                    if (player.isRemoved() || !player.onGround()) continue;
                    if (BaseNpcEntity.getDataFor(player.getUUID(), true) != null) continue;

                    Vec3 toPlayer = player.position().subtract(center);
                    Vec3 rotated  = VectorMathUtil.rotateAroundY(toPlayer, galath.getYRot());
                    if (Math.abs(rotated.x) > 0.65) continue;

                    // Remover minions en la captura
                    for (var minion : galath.skeletons) {
                        Vec3 mpos = minion.position();
                        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                                        new net.minecraftforge.network.PacketDistributor.TargetPoint(
                                                mpos.x, mpos.y, mpos.z, 50.0, galath.level().dimension())),
                                new SpawnEnergyBallParticlesPacket(mpos, true));
                        minion.discard();
                    }
                    galath.skeletons.clear();

                    if (player instanceof ServerPlayer sp) {
                        galath.moveTo(player.position());
                        galath.setInteractionTargetUUID(player.getUUID());
                        galath.setInteractionActive(true);
                        galath.setAnimState(AnimState.SURPRISE_INTRO);

                        byte lookByte = (byte) Mth.floor((galath.getYRot() + 180.0F) * 256.0F / 360.0F);
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
                        sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), new Vec3(0,0,0)));
                        sp.connection.send(new ClientboundMoveEntityPacket.Rot(sp.getId(), lookByte, (byte)(-14), true));
                    }
                    return;
                }

                // Matemáticas de Bézier para la trayectoria de captura
                Vec3 start  = galath.prevRenderPos;
                Vec3 mid    = galath.interpTarget;
                Vec3 cur    = galath.position();
                Vec3 outDir = mid.subtract(start);
                Vec3 peak   = new Vec3(mid.add(outDir).x, start.y, mid.add(outDir).z);

                boolean firstHalf = cur.distanceToSqr(new Vec3(start.x, cur.y, start.z)) > cur.distanceToSqr(new Vec3(peak.x, cur.y, peak.z));

                double t, segLen;
                if (firstHalf) {
                    t      = VectorMathUtil.paramOnSegment(mid, peak, cur);
                    segLen = mid.distanceTo(peak);
                } else {
                    t      = VectorMathUtil.paramOnSegment(start, mid, cur);
                    segLen = start.distanceTo(mid);
                }

                double step = (segLen / 0.05) > 0 ? 1.0 / (segLen / 0.05) * 20.0 : 0;
                t += step;

                if (!firstHalf && t < 0.9) {
                    galath.interpTarget = target.position().add(0, target.getEyeHeight() / 2.0, 0);
                }

                Vec3 newPos;
                if (firstHalf) {
                    newPos = new Vec3(
                            MathUtil.lerp(mid.x, peak.x, Math.min(1.0, t)),
                            MathUtil.lerp(mid.y, peak.y, Math.min(1.0, MathUtil.smoothStep(t))),
                            MathUtil.lerp(mid.z, peak.z, Math.min(1.0, t)));
                } else {
                    newPos = new Vec3(
                            MathUtil.lerp(start.x, mid.x, t),
                            MathUtil.lerp(start.y, mid.y, MathUtil.easeInOut(t)),
                            MathUtil.lerp(start.z, mid.z, t));
                }

                galath.moveTo(newPos.x, newPos.y, newPos.z);
                if (firstHalf) galath.getEntityData().set(GalathEntity.DATA_FLOAT, (float) t);
            },
            galath -> {
                if (galath.getAnimState() == AnimState.SURPRISE_INTRO) return true;
                Vec3 start = galath.prevRenderPos;
                Vec3 mid   = galath.interpTarget;
                if (start == null) return false;
                Vec3 outDir = mid.subtract(start);
                Vec3 peak   = new Vec3(mid.add(outDir).x, start.y, mid.add(outDir).z);
                return galath.distanceToSqr(peak.x, peak.y, peak.z) < 0.1;
            },
            galath -> {
                galath.interpTarget  = null;
                galath.prevRenderPos = null;
                galath.knockGroundTick = 0;
                galath.getEntityData().set(GalathEntity.DATA_FLOAT, 0.0F);
            },
            true, galath -> true, true
    );

    // =========================================================================
    // Interfaces de Acción y Funcionalidad del Enum
    // =========================================================================

    public interface GalathCallback { void execute(GalathEntity galath); }
    public interface GalathActionCallback { void execute(GalathEntity galath); }
    public interface GalathAttackPredicate { boolean test(GalathEntity galath); }
    public interface NpcActionCallback { void execute(GalathEntity galath); }
    public interface GalathPredicate { boolean test(GalathEntity galath); }

    public final GalathCallback        startCallback;
    public final GalathActionCallback  tickCallback;
    public final GalathAttackPredicate canCompleteTest;
    public final NpcActionCallback     resetCallback;
    public final boolean               applyAttackCoolDown;
    public final GalathPredicate       canUseTest;
    public final boolean               onlyDoThisOnPlayers;

    GalathAttackState(GalathCallback start,
                      GalathActionCallback tick,
                      GalathAttackPredicate canComplete,
                      NpcActionCallback reset,
                      boolean applyAttackCoolDown,
                      GalathPredicate canUse,
                      boolean onlyDoThisOnPlayers) {
        this.startCallback       = start;
        this.tickCallback        = tick;
        this.canCompleteTest     = canComplete;
        this.resetCallback       = reset;
        this.applyAttackCoolDown = applyAttackCoolDown;
        this.canUseTest          = canUse;
        this.onlyDoThisOnPlayers = onlyDoThisOnPlayers;
    }

    public void startAction(GalathEntity galath)  { startCallback.execute(galath); }
    public void tickAction(GalathEntity galath)   { tickCallback.execute(galath); }
    public boolean canComplete(GalathEntity galath) { return canCompleteTest.test(galath); }
    public void resetAction(GalathEntity galath)  { resetCallback.execute(galath); }
    public boolean canUse(GalathEntity galath)    { return canUseTest.test(galath); }
}