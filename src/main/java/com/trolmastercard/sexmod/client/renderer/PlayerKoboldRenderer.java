package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * PlayerKoboldRenderer (dk) - Ported from 1.12.2 to 1.20.1.
 *
 * Trivial {@link BaseNpcRenderer} for {@link PlayerKoboldEntity}.
 * No custom overrides - inherits all rendering from {@link BaseNpcRenderer}
 * (skin texture from player UUID, bone colour from NpcColoredRenderer chain).
 *
 * In 1.12.2 this extended {@code d_<el>} (BaseNpcRenderer<PlayerKoboldEntity>)
 * and forwarded both constructor args directly to super.
 */
public class PlayerKoboldRenderer extends BaseNpcRenderer<PlayerKoboldEntity> {

    public PlayerKoboldRenderer(EntityRendererProvider.Context context,
                                 GeoModel<PlayerKoboldEntity> model,
                                 double shadowRadius) {
        super(context, model, shadowRadius);
    }
}
