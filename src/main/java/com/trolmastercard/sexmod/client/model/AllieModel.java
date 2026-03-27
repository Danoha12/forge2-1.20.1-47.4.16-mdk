package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.AllieLampEntity;
import com.trolmastercard.sexmod.util.PlayerSkinUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AllieLampModel - Portado a 1.20.1 / GeckoLib 4.
 * * Modelo para la "Lámpara de Allie" (AllieLampEntity).
 * Al renderizarse por primera vez, descarga la skin del jugador local,
 * aplica un parche de color (efecto de brillo místico) y lo registra
 * como una textura dinámica para que la lámpara brille con la esencia del dueño.
 */
@OnlyIn(Dist.CLIENT)
public class AllieLampModel extends GeoModel<AllieLampEntity> {

    // Mantenemos las rutas originales para que cargue tus archivos .json actuales
    private static final ResourceLocation GEO =
            new ResourceLocation("sexmod", "geo/allie/lamp.geo.json");
    private static final ResourceLocation FALLBACK_TEX =
            new ResourceLocation("sexmod", "textures/entity/allie/lamp.png");
    private static final ResourceLocation ANIM =
            new ResourceLocation("sexmod", "animations/allie/lamp.animation.json");

    /** Ubicación de la textura dinámica - nula hasta que se descarga la skin. */
    @Nullable
    private ResourceLocation dynamicTexture = null;

    // =========================================================================
    //  GeoModel Overrides
    // =========================================================================

    @Override
    public ResourceLocation getModelResource(AllieLampEntity entity) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(AllieLampEntity entity) {
        // Si ya tenemos la textura generada, no la volvemos a procesar
        if (dynamicTexture != null) return dynamicTexture;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return FALLBACK_TEX;

            // Obtenemos la skin del jugador para "vincular" la lámpara a su dueño
            BufferedImage skin = PlayerSkinUtil.fetchSkin(mc.player.getGameProfile().getId());

            if (skin != null) {
                // Dibujamos el parche de brillo mágico (Pale Cyan / Blanco / Negro)
                Graphics2D g = skin.createGraphics();

                // Color del resplandor místico
                g.setColor(new Color(185, 254, 255));   // Cian pálido
                g.fillRect(0, 0, 2, 2);

                g.setColor(Color.WHITE);
                g.fillRect(2, 0, 1, 2);

                g.setColor(Color.BLACK);
                g.fillRect(3, 0, 1, 2);

                g.dispose();

                // Registramos la textura dinámica en el motor de Minecraft
                // Mantenemos el nombre "alliesLamp" para consistencia interna
                dynamicTexture = mc.getTextureManager().register(
                        "alliesLamp",
                        new DynamicTexture(skin));
            }

        } catch (Exception e) {
            // Si hay error (ej. modo offline), usamos la textura por defecto del mod
            dynamicTexture = FALLBACK_TEX;
        }

        return dynamicTexture != null ? dynamicTexture : FALLBACK_TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(AllieLampEntity entity) {
        return ANIM;
    }
}