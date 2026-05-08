package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.trolmastercard.sexmod.registry.ModMobEffects.MOB_EFFECTS;

/**
 * Registro central de efectos de poción/estado del mod.
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ModConstants.MODID);
    public static final RegistryObject<MobEffect> HORNY =
            MOB_EFFECTS.register("horny", HornyEffect::new);
    // Si tu SlimeEntity busca un efecto específico, regístralo aquí.
    // Ejemplo: public static final RegistryObject<MobEffect> SLIME_STUN =
    // EFFECTS.register("slime_stun", () -> new YourEffectClass());

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

}