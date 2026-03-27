package com.trolmastercard.sexmod.client.anim;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.EasingType;

import java.util.function.Function;

/**
 * CustomAnimationController - ported from bz.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * A thin subclass of {@link AnimationController} that exposes the three
 * constructor overloads used by the mod's NPC entities:
 *   1. Default transition speed (no easing)
 *   2. Named easing type
 *   3. Custom easing function
 *
 * GeckoLib 3 - 4 API changes:
 *   IAnimatable                        - GeoAnimatable
 *   AnimationController.IAnimationPredicate<T> - AnimationController.AnimationStateHandler<T>
 *   new AnimationController(T, name, speed, predicate) - AnimationController<T>(name, speed, handler)
 *   EasingType constructor param       - same (EasingType still used in GeckoLib 4)
 *   Function<Double,Double> custom ease - same
 *
 * NOTE: In GeckoLib 4, the entity instance is not passed to the constructor;
 *       it is bound when the controller is registered via
 *       {@code AnimatableManager.registerControllers(AnimatableInstanceCache)}.
 */
public class CustomAnimationController<T extends GeoAnimatable>
        extends AnimationController<T> {

    /**
     * Controller with default linear easing.
     *
     * @param name       Unique controller name (e.g. "main", "walk")
     * @param speed      Transition speed (frames)
     * @param handler    Predicate that drives animation selection
     */
    public CustomAnimationController(String name, float speed,
                                     AnimationStateHandler<T> handler) {
        super(name, (int) speed, handler);
    }

    /**
     * Controller with a named {@link EasingType}.
     */
    public CustomAnimationController(String name, float speed,
                                     EasingType easingType,
                                     AnimationStateHandler<T> handler) {
        super(name, (int) speed, easingType, handler);
    }

    /**
     * Controller with a custom easing function ({@code t - eased_t}).
     */
    public CustomAnimationController(String name, float speed,
                                     Function<Double, Double> customEasing,
                                     AnimationStateHandler<T> handler) {
        super(name, (int) speed, customEasing, handler);
    }
}
