package com.trolmastercard.sexmod.client.model;

import net.minecraft.client.model.geom.ModelPart;

/**
 * IBoneAccessor - replaces the obfuscated {@code at} interface from 1.12.2.
 *
 * Any model that needs to expose bone-rotation helpers and a root-bone accessor
 * should implement this interface.
 */
public interface IBoneAccessor {

    /**
     * Sets the local-space rotation angles on the given {@link ModelPart}.
     *
     * @param part  the part to rotate
     * @param xRot  X rotation in radians
     * @param yRot  Y rotation in radians
     * @param zRot  Z rotation in radians
     */
    void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot);

    /**
     * Returns the root (or primary) bone of the model.
     */
    ModelPart getRootBone();
}
