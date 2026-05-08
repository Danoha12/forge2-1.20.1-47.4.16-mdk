package com.trolmastercard.sexmod.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;

/**
 * NpcDataKeys — Registro central de llaves de datos sincronizados.
 */
public class NpcDataKeys {
    // Ejemplo de llaves comunes. Ajusta según lo que tus renderers pidan.
    public static final EntityDataAccessor<Integer> OUTFIT_ID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_INTERACTIVE = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> PLASURE_LEVEL = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
}