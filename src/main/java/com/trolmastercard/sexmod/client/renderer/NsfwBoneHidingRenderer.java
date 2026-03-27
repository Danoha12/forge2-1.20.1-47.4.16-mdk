package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from dt.java (1.12.2 - 1.20.1)
 * A BaseNpcRenderer that overrides the hidden-bone set to always hide the NSFW
 * anatomy bones (boobs, booty, vagina, fuckhole, leaf7, leaf8).
 *
 * Useful as the renderer for wild/non-owned kobolds where these bones should
 * never be visible regardless of customization flags.
 *
 * Original: {@code class dt extends d_}
 */
public class NsfwBoneHidingRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    public NsfwBoneHidingRenderer(EntityRendererProvider.Context context,
                                   GeoModel<T> model,
                                   double shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** Returns the fixed set of hidden NSFW bones - overrides model-driven visibility. */
    @Override
    public Set<String> getAlwaysHiddenBones() {
        return new NsfwBoneSet();
    }

    // -- Inner hidden-bone set -------------------------------------------------

    static final class NsfwBoneSet extends HashSet<String> {
        NsfwBoneSet() {
            add("boobs");
            add("booty");
            add("vagina");
            add("fuckhole");
            add("leaf7");
            add("leaf8");
        }
    }
}
