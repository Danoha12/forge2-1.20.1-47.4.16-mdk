package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.model.AllieModel;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Item renderer for {@link AlliesLampItem}.
 * Ported from f0.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * The lamp renders using the player's skin blended over the lamp base
 * texture.  The blended texture is fetched once from Mojang's session
 * server and cached as a {@link DynamicTexture}.
 */
@OnlyIn(Dist.CLIENT)
public class AlliesLampItemRenderer extends GeoItemRenderer<AlliesLampItem> {

    /** Cached blended texture (null until first render). */
    private static ResourceLocation cachedTexture = null;

    public AlliesLampItemRenderer() {
        super(new AllieModel());
    }

    // -- Texture resolution ---------------------------------------------------

    /**
     * Returns the lamp texture, blending the local player's skin over the
     * base lamp texture.  Falls back to the default {@link AllieModel} texture
     * if anything goes wrong.
     *
     * In 1.12.2 this was the private {@code a()} method on f0.
     */
    private ResourceLocation resolveTexture() {
        if (cachedTexture != null) return cachedTexture;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return getDefaultTextureLocation();

        try {
            // Fetch the player's skin URL from the session server
            String uuid = mc.player.getStringUUID().replace("-", "");
            URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(profileUrl.openStream()))) {
                String json = br.lines().collect(Collectors.joining());

                // Extract Base64-encoded value
                int vi = json.indexOf("\"value\" : ");
                if (vi == -1) throw new Exception("No value field");
                StringBuilder valueB64 = new StringBuilder();
                int pos = vi + 11;
                while (json.charAt(pos) != '"') valueB64.append(json.charAt(pos++));

                String decoded = new String(Base64.getDecoder().decode(valueB64.toString()));

                // Extract skin URL from decoded JSON
                int ui = decoded.indexOf("\"url\" : ");
                if (ui == -1) throw new Exception("No url field");
                StringBuilder skinUrl = new StringBuilder();
                pos = ui + 9;
                while (decoded.charAt(pos) != '"') skinUrl.append(decoded.charAt(pos++));

                // Fetch skin image
                BufferedImage skin = ImageIO.read(new URL(skinUrl.toString()));

                // Load the base lamp texture from the resource manager
                ResourceLocation baseLoc = getDefaultTextureLocation();
                BufferedImage base = ImageIO.read(
                        mc.getResourceManager().getResource(baseLoc)
                                .orElseThrow().open()
                );

                // Blend: skin pixels overwrite base where skin is non-transparent
                for (int x = 0; x < Math.min(base.getWidth(), skin.getWidth()); x++) {
                    for (int y = 0; y < Math.min(base.getHeight(), skin.getHeight()); y++) {
                        int pixel = skin.getRGB(x, y);
                        if (pixel != 0) base.setRGB(x, y, pixel);
                    }
                }

                DynamicTexture tex = new DynamicTexture(base);
                cachedTexture = mc.getTextureManager().register("sexmod_lamp_tex", tex);
            }
        } catch (Exception e) {
            cachedTexture = getDefaultTextureLocation();
        }

        return cachedTexture;
    }

    private ResourceLocation getDefaultTextureLocation() {
        return new ResourceLocation("sexmod", "textures/entity/allie/allie.png");
    }

    // -- Bone visibility filter ------------------------------------------------

    /**
     * In 1.12.2, f0 hid the left/right arm bones when the player was using
     * Allie ({@code sexmodAllieInUse} tag + first-person mode).
     * Here we override {@code shouldRenderBone} to replicate that.
     */
    @Override
    public boolean shouldRenderBone(software.bernie.geckolib.cache.object.GeoBone bone) {
        String name = bone.getName();
        boolean isArm = "leftArm".equals(name) || "rightArm".equals(name);
        if (!isArm) return true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        boolean allieInUse = mc.player.getPersistentData().getBoolean("sexmodAllieInUse");
        // Hide arms when Allie is active and camera is in first-person
        return !(allieInUse && mc.options.getCameraType().isFirstPerson());
    }

    // -- Texture override -----------------------------------------------------

    @Override
    public ResourceLocation getTextureLocation(AlliesLampItem animatable) {
        return resolveTexture();
    }
}
