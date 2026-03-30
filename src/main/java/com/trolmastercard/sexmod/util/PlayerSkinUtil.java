package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * PlayerSkinUtil — Portado a 1.20.1.
 * * Descarga la skin de un jugador desde la API de Mojang como BufferedImage.
 * * Utiliza Gson para un parseo seguro y a prueba de fallos.
 * * 🚨 DEBE llamarse desde un hilo secundario (background thread).
 */
@OnlyIn(Dist.CLIENT)
public final class PlayerSkinUtil {

  public static final int MAX_RETRIES = 3;

  private static final ResourceLocation FALLBACK_SKIN =
          new ResourceLocation("sexmod", "textures/player/steve.png");

  private PlayerSkinUtil() {}

  /**
   * Descarga y devuelve la imagen de la skin para el UUID dado.
   */
  public static BufferedImage fetchSkin(UUID playerUUID) {
    try {
      // Paso 1: Petición a la API de Mojang
      String uuidNoDash = playerUUID.toString().replace("-", "");
      URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDash);

      JsonObject profileJson;
      try (InputStreamReader reader = new InputStreamReader(profileUrl.openStream(), StandardCharsets.UTF_8)) {
        // Gson lee y estructura el JSON mágicamente
        profileJson = JsonParser.parseReader(reader).getAsJsonObject();
      }

      // Paso 2: Extraer el valor en Base64 de forma segura
      String base64Value = profileJson.getAsJsonArray("properties")
              .get(0).getAsJsonObject()
              .get("value").getAsString();

      // Paso 3: Decodificar el Base64 (que es otro JSON)
      String decodedString = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
      JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();

      // Paso 4: Navegar hasta la URL de la textura de la SKIN
      String skinUrl = textureJson.getAsJsonObject("textures")
              .getAsJsonObject("SKIN")
              .get("url").getAsString();

      // Paso 5: Descargar el PNG final
      return ImageIO.read(new URL(skinUrl));

    } catch (Exception e) {
      System.err.println("[SexMod] No se pudo descargar la skin de " + playerUUID + ". Usando fallback.");

      // Fallback: Devolver a Steve (o tu textura base)
      try {
        return ImageIO.read(
                Minecraft.getInstance().getResourceManager()
                        .getResource(FALLBACK_SKIN)
                        .orElseThrow()
                        .open()
        );
      } catch (Exception ex) {
        // Fallback extremo si falta el archivo steve.png para evitar crasheos
        return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
      }
    }
  }
}