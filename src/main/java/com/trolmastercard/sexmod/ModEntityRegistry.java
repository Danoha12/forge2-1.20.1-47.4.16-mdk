package com.trolmastercard.sexmod;

// --- IMPORTACIONES LIMPIAS ---
import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityRegistry {

    // Registrador principal usando tu MOD ID directamente
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "sexmod");

    // ==========================================
    // ¡AQUÍ DEJAMOS VIVO SOLO AL KOBOLD!
    // ==========================================
    public static final RegistryObject<EntityType<KoboldEntity>> KOBOLD =
            ENTITIES.register("kobold", () -> EntityType.Builder
                    .<KoboldEntity>of(KoboldEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(50).build("kobold"));

    // ==========================================
    // ENTIDADES SILENCIADAS (Hasta que las programemos)
    // ==========================================
    /*
    public static final RegistryObject<EntityType<KoboldEgg>> KOBOLD_EGG =
        ENTITIES.register("kobold_egg", () -> EntityType.Builder
            .<KoboldEgg>of(KoboldEgg::new, MobCategory.CREATURE)
            .sized(2.04F, 2.04F).clientTrackingRange(50).build("kobold_egg"));

    public static final RegistryObject<EntityType<WanderingEnemyEntity>> WANDERING_ENEMY =
        ENTITIES.register("pyrocinical", () -> EntityType.Builder
            .<WanderingEnemyEntity>of(WanderingEnemyEntity::new, MobCategory.AMBIENT)
            .sized(0.6F, 1.8F).clientTrackingRange(50).build("pyrocinical"));

    public static final RegistryObject<EntityType<EnergyBallEntity>> ENERGY_BALL =
        ENTITIES.register("energy_ball", () -> EntityType.Builder
            .<EnergyBallEntity>of(EnergyBallEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F).clientTrackingRange(64).build("energy_ball"));

    public static final RegistryObject<EntityType<AllieEntity>> ALLIE =
        ENTITIES.register("allie", () -> EntityType.Builder
            .<AllieEntity>of(AllieEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.95F).clientTrackingRange(50).build("allie"));

    public static final RegistryObject<EntityType<ClothingOverlayEntity>> CLOTHING_OVERLAY =
        ENTITIES.register("custom_model", () -> EntityType.Builder
            .<ClothingOverlayEntity>of(ClothingOverlayEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.95F).clientTrackingRange(50).build("custom_model"));
    */

    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}