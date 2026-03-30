package com.trolmastercard.sexmod.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.trolmastercard.sexmod.entity.AllieLampEntity;
import com.trolmastercard.sexmod.util.PlayerSkinUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * AllieLampModel — Portado a 1.20.1 / GeckoLib 4.
 * * GeoModel para la entidad "Allie Lamp".
 * Descarga la skin del jugador local, aplica un parche de color (brillo de lámpara),
 * la convierte a NativeImage (requisito de 1.20.1) y la registra como DynamicTexture.
 */
@OnlyIn(Dist.CLIENT)
public class AllieLampModel extends GeoModel<AllieLampEntity> {

    private static final ResourceLocation GEO =
            new ResourceLocation("sexmod", "geo/allie/lamp.geo.json");
    private static final ResourceLocation FALLBACK_TEX =
            new ResourceLocation("sexmod", "textures/entity/allie/lamp.png");
    private static final ResourceLocation ANIM =
            new ResourceLocation("sexmod", "animations/allie/lamp.animation.json");

    /** Textura dinámica en caché. Nula hasta que se descarga y procesa con éxito. */
    @Nullable
    private ResourceLocation dynamicTexture = null;

    // =========================================================================
    //  GeoModel overrides
    // =========================================================================

    @Override
    public ResourceLocation getModelResource(AllieLampEntity entity) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(AllieLampEntity entity) {
        if (this.dynamicTexture != null) return this.dynamicTexture;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return FALLBACK_TEX; // Prevenir NullPointerException al inicio

            // Descargar la skin del jugador local (Devuelve BufferedImage)
            BufferedImage skin = PlayerSkinUtil.fetchSkin(mc.player.getGameProfile().getId());
            if (skin == null) return FALLBACK_TEX;

            // Dibujar el parche de brillo (mismos colores originales)
            Graphics g = skin.getGraphics();
            g.setColor(new Color(185, 254, 255));   // Cyan pálido
            g.fillRect(0, 0, 2, 2);
            g.setColor(new Color(255, 255, 255));   // Blanco
            g.fillRect(2, 0, 1, 2);
            g.setColor(new Color(0, 0, 0));         // Negro
            g.fillRect(3, 0, 1, 2);
            g.dispose();

            // --- PUENTE 1.20.1: Convertir BufferedImage (ARGB) a NativeImage (ABGR) ---
            NativeImage nativeImage = new NativeImage(skin.getWidth(), skin.getHeight(), true);
            for (int x = 0; x < skin.getWidth(); x++) {
                for (int y = 0; y < skin.getHeight(); y++) {
                    int argb = skin.getRGB(x, y);

                    // Extraer canales
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int gColor = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Reensamblar en formato ABGR para NativeImage
                    int abgr = (a << 24) | (b << 16) | (gColor << 8) | r;
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }

            // Registrar como textura dinámica
            DynamicTexture dt = new DynamicTexture(nativeImage);
            this.dynamicTexture = mc.getTextureManager().register("allies_lamp", dt);

        } catch (Exception e) {
            e.printStackTrace();
            this.dynamicTexture = FALLBACK_TEX;
        }

        return this.dynamicTexture != null ? this.dynamicTexture : FALLBACK_TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(AllieLampEntity entity) {
        return ANIM;
    }
}