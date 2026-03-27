package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ported from dn.java (1.12.2 - 1.20.1)
 * Minimal BaseNpcRenderer wrapper for the "fz" entity type.
 *
 * Original: {@code class dn extends d_<fz>}
 * TODO: replace {@code BaseNpcEntity} with the actual fz entity class once identified.
 */
public class FzEntityRenderer extends BaseNpcRenderer<BaseNpcEntity> {

    public FzEntityRenderer(EntityRendererProvider.Context context,
                              GeoModel<BaseNpcEntity> model,
                              double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
