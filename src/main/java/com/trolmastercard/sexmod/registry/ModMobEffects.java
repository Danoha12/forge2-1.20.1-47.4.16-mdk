package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.HornyPotion;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Main.MODID);

    public static final RegistryObject<MobEffect> HORNY_POTION =
            MOB_EFFECTS.register("horny_potion", () -> new HornyPotion());
}