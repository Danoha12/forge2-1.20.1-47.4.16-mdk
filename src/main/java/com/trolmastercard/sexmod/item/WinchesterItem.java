package com.trolmastercard.sexmod.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * WinchesterItem - ported from aj.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * Animated item (Winchester prop). Has no active animations but implements
 * {@link GeoItem} so GeckoLib can render it via {@link WinchesterRenderer}.
 *
 * Registration (in your {@code ModItems}):
 * <pre>
 *   public static final RegistryObject&lt;WinchesterItem&gt; WINCHESTER =
 *       ITEMS.register("winchester", WinchesterItem::new);
 * </pre>
 *
 * In 1.12.2:
 *   - {@code setRegistryName} and {@code setUnlocalizedName} were called manually.
 *   - {@code registerControllers} was empty.
 *   - Item was a singleton ({@code aj.b = new aj()}).
 *
 * In 1.20.1:
 *   - Registration is handled by the {@code DeferredRegister} system.
 *   - {@code IAnimatable} - {@link GeoItem}.
 *   - {@code AnimationFactory} - {@link AnimatableInstanceCache} via {@link GeckoLibUtil}.
 *   - No need to register on MinecraftForge.EVENT_BUS - use EventBusSubscriber if needed.
 */
public class WinchesterItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WinchesterItem() {
        super(new Item.Properties());
    }

    // =========================================================================
    //  GeoItem / GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations for this prop item
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
