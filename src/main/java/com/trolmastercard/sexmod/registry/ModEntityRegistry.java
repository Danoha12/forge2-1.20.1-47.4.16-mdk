package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.entity.*;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEntityRegistry — Portado a 1.20.1.
 * * Define los EntityTypes para todas las chicas, avatares y proyectiles.
 */
public class ModEntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModConstants.MOD_ID);

    // ── Entidades Principales (NPCs) ─────────────────────────────────────────

    public static final RegistryObject<EntityType<JennyEntity>> JENNY = ENTITIES.register("jenny",
            () -> EntityType.Builder.of(JennyEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("jenny"));

    public static final RegistryObject<EntityType<EllieEntity>> ELLIE = ENTITIES.register("ellie",
            () -> EntityType.Builder.of(EllieEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("ellie"));

    public static final RegistryObject<EntityType<AllieEntity>> ALLIE = ENTITIES.register("allie",
            () -> EntityType.Builder.of(AllieEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("allie"));

    public static final RegistryObject<EntityType<BiaEntity>> BIA = ENTITIES.register("bia",
            () -> EntityType.Builder.of(BiaEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("bia"));

    public static final RegistryObject<EntityType<BeeEntity>> BEE = ENTITIES.register("bee",
            () -> EntityType.Builder.of(BeeEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("bee"));

    public static final RegistryObject<EntityType<LunaEntity>> LUNA = ENTITIES.register("luna",
            () -> EntityType.Builder.of(LunaEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("luna"));

    public static final RegistryObject<EntityType<KoboldEntity>> KOBOLD = ENTITIES.register("kobold",
            () -> EntityType.Builder.of(KoboldEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("kobold"));

    public static final RegistryObject<EntityType<GoblinEntity>> GOBLIN = ENTITIES.register("goblin",
            () -> EntityType.Builder.of(GoblinEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("goblin"));

    public static final RegistryObject<EntityType<GalathEntity>> GALATH = ENTITIES.register("galath",
            () -> EntityType.Builder.of(GalathEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("galath"));

    public static final RegistryObject<EntityType<MangleLieEntity>> MANGLELIE = ENTITIES.register("manglelie",
            () -> EntityType.Builder.of(MangleLieEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("manglelie"));

    public static final RegistryObject<EntityType<SlimeNpcEntity>> SLIME_NPC = ENTITIES.register("slime_npc",
            () -> EntityType.Builder.of(SlimeNpcEntity::new, MobCategory.CREATURE)
                    .sized(2.04F, 2.04F).build("slime_npc"));

    // ── Avatares de Jugador (Morphs) ───────────────────────────────────────
    // Registramos las clases concretas de los avatares

    public static final RegistryObject<EntityType<CatPlayerKobold>> PLAYER_CAT = ENTITIES.register("player_cat",
            () -> EntityType.Builder.of(CatPlayerKobold::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build("player_cat"));

    public static final RegistryObject<EntityType<JennyPlayerEntity>> PLAYER_JENNY = ENTITIES.register("player_jenny",
            () -> EntityType.Builder.of(JennyPlayerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build("player_jenny"));

    public static final RegistryObject<EntityType<SlimePlayerEntity>> PLAYER_SLIME = ENTITIES.register("player_slime",
            () -> EntityType.Builder.of(SlimePlayerEntity::new, MobCategory.MISC)
                    .sized(2.04F, 2.04F).build("player_slime"));

    // ── Entidades de Cuerpo (Staging / Cinemáticas) ──────────────────────────

    public static final RegistryObject<EntityType<JennyBodyEntity>> JENNY_BODY = ENTITIES.register("jenny_body",
            () -> EntityType.Builder.of(JennyBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build("jenny_body"));

    public static final RegistryObject<EntityType<MangleLieSexEntity>> MANGLELIE_SEX = ENTITIES.register("manglelie_sex",
            () -> EntityType.Builder.of(MangleLieSexEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build("manglelie_sex"));

    // ── Proyectiles y Objetos Técnicos ───────────────────────────────────────

    public static final RegistryObject<EntityType<EnergyBallEntity>> ENERGY_BALL = ENTITIES.register("energy_ball",
            () -> EntityType.Builder.<EnergyBallEntity>of(EnergyBallEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(1).build("energy_ball"));

    public static final RegistryObject<EntityType<LunaHookEntity>> LUNA_HOOK = ENTITIES.register("luna_hook",
            () -> EntityType.Builder.<LunaHookEntity>of(LunaHookEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).build("luna_hook"));

    public static final RegistryObject<EntityType<ClothingOverlayEntity>> CLOTHING_OVERLAY = ENTITIES.register("custom_model",
            () -> EntityType.Builder.of(ClothingOverlayEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).build("custom_model"));

    // ── Enemigos ─────────────────────────────────────────────────────────────

    public static final RegistryObject<EntityType<WanderingEnemyEntity>> WANDERING_ENEMY = ENTITIES.register("pyrocinical",
            () -> EntityType.Builder.of(WanderingEnemyEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).build("pyrocinical"));

    // ── Inicialización ───────────────────────────────────────────────────────

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}