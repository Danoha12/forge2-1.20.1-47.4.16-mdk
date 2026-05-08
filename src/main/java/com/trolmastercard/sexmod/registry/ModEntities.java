package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.entity.KoboldEggEntity; // 🚨 Asegúrate de que el nombre sea el correcto
import com.trolmastercard.sexmod.entity.KoboldEntity;    // 🚨 IMPORTANTE: Importa la criatura
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModConstants.MOD_ID);

    // ── Registro del Huevo de Kobold ─────────────────────────────────────────
    public static final RegistryObject<EntityType<KoboldEggEntity>> KOBOLD_EGG =
            ENTITIES.register("kobold_egg",
                    () -> EntityType.Builder.of(KoboldEggEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.7F)
                            .build("kobold_egg"));

    // ── Registro del Kobold (LA CRIATURA) ────────────────────────────────────
    // 🚨 ESTA ES LA PIEZA QUE FALTABA
    public static final RegistryObject<EntityType<KoboldEntity>> KOBOLD =
            ENTITIES.register("kobold",
                    () -> EntityType.Builder.of(KoboldEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.2F) // Tamaño del bicho
                            .build("kobold"));
}