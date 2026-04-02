package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.entity.KoboldEgg;
// Importa aquí tus otras entidades (Galath, Allie, etc.) cuando las registres
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEntities — Registro central de todas las entidades del mod.
 * * Portado a 1.20.1 usando DeferredRegister.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModConstants.MOD_ID);

    // ── Registro del Huevo de Kobold ─────────────────────────────────────────
    public static final RegistryObject<EntityType<KoboldEgg>> KOBOLD_EGG =
            ENTITIES.register("kobold_egg",
                    () -> EntityType.Builder.of(KoboldEgg::new, MobCategory.MISC)
                            .sized(0.6F, 0.7F) // Ajusta el tamaño de la hitbox del huevo
                            .build("kobold_egg"));

    /* Aquí irás añadiendo a las demás, por ejemplo:
       public static final RegistryObject<EntityType<GalathEntity>> GALATH = ...
    */
}