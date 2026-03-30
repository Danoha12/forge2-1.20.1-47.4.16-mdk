package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.block.SexmodFireBlock;
import com.trolmastercard.sexmod.item.*;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModItems — Portado a 1.20.1.
 * * Centraliza el registro de todos los Ítems y Bloques del mod usando DeferredRegister.
 * * Reemplaza el antiguo sistema de llamadas estáticas individuales.
 */
public final class ModItems {

    // Creamos los catálogos diferidos para Ítems y Bloques
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    // ── Registro de Bloques ──────────────────────────────────────────────────

    public static final RegistryObject<Block> SEXMOD_FIRE = BLOCKS.register("sexmod_fire",
            SexmodFireBlock::new);

    // ── Registro de Ítems ────────────────────────────────────────────────────

    public static final RegistryObject<Item> SLIME_ITEM = ITEMS.register("slime_item",
            SlimeItem::new);

    public static final RegistryObject<Item> ALLIES_LAMP = ITEMS.register("allies_lamp",
            AlliesLampItem::new);

    public static final RegistryObject<Item> STAFF = ITEMS.register("staff",
            StaffItem::new);

    public static final RegistryObject<Item> TRIBE_EGG = ITEMS.register("tribe_egg",
            TribeEggItem::new);

    public static final RegistryObject<Item> GALATH_COIN = ITEMS.register("galath_coin",
            GalathCoinItem::new);

    public static final RegistryObject<Item> HORNY_POTION = ITEMS.register("horny_potion",
            HornyPotion::new);

    public static final RegistryObject<Item> KOBOLD_EGG_SPAWN = ITEMS.register("kobold_egg_spawn",
            KoboldEggSpawnItem::new);

    public static final RegistryObject<Item> GALATH_WAND = ITEMS.register("galath_wand",
            GalathWandItem::new);

    // ── Método de Inicialización ─────────────────────────────────────────────

    /**
     * Vincula estos registros al bus de eventos de Forge.
     * * Llamar a este método una sola vez desde el constructor de tu clase principal (Main).
     */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}