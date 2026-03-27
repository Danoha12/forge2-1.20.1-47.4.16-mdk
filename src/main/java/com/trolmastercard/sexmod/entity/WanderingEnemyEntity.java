package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.LightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * WanderingEnemyEntity - Portado de al.class (1.12.2) a 1.20.1.
 * Entidad autónoma que deambula, se detiene cerca del jugador,
 * reproduce un sonido de alarma al ser golpeada y desaparece (despawn) tras un tiempo.
 */
public class WanderingEnemyEntity extends Mob {

    public static final long WANDER_TIMEOUT_MS = 60_000L;
    public static final float PLAYER_STOP_RANGE = 3.0F;
    static final float SUMMON_TICKS_F = 30.0F;
    static final int SUMMON_TICKS = 175;
    static final int WANDER_RADIUS = 10;

    // --- Estado ---
    private BlockPos wanderTarget = null;
    private int stuckTicks = 0;
    private boolean beingRemoved = false;

    /** Tick en el que el jugador golpeó a esta entidad (para sonido/tracking en cliente). */
    public int hitTick = -1;

    public WanderingEnemyEntity(EntityType<? extends WanderingEnemyEntity> type, Level level) {
        super(type, level);
    }

    // =========================================================================
    //  Tick & IA Principal
    // =========================================================================

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            wander();
        }
    }

    /** Lógica central de deambulación - Portado de al.a() */
    private void wander() {
        if (this.beingRemoved) {
            this.getNavigation().stop();
            return;
        }

        // Detenerse si hay un jugador cerca
        Player nearby = this.level().getNearestPlayer(this, 15.0D);
        if (nearby != null && nearby.distanceTo(this) < PLAYER_STOP_RANGE) {
            this.getNavigation().stop();
            return;
        }

        // Elegir o dirigirse al objetivo de deambulación
        if (this.wanderTarget != null) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(this.wanderTarget));
            if (distSq > 4.0D && this.stuckTicks <= SUMMON_TICKS) {
                this.getNavigation().moveTo(this.wanderTarget.getX(), this.wanderTarget.getY(), this.wanderTarget.getZ(), 0.35D);
                bounce();
                this.stuckTicks++;
                return;
            }
        }

        // Generar un nuevo objetivo aleatorio si se alcanzó el anterior o se atascó
        int dx = (this.random.nextBoolean() ? 1 : -1) * this.random.nextInt(WANDER_RADIUS);
        int dz = (this.random.nextBoolean() ? 1 : -1) * this.random.nextInt(WANDER_RADIUS);
        int targetY;

        if (this.level().dimensionType().hasCeiling()) {
            targetY = (int) Math.ceil(this.getY()); // Lógica para el Nether
        } else {
            int wx = this.blockPosition().getX() + dx;
            int wz = this.blockPosition().getZ() + dz;
            targetY = LightUtil.getSurfaceY(this.level(), wx, wz);
        }

        this.wanderTarget = new BlockPos(this.blockPosition().getX() + dx, targetY, this.blockPosition().getZ() + dz);
        this.stuckTicks = 0;
    }

    /** Aplica un impulso de salto aleatorio al caminar - Portado de al.d() */
    protected void bounce() {
        Path path = this.getNavigation().getPath();
        if (path == null || this.isInWater() || !this.onGround()) return;

        int current = path.getNextNodeIndex();
        int end = path.getNodeCount();
        if (end == current || end - 1 == current) return;

        Node p1 = path.getNode(current);
        Node p2 = path.getNode(current + 1);

        Vec3 dir = new Vec3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
        this.setDeltaMovement(dir.x / 7.0D, dir.y / 7.0D, dir.z / 7.0D);
    }

    // =========================================================================
    //  Daño y Eventos
    // =========================================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Daño por vacío: eliminar inmediatamente
        if (source.is(this.level().damageSources().outOfWorld().typeHolder().unwrapKey().get())) {
            this.discard();
            return true;
        }

        // Solo recibe daño de jugadores
        if (!(source.getEntity() instanceof Player)) return false;

        // Reproducir sonido de alarma en el cliente
        if (this.level().isClientSide()) {
            playAlarm();
        }

        // Programar eliminación después de 6250 ticks
        this.beingRemoved = true;
        LightUtil.scheduleTask(6250, this::discard);
        return false;
    }

    /** Reproduce el sonido de alarma "weoweo" y guarda el tick del golpe. */
    @OnlyIn(Dist.CLIENT)
    private void playAlarm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.hitTick = mc.player.tickCount;
        }
        this.playSound(ModSounds.MISC_WEOWEO_3.get(), 1.0F, 1.0F);
    }

    // =========================================================================
    //  Reglas de Spawn y Constantes
    // =========================================================================

    /** Probabilidad de spawn muy baja: 1/100 -> 1/10 = 0.1% */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        if (this.random.nextInt(100) < 1 && this.random.nextInt(100) < 10) {
            return super.checkSpawnRules(level, reason);
        }
        this.discard();
        return false;
    }

    /** Retorna el límite de radio de deambulación (sqrt(1800)). */
    public double getWanderRange() {
        return Math.sqrt(1800.0D);
    }
}