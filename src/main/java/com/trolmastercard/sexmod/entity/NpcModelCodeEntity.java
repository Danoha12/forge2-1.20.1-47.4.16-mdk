package com.trolmastercard.sexmod.entity; // Ajusta a tu paquete de entidades

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * NpcModelCodeEntity — Portado a 1.20.1.
 * * Entidad base que maneja el "ADN" o código de personalización del NPC.
 * * Sincroniza el string de modelo entre servidor y cliente.
 */
public abstract class NpcModelCodeEntity extends BaseNpcEntity {

    // ── Synced Data (Registros de Forge 1.20.1) ──────────────────────────────

    public static final EntityDataAccessor<String> MODEL_PENDING =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<BlockPos> HOME_POS =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.BLOCK_POS);

    public static final EntityDataAccessor<String> MODEL_CODE =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.STRING);

    // ── Caché del Cliente para Cambios ───────────────────────────────────────

    private String prevPending = null;
    private String prevCode = null;
    private BlockPos prevHomePos = null;

    // ── Constructor ──────────────────────────────────────────────────────────

    protected NpcModelCodeEntity(EntityType<? extends NpcModelCodeEntity> type, Level level) {
        super(type, level);
    }

    // ── Inicialización Segura (1.20.1) ───────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        // 🚨 TODOS los DataAccessors deben definirse aquí con un valor por defecto.
        // No intentes construir lógicas complejas aquí porque el constructor de la clase hija
        // aún no ha terminado.
        this.entityData.define(MODEL_PENDING, "");
        this.entityData.define(HOME_POS, BlockPos.ZERO);
        this.entityData.define(MODEL_CODE, "");
    }

    /**
     * En 1.20.1, el momento seguro para inicializar datos complejos al generar
     * una nueva entidad es durante finalizeSpawn o un método similar, pero
     * como esto parece ser interno, lo inyectaremos en el primer tick del servidor.
     */
    protected void initializeModelCodeIfNeeded() {
        if (!this.level().isClientSide() && this.entityData.get(MODEL_CODE).isEmpty()) {
            this.entityData.set(MODEL_CODE, buildInitialCode(new StringBuilder()));
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Genera el código inicial solo en el servidor la primera vez
        initializeModelCodeIfNeeded();

        // Detecta cambios en el cliente
        detectModelCodeChange();
    }

    private void detectModelCodeChange() {
        if (!this.level().isClientSide()) return;

        String pending = this.entityData.get(MODEL_PENDING);
        String code = this.entityData.get(MODEL_CODE);
        BlockPos homePos = this.entityData.get(HOME_POS);

        if (this.prevPending == null) {
            this.prevPending = pending;
            this.prevCode = code;
            this.prevHomePos = homePos;
            return;
        }

        boolean changed = !this.prevCode.equals(code)
                || !this.prevPending.equals(pending)
                || !this.prevHomePos.equals(homePos);

        this.prevPending = pending;
        this.prevCode = code;
        this.prevHomePos = homePos;

        if (changed) {
            onModelCodeChanged();
        }
    }

    // ── Abstract interface ───────────────────────────────────────────────────

    protected abstract void onModelCodeChanged();
    protected abstract String buildInitialCode(StringBuilder sb);

    // ── Helpers Matemáticos para Generar ADN ─────────────────────────────────

    private static final RandomSource RANDOM = RandomSource.create();

    public static void appendFixed(StringBuilder sb, int value) {
        if (value < 10) sb.append('0');
        sb.append(value).append("-");
    }

    public static void appendRandom(StringBuilder sb, int max) {
        // En 1.20.1, RandomSource maneja los límites de forma segura
        int v = max <= 0 ? 0 : RANDOM.nextInt(max + 1);
        if (v < 10) sb.append('0');
        sb.append(v).append("-");
    }

    public static void appendRandomExcluding(StringBuilder sb, int max) {
        int v = max <= 0 ? 0 : RANDOM.nextInt(max);
        if (v < 10) sb.append('0');
        sb.append(v).append("-");
    }

    public static void appendGaussian(StringBuilder sb) {
        double u = RANDOM.nextDouble();
        double g = Math.pow(Math.E, -Math.pow(-2.5D + 5.0D * u, 2.0D));
        // String.format en Java puede ser lento si se llama cientos de veces,
        // pero para este caso está bien. Aseguramos el Locale.US para evitar comas
        // en idiomas europeos que rompan el split(".").
        String s = String.format(java.util.Locale.US, "%.2f", g);
        String[] parts = s.split("\\.");
        sb.append(parts.length < 2 ? s : parts[1]).append("-");
    }

    public static String[] parseCode(NpcModelCodeEntity entity) {
        return entity.getEntityData().get(MODEL_CODE).split("-");
    }
}