package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.client.particle.SexmodDragonBreathParticle; // Ajusta el import
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.SexProposalManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * ClientSetup — Reemplaza al antiguo ClientProxy de la 1.12.2.
 * * Maneja los KeyBindings, Partículas y configuración exclusiva del cliente.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    // ── Teclas (KeyBindings) ─────────────────────────────────────────────────

    public static final KeyMapping KEY_INTERACT_GOBLIN = new KeyMapping(
            "key.sexmod.interact",
            GLFW.GLFW_KEY_G,
            "key.categories.sexmod"
    );

    public static final KeyMapping KEY_CUSTOMIZE_MENU = new KeyMapping(
            "key.sexmod.customize",
            GLFW.GLFW_KEY_L,
            "key.categories.sexmod"
    );

    // ── 1. Evento de Inicialización Principal del Cliente ────────────────────

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Inicializar Singletons y Configs de Cliente
            SexProposalManager.INSTANCE = new SexProposalManager();
            Main.setConfigs();

            // Los renderizadores ya se registran en EntityRenderRegistry
            // El pre-cargado de modelos (FakeWorld) se eliminó porque GeckoLib 4
            // cachea los modelos .geo.json automáticamente al cargar el juego.
        });
    }

    // ── 2. Evento de Registro de Teclas ──────────────────────────────────────

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(KEY_INTERACT_GOBLIN);
        event.register(KEY_CUSTOMIZE_MENU);
    }

    // ── 3. Evento de Registro de Partículas ──────────────────────────────────

    @SubscribeEvent
    public static void onParticleRegister(RegisterParticleProvidersEvent event) {
        // Asumiendo que DRAGON_BREATH_CUM está registrado en tu ModParticleRegistry
        // event.registerSpriteSet(ModParticleRegistry.DRAGON_BREATH_CUM.get(), SexmodDragonBreathParticle.Factory::new);
    }

    // ── Eventos del Bus de FORGE (Comandos de Cliente) ───────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void onClientCommands(RegisterClientCommandsEvent event) {
            // En 1.20.1, los comandos usan la librería Brigadier.
            // Necesitarás adaptar las clases de comandos para que usen event.getDispatcher()

            // WhitelistServerCommand.register(event.getDispatcher());
            // SetModelCodeCommand.register(event.getDispatcher());
            // FutaCommand.register(event.getDispatcher());
        }
    }
}