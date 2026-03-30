package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * OutlineShaderManager — Portado a 1.20.1.
 * * Carga y maneja el shader de contorno para los NPCs.
 * * Implementa ResourceManagerReloadListener para sobrevivir a F3+T.
 */
@Mod.EventBusSubscriber(modid = "sexmod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OutlineShaderManager implements ResourceManagerReloadListener {

    private static final ResourceLocation SHADER_LOCATION =
            new ResourceLocation("sexmod", "shaders/post/outline.json");

    public static PostChain postChain;

    // ── Registro en Forge (1.20.1) ───────────────────────────────────────────

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // Le decimos a Forge: "Oye, avísame cuando recargues los recursos"
        event.registerReloadListener(new OutlineShaderManager());
    }

    // ── Evento de Recarga (Sobrevive al F3+T) ────────────────────────────────

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        load(); // Recarga el shader de forma segura
    }

    // ── Lógica de Carga ──────────────────────────────────────────────────────

    public static void load() {
        Minecraft mc = Minecraft.getInstance();

        // Limpieza de memoria crítica si el shader ya existía antes de un F3+T
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }

        try {
            postChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    SHADER_LOCATION
            );

            postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());

            System.out.println("[SexMod] Shader de contorno registrado exitosamente :)");

        } catch (Exception e) {
            // Main.LOGGER.warn("Failed to load shader: {}", SHADER_LOCATION, e);
            System.err.println("[SexMod] Error al cargar el shader: " + SHADER_LOCATION);
            e.printStackTrace();
            postChain = null;
        }
    }

    // ── Evento de Redimensión de Ventana ─────────────────────────────────────

    /**
     * Llama a esto desde tu evento de cambio de resolución, si aplica.
     */
    public static void resize(int width, int height) {
        if (postChain != null) {
            postChain.resize(width, height);
        }
    }

    public static boolean isAvailable() {
        return postChain != null;
    }
}