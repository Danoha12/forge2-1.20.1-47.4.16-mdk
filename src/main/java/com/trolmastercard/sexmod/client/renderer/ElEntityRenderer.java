package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ported from dk.java (1.12.2 - 1.20.1)
 * Minimal BaseNpcRenderer wrapper for the "el" entity type.
 *
 * Original: {@code class dk extends d_<el>}
 * TODO: replace {@code BaseNpcEntity} with the actual el entity class once identified.
 */
public class ElEntityRenderer extends BaseNpcRenderer<BaseNpcEntity> {

    public ElEntityRenderer(EntityRendererProvider.Context context,
                             GeoModel<BaseNpcEntity> model,
                             double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
