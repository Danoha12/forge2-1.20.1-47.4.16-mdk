package com.trolmastercard.sexmod.client.model;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.HashSet;

/**
 * IBoneFilter - ported from c3.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * Interface for NPC models that need to filter out specific bones during
 * clothing-overlay rendering.  Used alongside {@link BaseNpcModel} (cv) to
 * determine which bones should be skipped when rendering armor/clothing overlays.
 *
 * Two default methods:
 *   {@link #getHiddenBoneNames()} - returns the set of bone names to hide;
 *       defaults to the global empty set {@code NpcBoneRegistry.EMPTY_SET} (gx.a).
 *   {@link #shouldRenderBone(HashSet, CoreGeoBone)} - walks the bone's parent
 *       chain and returns false if any ancestor name is in the hidden set OR
 *       starts with "armor".
 *
 * Field mapping:
 *   gx.a = NpcBoneRegistry.EMPTY_SET (static shared empty HashSet<String>)
 *
 * GeckoLib 3 - 4:
 *   GeoBone - CoreGeoBone
 *   GeoBone.parent - CoreGeoBone.getParent()
 *   GeoBone.getName() - CoreGeoBone.getName()
 */
public interface IBoneFilter {

    /**
     * Returns the set of bone names that should be hidden / skipped.
     * Default: empty set (no filtering).
     * Original: {@code c3.a() - gx.a}
     */
    default HashSet<String> getHiddenBoneNames() {
        return NpcBoneRegistry.EMPTY_SET;
    }

    /**
     * Returns {@code true} if {@code bone} (and its hierarchy) should be rendered.
     * Walks up the parent chain; returns {@code false} if any ancestor's name is
     * found in {@code hiddenNames} or starts with {@code "armor"}.
     *
     * Original: {@code c3.a(HashSet<String>, GeoBone)}
     */
    default boolean shouldRenderBone(HashSet<String> hiddenNames, CoreGeoBone bone) {
        CoreGeoBone current = bone;
        while (current.getParent() != null) {
            String name = current.getName();
            if (hiddenNames.contains(name)) return false;
            if (name.startsWith("armor"))   return false;
            current = current.getParent();
        }
        return true;
    }
}
