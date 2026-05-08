package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;

/**
 * IBoneFilter — Contrato para la visibilidad de piezas del modelo.
 */
public interface IBoneFilter {
    /**
     * @param entity La entidad que se está dibujando.
     * @param boneName El nombre del hueso en el archivo .geo.json.
     * @return true si el hueso es visible, false para ocultarlo.
     */
    boolean isBoneVisible(BaseNpcEntity entity, String boneName);
}