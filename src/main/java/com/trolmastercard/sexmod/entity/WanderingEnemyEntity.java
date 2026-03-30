package com.trolmastercard.sexmod.entity; // Ajusta a tu paquete de entidades

import com.trolmastercard.sexmod.registry.ModSounds; // Asegúrate de tener este registro
import com.trolmastercard.sexmod.util.LightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * WanderingEnemyEntity — Portado a 1.20.1.
 * * Entidad autónoma que vaga aleatoriamente.
 * * Se detiene si un jugador se acerca.
 * * Reproduce una alarma y programa su desaparición si es golpeada.
 */
public class WanderingEnemyEntity extends Mob {

    public static final float PLAYER_STOP_RANGE = 3.0F;
    static final int SUMMON_TICKS = 175;
    static final int WANDER_RADIUS = 10;

    // ── Estado ───────────────────────────────────────────────────────────────
    private BlockPos wanderTarget = null;
    private int stuckTicks = 0;
    private boolean beingRemoved = false;

    // Timer interno robusto para no depender de utilidades en RAM que se borran al reiniciar
    private int removalTimer = -1;

    public int hitTick = -1; // Usado por el renderizador del cliente

    public WanderingEnemyEntity(EntityType<? extends WanderingEnemyEntity> type, Level level) {
        super(type, level);
    }

    // ── Ciclo de Vida (Tick) ─────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // 🛡️ Timer de desaparición integrado y seguro para reinicios
        if (beingRemoved && !this.level().isClientSide()) {
            if (removalTimer > 0) {
                removalTimer--;
            } else if (removalTimer == 0) {
                this.discard();
                return; // Evita seguir ejecutando la IA
            }
        }

        if (!this.level().isClientSide()) {
            wander();
        }
    }

    // ── Lógica de IA Original ────────────────────────────────────────────────

    private void wander() {
        if (beingRemoved) {
            getNavigation().stop();
            return;
        }

        // Detenerse si hay un jugador cerca
        Player nearby = level().getNearestPlayer(this, 15.0D);
        if (nearby != null && nearby.distanceTo(this) < PLAYER_STOP_RANGE) {
            getNavigation().stop();
            return;
        }

        // Caminar hacia el objetivo o escoger uno nuevo
        if (wanderTarget != null) {
            double distSq = wanderTarget.distToCenterSqr(getX(), getY(), getZ());
            if (distSq > 4.0D && stuckTicks <= SUMMON_TICKS) {
                getNavigation().moveTo(wanderTarget.getX(), wanderTarget.getY(), wanderTarget.getZ(), 0.35D);
                bounce();
                stuckTicks++;
                return;
            }
        }

        // Elegir nuevo destino aleatorio
        int dx = (getRandom().nextBoolean() ? 1 : -1) * getRandom().nextInt(WANDER_RADIUS);
        int dz = (getRandom().nextBoolean() ? 1 : -1) * getRandom().nextInt(WANDER_RADIUS);

        int targetY;
        if (level().dimensionType().hasCeiling()) {
            targetY = (int) Math.ceil(getY());
        } else {
            int wx = blockPosition().getX() + dx;
            int wz = blockPosition().getZ() + dz;
            targetY = LightUtil.getSurfaceY(level(), wx, wz); // Asumiendo que tu utilidad está porteada
        }

        wanderTarget = new BlockPos(blockPosition().getX() + dx, targetY, blockPosition().getZ() + dz);
        stuckTicks = 0;
    }

    protected void bounce() {
        var path = getNavigation().getPath();
        if (path == null || isInWater() || !onGround()) return;

        int current = path.getNextNodeIndex();
        int end = path.getNodeCount();
        if (end == current || end - 1 == current) return;

        var p1 = path.getNode(current);
        var p2 = path.getNode(current + 1);

        Vec3 dir = new Vec3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
        // Pequeño impulso de salto al caminar
        setDeltaMovement(dir.x / 7.0D, dir.y / 7.0D, dir.z / 7.0D);
    }

    // ── Sistema de Daño ──────────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Muerte instantánea por vacío
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) { // 1.20.1 outOfWorld tag
            this.discard();
            return true;
        }

        // Solo reacciona a jugadores
        if (!(source.getEntity() instanceof Player)) return false;

        if (this.level().isClientSide()) {
            // 🛡️ Aislamiento seguro del cliente
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::playAlarm);
        } else {
            // Lado del servidor: activar la huida/desaparición
            if (!beingRemoved) {
                beingRemoved = true;
                removalTimer = 6250; // Aprox 5 minutos en ticks
            }
        }

        // Retornamos false porque la entidad es invulnerable al daño real
        return false;
    }

    private void playAlarm() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            this.hitTick = mc.player.tickCount;
        }
        this.playSound(ModSounds.MISC_WEOWEO_3.get(), 1.0F, 1.0F); // Asumiendo RegistryObject<SoundEvent>
    }

    // ── Generación (Spawning) ────────────────────────────────────────────────

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        // Rata de spawn extremadamente baja (0.1%)
        if (getRandom().nextInt(100) < 1 && getRandom().nextInt(100) < 10) {
            return true;
        }

        this.discard(); // Autodestrucción si falla el roll
        return false;
    }
}