package com.trolmastercard.sexmod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * MangleLieRenderer (dn) - Ported from 1.12.2 to 1.20.1.
 *
 * Trivial {@link BaseNpcRenderer} for {@link MangleLieEntity} (fz).
 * No custom overrides - all rendering handled by the base class.
 *
 * In 1.12.2: {@code class dn extends d_<fz>} with no body.
 */
public class MangleLieRenderer extends BaseNpcRenderer<MangleLieEntity> {

    public MangleLieRenderer(EntityRendererProvider.Context context,
                              GeoModel<MangleLieEntity> model,
                              double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
