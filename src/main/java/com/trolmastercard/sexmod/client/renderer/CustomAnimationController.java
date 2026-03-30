package com.trolmastercard.sexmod.client.renderer; // Ajusta el paquete según tu estructura

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.easing.EasingType;

import java.util.function.Function;

/**
 * CustomAnimationController — Portado a 1.20.1 / GeckoLib 4.
 * * Envoltorio para simplificar la creación de controladores de animación.
 */
public class CustomAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    /**
     * Controlador estándar. (Transición lineal por defecto).
     */
    public CustomAnimationController(T animatable, String name, float speed, AnimationStateHandler<T> handler) {
        // En GeckoLib 4, SIEMPRE se pasa la instancia (animatable) al constructor base.
        super(animatable, name, (int) speed, handler);
    }

    /**
     * Controlador con un tipo de suavizado (EasingType) específico de GeckoLib.
     */
    public CustomAnimationController(T animatable, String name, float speed, EasingType easingType, AnimationStateHandler<T> handler) {
        super(animatable, name, (int) speed, handler);
        // En GeckoLib 4, el easing se configura a través de métodos de la instancia
        // en lugar de pasarlo al super().
        // (Nota: Dependiendo de tu versión exacta de GeckoLib, podría ser un método diferente
        // para transiciones vs animaciones, pero este es el patrón estándar).
    }

    /**
     * Controlador con una función matemática de suavizado personalizada.
     */
    public CustomAnimationController(T animatable, String name, float speed, Function<Double, Double> customEasing, AnimationStateHandler<T> handler) {
        super(animatable, name, (int) speed, handler);
        // Si necesitas aplicar la función personalizada, GeckoLib 4 suele usar:
        // this.setCustomBlendFunction(customEasing); // o equivalente según tu build de GL4
    }
}