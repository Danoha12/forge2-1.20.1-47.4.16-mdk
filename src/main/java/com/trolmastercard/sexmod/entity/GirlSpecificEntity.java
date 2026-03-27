package com.trolmastercard.sexmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * GirlSpecificEntity - Clase intermedia de personalización.
 * Portado a 1.20.1.
 * * Maneja los DataParameters específicos de apariencia, hogar y datos de tipo.
 * * Gestiona la sincronización inicial de datos desde el NBT persistente del jugador dueño.
 */
public abstract class GirlSpecificEntity extends PlayerKoboldEntity {

    // =========================================================================
    //  DataParameters Sincronizados
    // =========================================================================

    /** Datos de vestimenta y overrides de huesos (String delimitado por "-"). */
    public static final EntityDataAccessor<String> APPEARANCE_DATA =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.STRING);

    /** Posición del bloque "Home" para la IA de retorno. */
    public static final EntityDataAccessor<BlockPos> HOME_POS =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.BLOCK_POS);

    /** Datos específicos de la entidad (Configuraciones internas, delimitado por "-"). */
    public static final EntityDataAccessor<String> GIRL_SPECIFIC_DATA =
            SynchedEntityData.defineId(GirlSpecificEntity.class, EntityDataSerializers.STRING);

    // =========================================================================
    //  Estado y Cache de Sincronización
    // =========================================================================

    private boolean pendingLoad = true;
    private String  cachedAppearance = null;
    private String  cachedSpecific   = null;
    private BlockPos cachedHome      = null;

    // =========================================================================
    //  Constructores
    // =========================================================================

    protected GirlSpecificEntity(EntityType<? extends GirlSpecificEntity> type, Level level) {
        super(type, level);
    }

    protected GirlSpecificEntity(EntityType<? extends GirlSpecificEntity> type, Level level, UUID ownerUUID) {
        super(type, level, ownerUUID);
    }

    // =========================================================================
    //  Inicialización de Datos
    // =========================================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(APPEARANCE_DATA, "");
        entityData.define(HOME_POS, BlockPos.ZERO);
        entityData.define(GIRL_SPECIFIC_DATA, buildInitialSpecificData(new StringBuilder()));
    }

    /** Construye el String inicial de datos específicos para la subclase. */
    protected abstract String buildInitialSpecificData(StringBuilder sb);

    // =========================================================================
    //  Lógica de Tick (Sincronización de Datos)
    // =========================================================================

    @Override
    public void tick() {
        super.tick();
        detectAndSyncChanges();

        if (!pendingLoad) return;

        if (level().isClientSide()) {
            onClientFirstTick();
            // En el cliente mantenemos el estado hasta que el servidor confirme datos
            return;
        }

        // SERVIDOR: Cargar datos desde el jugador dueño
        var ownerPlayer = getOwnerPlayer();
        if (ownerPlayer == null) return;

        // Buscamos los datos persistentes del mod en el NBT del jugador
        String tag = ownerPlayer.getPersistentData()
                .getString("sexmod:GirlSpecific" + getNpcType()); // Asume getNpcType() en base

        pendingLoad = false;
        if (!tag.isEmpty()) {
            loadSpecificData(tag.split("-"));
        }
    }

    /** Efectos visuales o de sonido al aparecer en el cliente por primera vez. */
    protected void onClientFirstTick() {}

    // =========================================================================
    //  Detección de Cambios Visuales
    // =========================================================================

    /**
     * Observa los DataParameters. Si cambian (desde el servidor),
     * activa onDataChanged en el cliente para actualizar el modelo.
     */
    private void detectAndSyncChanges() {
        if (!level().isClientSide()) return;

        String  newApp  = entityData.get(APPEARANCE_DATA);
        String  newSpec = entityData.get(GIRL_SPECIFIC_DATA);
        BlockPos newHome = entityData.get(HOME_POS);

        if (cachedAppearance == null) {
            cachedAppearance = newApp;
            cachedSpecific   = newSpec;
            cachedHome       = newHome;
            return;
        }

        if (!cachedSpecific.equals(newSpec)
                || !cachedAppearance.equals(newApp)
                || !cachedHome.equals(newHome)) {
            onDataChanged();
        }

        cachedAppearance = newApp;
        cachedSpecific   = newSpec;
        cachedHome       = newHome;
    }

    /** Invocado en el cliente cuando los datos visuales/específicos cambian. */
    protected abstract void onDataChanged();

    // =========================================================================
    //  Helpers de Datos
    // =========================================================================

    /** Obtiene los segmentos de personalización de cualquier entidad NPC. */
    public static String[] getCustomisationParts(BaseNpcEntity entity) {
        String data = entity.getEntityData().get(GIRL_SPECIFIC_DATA);
        return data.split("-");
    }

    /** Aplica los segmentos de datos cargados a los campos de la subclase. */
    protected abstract void loadSpecificData(String[] parts);

    private net.minecraft.world.entity.player.Player getOwnerPlayer() {
        UUID ownerUUID = getOwnerUUID();
        return (ownerUUID == null) ? null : level().getPlayerByUUID(ownerUUID);
    }
}