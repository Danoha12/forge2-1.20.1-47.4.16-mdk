package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete de renderizadores

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity; // Asegúrate de tener esta clase
import com.trolmastercard.sexmod.client.model.PlayerKoboldModel; // Asumiendo que le pasarás el modelo
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * PlayerKoboldRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador trivial para PlayerKoboldEntity.
 * * Toda la lógica pesada (textura del jugador, colores, físicas)
 * * es heredada directamente de BaseNpcRenderer. ¡Herencia perfecta!
 */
public class PlayerKoboldRenderer extends BaseNpcRenderer<PlayerKoboldEntity> {

    public PlayerKoboldRenderer(EntityRendererProvider.Context context,
                                GeoModel<PlayerKoboldEntity> model,
                                double shadowRadius) {
        super(context, model, shadowRadius);
    }
}