package com.trolmastercard.sexmod.client.renderer.item;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.AllieModel;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * AlliesLampItemRenderer — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * Renderizador del ítem de la lámpara. Descarga la skin del jugador desde Mojang,
 * la fusiona sobre la textura base usando NativeImage y la guarda en caché.
 * Oculta los brazos en primera persona cuando la lámpara está en uso.
 */
@OnlyIn(Dist.CLIENT)
public class AlliesLampItemRenderer extends GeoItemRenderer<AlliesLampItem> {

    /** Textura fusionada en caché (nula hasta el primer renderizado exitoso). */
    private static ResourceLocation cachedTexture = null;

    public AlliesLampItemRenderer() {
        super(new AllieModel());
    }

    // ── Texture resolution (Optimizado para NativeImage 1.20.1) ──────────────

    private ResourceLocation resolveTexture() {
        if (cachedTexture != null) return cachedTexture;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return getDefaultTextureLocation();

        try {
            // Obtener el JSON del perfil de Mojang
            String uuid = mc.player.getStringUUID().replace("-", "");
            URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(profileUrl.openStream()))) {
                String json = br.lines().collect(Collectors.joining());

                // Extraer el valor en Base64
                int vi = json.indexOf("\"value\" : ");
                if (vi == -1) throw new Exception("No value field");
                StringBuilder valueB64 = new StringBuilder();
                int pos = vi + 11;
                while (json.charAt(pos) != '"') valueB64.append(json.charAt(pos++));

                String decoded = new String(Base64.getDecoder().decode(valueB64.toString()));

                // Extraer la URL de la skin
                int ui = decoded.indexOf("\"url\" : ");
                if (ui == -1) throw new Exception("No url field");
                StringBuilder skinUrl = new StringBuilder();
                pos = ui + 9;
                while (decoded.charAt(pos) != '"') skinUrl.append(decoded.charAt(pos++));

                // Leer la skin directamente como NativeImage (Formato 1.20.1)
                NativeImage skin = NativeImage.read(new URL(skinUrl.toString()).openStream());

                // Leer la textura base desde los recursos del juego
                ResourceLocation baseLoc = getDefaultTextureLocation();
                NativeImage base = NativeImage.read(
                        mc.getResourceManager().getResource(baseLoc).orElseThrow().open()
                );

                // Fusionar: Los píxeles no transparentes de la skin sobrescriben la base
                for (int x = 0; x < Math.min(base.getWidth(), skin.getWidth()); x++) {
                    for (int y = 0; y < Math.min(base.getHeight(), skin.getHeight()); y++) {
                        int pixel = skin.getPixelRGBA(x, y); // ABGR
                        // Comprobar si el canal Alpha (transparencia) es mayor a 0
                        if (((pixel >> 24) & 0xFF) != 0) {
                            base.setPixelRGBA(x, y, pixel);
                        }
                    }
                }

                DynamicTexture tex = new DynamicTexture(base);
                cachedTexture = mc.getTextureManager().register("mod_lamp_tex", tex);
            }
        } catch (Exception e) {
            // Si falla la conexión o el parseo, usar la base por defecto
            cachedTexture = getDefaultTextureLocation();
        }

        return cachedTexture;
    }

    private ResourceLocation getDefaultTextureLocation() {
        return new ResourceLocation("sexmod", "textures/entity/allie/allie.png");
    }

    // ── Bone visibility filter (Actualizado a GeckoLib 4) ────────────────────

    @Override
    public void preRender(PoseStack poseStack, AlliesLampItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {

        Minecraft mc = Minecraft.getInstance();
        boolean hideArms = false;

        if (mc.player != null) {
            // Usamos la constante segura definida en el ítem
            boolean allieInUse = mc.player.getPersistentData().getBoolean(AlliesLampItem.KEY_IN_USE);
            hideArms = allieInUse && mc.options.getCameraType().isFirstPerson();
        }

        // Ocultar o mostrar los huesos de los brazos dinámicamente
        boolean finalHideArms = hideArms;
        model.getBone("leftArm").ifPresent(b -> b.setHidden(finalHideArms));
        model.getBone("rightArm").ifPresent(b -> b.setHidden(finalHideArms));

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    // ── Texture override ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getTextureLocation(AlliesLampItem animatable) {
        return resolveTexture();
    }
}