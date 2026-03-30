package com.trolmastercard.sexmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * GirlSpecificEntity — Portado a 1.20.1.
 * * Entidad abstracta intermedia que añade datos específicos de las chicas
 * * (apariencia, posición de casa, y datos delimitados por guiones "-").
 * * * * Jerarquía: GirlSpecificEntity -> PlayerKoboldEntity -> BaseNpcEntity -> ...
 */
public abstract class GirlSpecificEntity extends PlayerKoboldEntity {

    // ── DataParameters ────────────────────────────────────────────────────────

    public static final EntityDataAccessor<String> APPEARANCE_DATA =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<BlockPos> HOME_POS =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.BLOCK_POS);

    public static final EntityDataAccessor<String> GIRL_SPECIFIC_DATA =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.STRING);

    // ── Estado ────────────────────────────────────────────────────────────────

    private boolean pendingLoad = true;

    // ── Constructores ─────────────────────────────────────────────────────────

    protected GirlSpecificEntity(EntityType<? extends GirlSpecificEntity> type, Level level) {
        super(type, level);
    }

    protected GirlSpecificEntity(EntityType<? extends GirlSpecificEntity> type, Level level, UUID ownerUUID) {
        super(type, level, ownerUUID);
    }

    // ── Registro de Datos ─────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(APPEARANCE_DATA, "");
        entityData.define(HOME_POS, BlockPos.ZERO);
        entityData.define(GIRL_SPECIFIC_DATA, buildInitialSpecificData(new StringBuilder()));
    }

    protected abstract String buildInitialSpecificData(StringBuilder sb);

    // ── Tick y Sincronización ─────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (!pendingLoad) return;

        if (level().isClientSide()) {
            onClientFirstTick();
            pendingLoad = false; // Ya no necesitamos que se quede trabado en true
            return;
        }

        // Servidor: extrae datos específicos del NBT persistente del jugador dueño
        var ownerPlayer = getOwnerPlayer();
        if (ownerPlayer == null) return;

        // Forge 1.20.1: getPersistentData() es la forma correcta
        String tag = ownerPlayer.getPersistentData()
                .getString("sexmod:GirlSpecific" + NpcType.fromEntity(this));

        pendingLoad = false;
        if (!tag.isEmpty()) {
            loadSpecificData(splitData(tag));
        }
    }

    protected void onClientFirstTick() {}

    // ── Detección de Cambios (Optimizado 1.20.1) ──────────────────────────────

    /**
     * ¡Magia de la 1.20.1! Este método nativo se dispara SOLO cuando el servidor
     * envía un cambio en alguna de las variables. Adiós al lag por revisar Strings cada tick.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (this.level().isClientSide()) {
            if (APPEARANCE_DATA.equals(key) || GIRL_SPECIFIC_DATA.equals(key) || HOME_POS.equals(key)) {
                onDataChanged();
            }
        }
    }

    /**
     * Se llama cuando cambia alguno de los tres DataParameters en el cliente.
     * Las subclases lo sobrescriben para aplicar cambios visuales.
     */
    protected abstract void onDataChanged();

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Retorna la personalización como un String[] dividido por "-".
     * CRÍTICO: Ahora exige un GirlSpecificEntity para evitar crasheos de ClassCast.
     */
    public static String[] getCustomisationParts(GirlSpecificEntity entity) {
        return entity.entityData.get(GIRL_SPECIFIC_DATA).split("-");
    }

    protected abstract void loadSpecificData(String[] parts);

    // ── Internos ──────────────────────────────────────────────────────────────

    private String[] splitData(String raw) {
        return raw.split("-");
    }

    private net.minecraft.world.entity.player.Player getOwnerPlayer() {
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) return null;
        return level().getPlayerByUUID(ownerUUID);
    }
}