package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * ModMenus — Registro moderno de menús e inventarios para la 1.20.1
 */
public class ModMenus {

    // El motor principal que empaca los menús
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModConstants.MOD_ID);

    // ── Aquí registrarás tus menús (como el KoboldChestContainer) más adelante ──
    // Ejemplo de cómo se verá cuando lo conectes:
    // public static final RegistryObject<MenuType<KoboldChestContainer>> KOBOLD_CHEST =
    //         MENUS.register("kobold_chest", () -> IForgeMenuType.create(KoboldChestContainer::new));

    // El cable de encendido para conectar al Main
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}