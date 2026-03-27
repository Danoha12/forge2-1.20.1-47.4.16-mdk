package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcModelCodeEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

/**
 * NpcModelCodeEntity - ported from e4.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Abstract entity layer that carries three synced values the NPC uses to describe
 * its current model-code state:
 *
 *   N  (id 119, String)   - "new" / pending model-code segment
 *   K  (id 120, BlockPos) - home / reference position
 *   M  (id 121, String)   - current model-code string
 *
 * On the client side, each tick the current values are compared to the cached
 * previous values (P, L, O). If anything changed, the abstract method {@link #a()}
 * is called so subclasses can react (rebuild geometry, reload textures, etc.).
 *
 * Model-code helpers:
 *   {@link #appendFixed(StringBuilder, int)} - appends a zero-padded 2-digit int + "-"
 *   {@link #appendRandom(StringBuilder, int)} - appends a random 0..max value + "-"
 *   {@link #appendGaussian(StringBuilder)}    - appends a 0-1 gaussian-bell decimal + "-"
 *   {@link #appendRandomExcluding(StringBuilder, int)} - same as appendRandom but max excluded
 *   {@link #parseCode(BaseNpcEntity)}          - splits the M data-param on "-"
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityDataManager.func_187226_a - SynchedEntityData.defineId
 *   - DataSerializers.field_187194_d - EntityDataSerializers.STRING
 *   - DataSerializers.field_187200_j - EntityDataSerializers.BLOCK_POS
 *   - func_70088_a() - defineSynchedData()
 *   - field_70170_p.field_72995_K - level.isClientSide()
 *   - gj - FakeWorld (if instanceof FakeWorld, skip define)
 *   - func_70071_h_() - tick()
 *   - m.func_187225_a(param) - entityData.get(param)
 *   - m.func_187214_a(param, val) - entityData.define(param, val)
 *   - r.f.nextInt / nextDouble - random.nextInt / nextDouble
 */
public abstract class NpcModelCodeEntity extends BaseNpcEntity {

    // -- Synced data ------------------------------------------------------------

    /** Pending model-code segment (synced). */
    public static final EntityDataAccessor<String>   MODEL_PENDING =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.STRING);
    /** Home / reference block position (synced). */
    public static final EntityDataAccessor<BlockPos> HOME_POS      =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.BLOCK_POS);
    /** Current model-code string (synced). */
    public static final EntityDataAccessor<String>   MODEL_CODE    =
            SynchedEntityData.defineId(NpcModelCodeEntity.class, EntityDataSerializers.STRING);

    // -- Canonical aliases matching original e4 field names (N, K, M) ----------
    /** e4.N (id 119) - pending model-code / body-color token. */
    public static final EntityDataAccessor<String>   N          = MODEL_PENDING;
    /** e4.K (id 120) - home / eye-color reference as BlockPos. */
    public static final EntityDataAccessor<BlockPos> K          = HOME_POS;
    /** e4.M (id 121) - current model-code string. */
    public static final EntityDataAccessor<String>   M          = MODEL_CODE;
    /** Alias: body-color name string (same as N / MODEL_PENDING). */
    public static final EntityDataAccessor<String>   BODY_COLOR = MODEL_PENDING;

    // -- Client-side change-detection cache -------------------------------------
    String  prevPending  = null;
    String  prevCode     = null;
    BlockPos prevHomePos = null;

    // -- Constructor ------------------------------------------------------------

    protected NpcModelCodeEntity(
            net.minecraft.world.entity.EntityType<? extends NpcModelCodeEntity> type,
            Level level) {
        super(type, level);
    }

    // -- Synced data ------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Skip define in FakeWorld (client-side preview world)
        if (level.isClientSide() && level instanceof FakeWorld) return;
        this.entityData.define(MODEL_CODE, buildInitialCode(new StringBuilder()));
    }

    // -- Tick -------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        detectModelCodeChange();
    }

    /**
     * Checks whether any of the three synced values changed since the last tick.
     * If so, calls {@link #onModelCodeChanged()} so subclasses can react.
     * Only runs on the client.
     */
    void detectModelCodeChange() {
        if (!level.isClientSide()) return;

        String   pending  = this.entityData.get(MODEL_PENDING);
        String   code     = this.entityData.get(MODEL_CODE);
        BlockPos homePos  = this.entityData.get(HOME_POS);

        if (prevPending == null) {
            prevPending  = pending;
            prevCode     = code;
            prevHomePos  = homePos;
            return;
        }

        boolean changed = !prevCode.equals(code)
                || !prevPending.equals(pending)
                || !prevHomePos.equals(homePos);

        prevPending  = pending;
        prevCode     = code;
        prevHomePos  = homePos;

        if (changed) onModelCodeChanged();
    }

    // -- Abstract interface -----------------------------------------------------

    /** Called on the client when any model-code value changes. */
    protected abstract void onModelCodeChanged();

    /** Builds and returns the initial model-code string for this entity type. */
    protected abstract String buildInitialCode(StringBuilder sb);

    // -- Model-code builder helpers ---------------------------------------------

    /** Appends a zero-padded 2-digit fixed value + "-" to the builder. */
    public static void appendFixed(StringBuilder sb, int value) {
        if (value < 10) sb.append(0);
        sb.append(value).append("-");
    }

    /** Appends a random value in [0, max] (inclusive) + "-". */
    public static void appendRandom(StringBuilder sb, int max) {
        int v = net.minecraft.util.RandomSource.create().nextIntBetweenInclusive(0, max);
        if (v < 10) sb.append(0);
        sb.append(v).append("-");
    }

    /** Appends a 2-decimal gaussian bell value (- 0-1 range) + "-". */
    public static void appendGaussian(StringBuilder sb) {
        double u = net.minecraft.util.RandomSource.create().nextDouble();
        double g = Math.pow(Math.E, -Math.pow(-2.5D + 5.0D * u, 2.0D));
        String s = String.format("%.2f", g);
        String[] parts = s.split("[.,]");
        sb.append(parts.length < 2 ? s : parts[1]).append("-");
    }

    /** Appends a random value in [0, max) (exclusive) + "-". */
    public static void appendRandomExcluding(StringBuilder sb, int max) {
        int v = net.minecraft.util.RandomSource.create().nextInt(max);
        if (v < 10) sb.append(0);
        sb.append(v).append("-");
    }

    /**
     * Splits the MODEL_CODE synced value on "-" and returns the parts.
     * Mirrors original e4.a(em) static method.
     */
    public static String[] parseCode(BaseNpcEntity entity) {
        return entity.getEntityData().get(MODEL_CODE).split("-");
    }
}
