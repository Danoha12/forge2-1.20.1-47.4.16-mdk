package com.trolmastercard.sexmod.client.renderer; // Ajusta al paquete correcto

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.Set;

/**
 * NsfwBoneHidingRenderer — Portado a 1.20.1.
 * * Renderizador que siempre oculta los huesos NSFW, ignorando la customización.
 * * Ideal para Kobolds salvajes o NPCs no domados.
 */
public class NsfwBoneHidingRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    // 🚨 1.20.1: Usamos Set.of() de Java 9+ guardado en una constante estática.
    // Esto evita asignar memoria nueva en cada frame del renderizado.
    private static final Set<String> NSFW_BONES = Set.of(
            "boobs", "booty", "vagina", "fuckhole", "leaf7", "leaf8"
    );

    public NsfwBoneHidingRenderer(EntityRendererProvider.Context context,
                                  GeoModel<T> model,
                                  double shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** * Devuelve el set fijo de huesos ocultos.
     * Sobrescribe la visibilidad impulsada por el modelo base.
     */
    @Override
    public Set<String> getAlwaysHiddenBones() {
        return NSFW_BONES;
    }
}