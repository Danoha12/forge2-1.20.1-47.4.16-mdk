package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.network.packet.ResetControllerPacket;
import com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.*;
import com.trolmastercard.sexmod.world.damagesource.GalathCombatDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * GalathAttackState — Portado a 1.20.1.
 * Define los comportamientos de combate de Galath: Cambio de posición, Invocación, Ataque de Espada y Asalto.
 */
public enum GalathAttackState {

    // ── CAMBIO DE POSICIÓN ───────────────────────────────────────────────────
    CHANGE_POSITION(
            galath -> {
                var level = galath.level();
                var curPos = galath.blockPosition();
                var target = galath.getTarget();
                if (target == null) return;
                var tgtPos = target.blockPosition();

                List<BlockPos> airCandidates = new ArrayList<>();
                Map<BlockPos, Integer> scored = new TreeMap<>(Comparator.comparingInt(p -> (int) p.distManhattan(tgtPos)));

                // Buscamos un lugar estratégico en un radio de 10 bloques
                for (int dx = -10; dx <= 10; dx++) {
                    for (int dz = -10; dz <= 10; dz++) {
                        BlockPos candidate = curPos.offset(dx, 0, dz);
                        if (!level.isEmptyBlock(candidate.below())) {
                            scored.put(candidate, Math.abs(dx) + Math.abs(dz));
                        } else {
                            airCandidates.add(candidate);
                        }
                    }
                }

                if (!scored.isEmpty()) {
                    var entries = new ArrayList<>(scored.entrySet());
                    int pick = galath.getRandom().nextInt(Math.max(1, entries.size() / 2));
                    galath.targetPos = Vec3.atCenterOf(entries.get(pick).getKey());
                } else if (!airCandidates.isEmpty()) {
                    galath.targetPos = Vec3.atCenterOf(airCandidates.get(galath.getRandom().nextInt(airCandidates.size())));
                }

                galath.setStateCounter(0);
                galath.setAnimState(AnimState.FLY);
                ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> galath), new ResetControllerPacket(galath.getId()));
            },
            galath -> {
                Vec3 currentPos = galath.position();
                Vec3 targetPos = galath.targetPos;
                if (targetPos == null) return;

                int counter = galath.getStateCounter();
                galath.setStateCounter(counter + 1);
                if (counter != 0) return;

                Vec3 diff = targetPos.subtract(currentPos);
                Vec3 norm = diff.normalize();
                galath.setDeltaMovement(norm.x * 0.6, Mth.clamp(diff.y * 0.6, -0.6, 0.6), norm.z * 0.6);
            },
            galath -> galath.getStateCounter() > 23,
            galath -> {
                galath.setDeltaMovement(Vec3.ZERO);
                galath.setStateCounter(0);
            },
            false, galath -> true, false
    ),

    // ── INVOCACIÓN DE ESQUELETOS (Energy Balls) ──────────────────────────────
    SUMMON_SKELETON(
            galath -> {
                galath.setAnimState(AnimState.SUMMON_SKELETON);
                galath.skeletonSpawnTimer = 0;
                galath.getEntityData().set(GalathEntity.DATA_EB_L, true);
                galath.getEntityData().set(GalathEntity.DATA_EB_R, true);
                galath.getEntityData().set(GalathEntity.DATA_MIRRORED, galath.getRandom().nextBoolean());
                BaseNpcEntity.sendNpcSound(galath, ModSounds.GIRLS_GALATH_STRONGCHARGE, true);
            },
            galath -> {
                galath.setDeltaMovement(Vec3.ZERO);
                if (galath.skeletonSpawnTimer != 30) return;

                Vec3 targetPos = galath.getTarget().position();
                boolean mirrored = galath.getEntityData().get(GalathEntity.DATA_MIRRORED);

                // Disparo desde el ala derecha
                if (galath.getEntityData().get(GalathEntity.DATA_EB_R)) {
                    spawnEnergyBall(galath, targetPos, mirrored ? GalathEntity.ENERGY_BALL_L_OFFSET : GalathEntity.ENERGY_BALL_R_OFFSET);
                }
                // Disparo desde el ala izquierda
                if (galath.getEntityData().get(GalathEntity.DATA_EB_L)) {
                    spawnEnergyBall(galath, targetPos, mirrored ? GalathEntity.ENERGY_BALL_R_OFFSET : GalathEntity.ENERGY_BALL_L_OFFSET);
                }
            },
            galath -> galath.skeletonSpawnTimer >= 45,
            galath -> galath.skeletonSpawnTimer = 0,
            true, galath -> galath.skeletons.size() < 2, true
    ),

    // ── ATAQUE CON ESPADA ────────────────────────────────────────────────────
    ATTACK_SWORD(
            galath -> {
                galath.setAttackAnimIdx(0);
                galath.setAnimState(AnimState.ATTACK_SWORD);
                galath.setDeltaMovement(Vec3.ZERO);
                galath.setChargeStartPos(galath.position());

                Vec3 targetPos = galath.getTarget().position();
                double yaw = Math.toDegrees(Math.atan2(targetPos.x - galath.getX(), targetPos.z - galath.getZ())) - 90.0;
                galath.setYRot((float) yaw);
                BaseNpcEntity.sendNpcSound(galath, ModSounds.GIRLS_GALATH_STRONGCHARGE, true);
            },
            galath -> {
                var target = galath.getTarget();
                int phase = galath.getAttackAnimIdx() + 1;
                galath.setAttackAnimIdx(phase);

                if (phase >= 24 && phase <= 32) {
                    // Deslizamiento hacia el objetivo
                    Vec3 tgtPos = target.position().add(0, target.getEyeHeight(), 0);
                    float t = (phase - 24) / 8.0F;
                    galath.setPos(MathUtil.lerpVec3(galath.getChargeStartPos(), tgtPos.add(VectorMathUtil.rotateVec(new Vec3(0,0,3), galath.getYRot() + 180)), t));
                } else if (phase > 32 && phase <= 54) {
                    // Ataque y daño
                    galath.setPos(target.position().add(VectorMathUtil.rotateVec(new Vec3(0,0,1.5), galath.getYRot() + 180)));
                    if (phase == 36 || phase == 40) {
                        target.hurt(new GalathCombatDamageSource(galath), 5.0F);
                        target.invulnerableTime = 0;
                    }
                } else if (phase == 54) {
                    galath.setAnimState(AnimState.FLY);
                    galath.setStateCounter(1);
                }
            },
            galath -> galath.getStateCounter() > 23,
            galath -> {
                galath.setStateCounter(0);
                galath.setAttackAnimIdx(-1);
            },
            true, galath -> true, false
    ),

    // ── ASALTO (RAPE) ────────────────────────────────────────────────────────
    RAPE(
            galath -> {
                galath.setAnimState(AnimState.RAPE_PREPARE);
                galath.summonBoneTick = 0; // Usado como timer de rape
            },
            galath -> {
                if (++galath.summonBoneTick < 48) return;

                galath.setAnimState(AnimState.RAPE_INTRO);
                var target = galath.getTarget();
                if (galath.interpTarget == null) { // Usamos interpTarget como flag de inicio de carga
                    galath.interpTarget = target.position();
                    galath.setChargeStartPos(galath.position());
                }

                // Detección de agarre
                AABB grabBox = galath.getBoundingBox().inflate(0.2);
                for (Player player : galath.level().getEntitiesOfClass(Player.class, grabBox)) {
                    if (player.isCreative() || player.isSpectator() || !player.onGround()) continue;

                    // Limpiar esbirros antes de la escena
                    galath.skeletons.forEach(Entity::discard);
                    galath.skeletons.clear();

                    if (player instanceof ServerPlayer sp) {
                        galath.setPos(sp.position());
                        galath.setSexPartnerUUID(sp.getUUID());
                        galath.setAnimState(AnimState.RAPE_ON_GOING);

                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
                        sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), Vec3.ZERO));
                        sp.connection.send(new ClientboundMoveEntityPacket.Rot(sp.getId(),
                                (byte) Mth.floor((galath.getYRot() + 180.0F) * 256.0F / 360.0F), (byte) (-14), true));
                    }
                    return;
                }

                // Trayectoria Bézier de aproximación
                double t = Math.min(1.0, (galath.summonBoneTick - 48) / 20.0);
                Vec3 start = galath.getChargeStartPos();
                Vec3 targetPos = target.position();
                Vec3 mid = start.add(targetPos).scale(0.5).add(0, 2, 0); // Punto de control para arco

                Vec3 nextPos = MathUtil.bezier(start, mid, targetPos, (float) t);
                galath.setPos(nextPos.x, nextPos.y, nextPos.z);
            },
            galath -> galath.getAnimState() == AnimState.RAPE_ON_GOING || galath.summonBoneTick > 80,
            galath -> {
                galath.summonBoneTick = 0;
                galath.interpTarget = null;
            },
            true, galath -> true, true
    );

    // ── CAMPOS Y CONSTRUCTOR ─────────────────────────────────────────────────

    public final GalathCallback startCallback;
    public final GalathActionCallback tickCallback;
    public final GalathAttackPredicate canCompleteTest;
    public final NpcActionCallback resetCallback;
    public final boolean applyAttackCoolDown;
    public final GalathPredicate canUseTest;
    public final boolean onlyDoThisOnPlayers;

    GalathAttackState(GalathCallback start, GalathActionCallback tick, GalathAttackPredicate canComplete,
                      NpcActionCallback reset, boolean applyAttackCoolDown, GalathPredicate canUse, boolean onlyDoThisOnPlayers) {
        this.startCallback = start;
        this.tickCallback = tick;
        this.canCompleteTest = canComplete;
        this.resetCallback = reset;
        this.applyAttackCoolDown = applyAttackCoolDown;
        this.canUseTest = canUse;
        this.onlyDoThisOnPlayers = onlyDoThisOnPlayers;
    }

    // ── MÉTODOS DE APOYO ─────────────────────────────────────────────────────

    private static void spawnEnergyBall(GalathEntity galath, Vec3 targetPos, Vec3 boneOffset) {
        Vec3 spawnPos = galath.position().add(VectorMathUtil.rotateVec(boneOffset, 180.0F + galath.getYRot()));
        Vec3 dir = targetPos.subtract(spawnPos).normalize().add(galath.getRandom().nextGaussian() * 0.1, 0, galath.getRandom().nextGaussian() * 0.1);
        EnergyBallEntity ball = new EnergyBallEntity(galath.level(), galath, dir.scale(0.4));
        ball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        galath.level().addFreshEntity(ball);
    }

    public void start(GalathEntity galath) { startCallback.execute(galath); }
    public void tick(GalathEntity galath) { tickCallback.execute(galath); }
    public boolean isComplete(GalathEntity galath) { return canCompleteTest.test(galath); }
    public void end(GalathEntity galath) { resetCallback.execute(galath); }
    public boolean canDo(GalathEntity galath) { return canUseTest.test(galath); }
}