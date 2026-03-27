package com.trolmastercard.sexmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PlayerSkinUtil - ported from y.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Downloads a player's skin texture from the Mojang session server as a
 * {@link BufferedImage}. Falls back to the bundled Steve texture if the
 * download fails (network error, offline mode, etc.).
 *
 * The texture is fetched by:
 *  1. Querying {@code sessionserver.mojang.com/session/minecraft/profile/<uuid>}
 *  2. Parsing the Base64-encoded {@code "value"} field from the JSON response
 *  3. Decoding that to find the {@code "url"} of the actual skin PNG
 *  4. Downloading and returning the PNG as a {@link BufferedImage}
 *
 * All network I/O should be called off the main thread.
 */
@OnlyIn(Dist.CLIENT)
public final class PlayerSkinUtil {

    /** Number of retry attempts before giving up and using the fallback texture. */
    public static final int MAX_RETRIES = 3;

    /** Fallback Steve skin resource. */
    private static final ResourceLocation FALLBACK_SKIN =
        new ResourceLocation("sexmod", "textures/player/steve.png");

    private PlayerSkinUtil() {}

    /**
     * Downloads and returns the skin image for {@code playerUUID}.
     *
     * Must be called from a background thread - never the render/game thread.
     *
     * @param playerUUID the player's UUID (dashes optional)
     * @return the skin as a {@link BufferedImage}, or the Steve fallback on error
     */
    @OnlyIn(Dist.CLIENT)
    public static BufferedImage fetchSkin(UUID playerUUID) throws Exception {
        try {
            // Step 1 - fetch profile JSON from Mojang
            String uuidNoDash = playerUUID.toString().replace("-", "");
            URL profileUrl = new URL(
                "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDash);
            String json;
            try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(profileUrl.openStream()))) {
                json = reader.lines().collect(Collectors.joining());
            }

            // Step 2 - extract the Base64-encoded "value" field
            int valueStart = json.indexOf("\"value\" : ");
            if (valueStart < 0) throw new IllegalArgumentException("No 'value' field in profile");
            int dataStart = valueStart + 11; // skip: "value" :  "
            StringBuilder encoded = new StringBuilder();
            int i = 0;
            while (json.charAt(dataStart + i) != '"') {
                encoded.append(json.charAt(dataStart + i++));
            }

            // Step 3 - decode Base64 and extract the texture URL
            String decoded = new String(Base64.getDecoder().decode(encoded.toString()));
            int urlStart = decoded.indexOf("\"url\" : ");
            if (urlStart < 0) throw new IllegalArgumentException("No 'url' field in texture data");
            int urlData = urlStart + 9; // skip: "url" :  "
            StringBuilder skinUrl = new StringBuilder();
            int j = 0;
            while (decoded.charAt(urlData + j) != '"') {
                skinUrl.append(decoded.charAt(urlData + j++));
            }

            // Step 4 - download the skin PNG
            return ImageIO.read(new URL(skinUrl.toString()));

        } catch (Exception e) {
            // Fallback: return the bundled Steve texture
            return ImageIO.read(
                Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(FALLBACK_SKIN)
                    .orElseThrow()
                    .open());
        }
    }
}
