package com.trolmastercard.sexmod.client.renderer.entity;

import com.trolmastercard.sexmod.client.model.entity.LunaModel;
import com.trolmastercard.sexmod.entity.LunaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * LunaRenderer — Construido para 1.20.1 / GeckoLib 4.
 * * Renderizador base para LunaEntity (previamente CatEntity).
 * * Conecta la entidad lógica con su GeoModel visual.
 */
public class LunaRenderer extends BaseNpcRenderer<LunaEntity> {

    public LunaRenderer(EntityRendererProvider.Context ctx) {
        // Inicializamos el renderizador pasando el contexto de Minecraft y el modelo de Luna
        super(ctx, new LunaModel());

        // El tamaño de la sombra circular bajo sus pies (0.4F es el estándar para bípedos)
        this.shadowRadius = 0.4F;
    }

    // Nota: Si más adelante quieres que Luna renderice el Salmón o el Libro Encantado
    // en su mano (heldVisualItem), puedes sobreescribir el método `onBoneRender` aquí,
    // tal como hicimos con el arco de Manglelie. Por ahora, el renderizador base es perfecto.
}