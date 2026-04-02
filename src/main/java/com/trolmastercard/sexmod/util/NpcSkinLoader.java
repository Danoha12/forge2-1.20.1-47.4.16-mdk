package com.trolmastercard.sexmod.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NpcSkinLoader — Portado a 1.20.1.
 * * Descarga y registra texturas de skins de forma asíncrona.
 * * Evita descargar la misma skin varias veces usando un caché en memoria.
 */
@OnlyIn(Dist.CLIENT)
public class NpcSkinLoader {

    // 🗄️ Caché para no saturar a Mojang ni tu RAM
    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final String MOJANG_SKIN_URL = "https://visage.surreal.cloud/skin/"; // Usamos Visage para obtener el .png directo de forma fácil

    /**
     * Obtiene la skin de un jugador. Si no está descargada, inicia la descarga en segundo plano.
     * @return El ResourceLocation de la skin o la skin por defecto (Steve) si aún no está lista.
     */
    public static ResourceLocation getOrCreateSkin(UUID uuid) {
        if (uuid == null) return Minecraft.getInstance().player != null ?
                Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().get("default").getTextureLocation() :
                new ResourceLocation("textures/entity/steve.png");

        if (SKIN_CACHE.containsKey(uuid)) {
            return SKIN_CACHE.get(uuid);
        }

        // 🚀 Si no está en caché, lanzamos la descarga asíncrona
        SKIN_CACHE.put(uuid, new ResourceLocation("textures/entity/steve.png")); // Placeholder temporal
        loadSkinAsync(uuid);

        return SKIN_CACHE.get(uuid);
    }

    private static void loadSkinAsync(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(MOJANG_SKIN_URL + uuid.toString());
                try (InputStream is = url.openStream()) {
                    NativeImage image = NativeImage.read(is);

                    // 🚨 IMPORTANTE: El registro de la textura DEBE ocurrir en el hilo principal de Minecraft
                    Minecraft.getInstance().execute(() -> {
                        ResourceLocation loc = new ResourceLocation("sexmod", "skins/" + uuid.toString().toLowerCase());
                        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(image));
                        SKIN_CACHE.put(uuid, loc);
                        System.out.println("[SexMod] Skin cargada con éxito para: " + uuid);
                    });
                }
            } catch (Exception e) {
                System.err.println("[SexMod] Error al descargar la skin para " + uuid + ": " + e.getMessage());
            }
        });
    }
}