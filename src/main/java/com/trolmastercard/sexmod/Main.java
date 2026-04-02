package com.trolmastercard.sexmod;

import com.trolmastercard.sexmod.command.NpcSpawnCommand;
import com.trolmastercard.sexmod.registry.ModBlocks;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.registry.ModItems;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

import java.io.IOException;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "sexmod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    // ── EL PUENTE (Proxy) ──
    // Esto detecta si estamos en Cliente o Servidor y crea la instancia correcta.
    public static CommonProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);

    public Main() {
        GeckoLib.initialize();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 1. Registro de contenido (Debe ir en el constructor en la 1.20.1)
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        // 2. Suscribir los eventos del Proxy al ciclo de vida
        modBus.addListener(this::setup);
        modBus.addListener(this::loadComplete);

        // 3. Eventos de Forge (Comandos y Handlers)
        forgeBus.addListener(this::onCommandsRegister);
        forgeBus.register(new NpcDeathHandler());
        forgeBus.register(new PlayerSexEventHandler());

        // Cargar configuración inicial
        try {
            setConfigs();
        } catch (IOException e) {
            LOGGER.error("Error cargando config", e);
        }
    }

    public static void setConfigs() {
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Llamamos al preInit e init de tu CommonProxy
        proxy.preInit(event);
        try {
            proxy.init(event);
        } catch (IOException e) {
            LOGGER.error("Error en el Init del Proxy", e);
        }
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        // Llamamos al postInit de tu CommonProxy
        try {
            proxy.postInit(event);
        } catch (IOException e) {
            LOGGER.error("Error en el PostInit del Proxy", e);
        }
    }

    private void onCommandsRegister(RegisterCommandsEvent event) {
        NpcSpawnCommand.register(event.getDispatcher());
        // Aquí puedes registrar más comandos de servidor
    }

    // [Aquí iría tu método setConfigs() que ya tenemos, omitido para brevedad]
}