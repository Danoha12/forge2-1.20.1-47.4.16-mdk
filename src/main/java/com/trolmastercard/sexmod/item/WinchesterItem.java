package com.trolmastercard.sexmod.item; // Ajusta a tu paquete de ítems

import com.trolmastercard.sexmod.client.renderer.WinchesterRenderer; // Asegúrate de tener esta clase
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * WinchesterItem — Portado a 1.20.1 / GeckoLib 4.
 * * Ítem animado (Prop de Winchester). No tiene animaciones activas por sí solo,
 * * pero implementa GeoItem para que GeckoLib pueda renderizar su modelo 3D.
 */
public class WinchesterItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 🚨 1.20.1: Recibimos Properties para poder configurar el stack y la pestaña creativa en el registro
    public WinchesterItem(Properties properties) {
        super(properties);
    }

    // ── GeckoLib 4 (Lógica y Animación) ──────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Sin animaciones activas por defecto para este prop
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ── Enlace del Renderizador Cliente (¡Vital para 1.20.1!) ────────────────

    /**
     * En GeckoLib 4, los ítems DEBEN registrar su BlockEntityWithoutLevelRenderer (BEWLR)
     * a través de este método para que el juego sepa que debe usar el modelo 3D
     * en la mano, en el inventario y tirado en el suelo.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private WinchesterRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new WinchesterRenderer();
                }
                return this.renderer;
            }
        });
    }
}