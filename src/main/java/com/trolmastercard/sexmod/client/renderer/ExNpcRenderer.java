package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ported from du.java (1.12.2 - 1.20.1)
 * Minimal BaseNpcRenderer wrapper for the "ex" entity type.
 *
 * Original: {@code class du extends d_<ex>}
 * NOTE: "ex" was tentatively mapped to NpcSubtype enum in the obfuscation table,
 *       but this class uses it as a GeoEntity type - it is likely a separate entity class.
 * TODO: replace {@code BaseNpcEntity} with the actual ex entity class once identified.
 */
public class ExNpcRenderer extends BaseNpcRenderer<BaseNpcEntity> {

    public ExNpcRenderer(EntityRendererProvider.Context context,
                          GeoModel<BaseNpcEntity> model,
                          double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
