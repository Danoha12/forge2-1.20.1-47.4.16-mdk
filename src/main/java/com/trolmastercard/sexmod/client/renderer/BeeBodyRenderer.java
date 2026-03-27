package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.BeeNpcEntity;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * BeeBodyRenderer (dt) - Ported from 1.12.2 to 1.20.1.
 *
 * Full-body {@link BaseNpcRenderer} for the Bee NPC.
 * Overrides the NSFW hidden-bone set to censor "leaf7" and "leaf8"
 * (the bee's leaf-clothing bones) in addition to the standard
 * explicit body-part bones.
 *
 * Hidden bones: "boobs", "booty", "vagina", "fuckhole", "leaf7", "leaf8"
 *
 * 1.12.2 - 1.20.1:
 *   - {@code class dt extends d_} - {@code class BeeBodyRenderer extends BaseNpcRenderer<BeeNpcEntity>}
 *   - Inner anonymous {@code HashSet} - named inner class {@link HiddenBoneSet}
 *   - {@code public HashSet<String> a()} - {@link #getExtraHiddenBones()}
 *     (called from BaseNpcRenderer during frame setup)
 */
public class BeeBodyRenderer extends BaseNpcRenderer<BeeNpcEntity> {

    public BeeBodyRenderer(GeoModel<BeeNpcEntity> model) {
        super(model);
    }

    /**
     * Returns the set of bones that should always be invisible for this renderer,
     * regardless of the entity's clothing state.
     *
     * Original: {@code public HashSet<String> a()} returning {@code new a()} inner class.
     */
    @Override
    public HashSet<String> getExtraHiddenBones() {
        return new HiddenBoneSet();
    }

    // -------------------------------------------------------------------------
    //  Hidden-bone definitions
    // -------------------------------------------------------------------------

    /** Bones that are always culled from the Bee NPC's body render. */
    private static class HiddenBoneSet extends HashSet<String> {
        HiddenBoneSet() {
            add("boobs");
            add("booty");
            add("vagina");
            add("fuckhole");
            add("leaf7");
            add("leaf8");
        }
    }
}
