package com.trolmastercard.sexmod;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * NpcSubtypeRenderer (du) - Ported from 1.12.2 to 1.20.1.
 *
 * Trivial {@link BaseNpcRenderer} for a generic NPC subtype entity (ex).
 * No custom overrides.
 *
 * In 1.12.2: {@code class du extends d_<ex>} with no body.
 * "ex" is an entity subtype class (not the NpcSubtype enum).
 */
public class NpcSubtypeRenderer extends BaseNpcRenderer<NpcSubtypeEntity> {

    public NpcSubtypeRenderer(EntityRendererProvider.Context context,
                               GeoModel<NpcSubtypeEntity> model,
                               double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
