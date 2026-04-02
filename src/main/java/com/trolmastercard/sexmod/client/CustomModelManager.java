package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.registry.ClothingSlot;
import com.trolmastercard.sexmod.entity.LightingMode;
import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ModelListPacket;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * CustomModelManager — Gestiona la carga de modelos externos (.geo.json, .png, .cfg).
 */
@OnlyIn(Dist.CLIENT)
public class CustomModelManager {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String BASE_DIR = "sexmod/custom_models";
    public static final String WHITELIST_FILE = "sexmod/custom_models/whitelisted_servers.txt";
    public static final String MOD_FOLDER = "sexmod_custom_models";

    private static final Map<String, Entry> registry = new HashMap<>();
    public static boolean loaded = false;

    // ── API de Acceso ────────────────────────────────────────────────────────

    public static Map<String, Entry> getAll() { return registry; }

    public static void clearModels(boolean clientSide) {
        if (clientSide) {
            for (Entry e : registry.values()) {
                if (e.geoResourceLocation != null) GeckoLibCache.getBakedModels().remove(e.geoResourceLocation);
                if (e.textureResourceLocation != null) Minecraft.getInstance().getTextureManager().release(e.textureResourceLocation);
            }
        }
        registry.clear();
        loaded = false;
    }

    public static int reload(boolean clientSide) {
        clearModels(clientSide);
        return loadAll(clientSide);
    }

    // ── Lógica de Carga ──────────────────────────────────────────────────────

    public static int loadAll(boolean clientSide) {
        log("Iniciando carga de modelos personalizados...", ChatFormatting.WHITE);
        File folder = new File(getCustomModelsDir());
        if (!folder.exists()) folder.mkdirs();

        File[] subDirs = folder.listFiles(File::isDirectory);
        if (subDirs == null) return -1;

        int count = 0;
        for (File dir : subDirs) {
            String name = dir.getName();
            String result = registerModel(name, folder.getPath(), clientSide);
            if (result.isEmpty()) count++;
            else log("Error en '" + name + "': " + result, ChatFormatting.RED);
        }

        loaded = true;
        log("Se registraron " + count + " modelos correctamente.", ChatFormatting.GREEN);
        return count;
    }

    public static String registerModel(String name, String dir, boolean clientSide) {
        if (registry.containsKey(name)) return "";

        File baseDir = new File(dir, name);
        File cfgFile = new File(baseDir, name + ".cfg");
        if (!cfgFile.exists()) return "Falta archivo .cfg";

        Entry entry = new Entry(cfgFile, name);
        if (entry.error != null) return entry.error;

        if (clientSide) {
            try {
                // 1. Cargar Textura
                File texFile = new File(baseDir, name + ".png");
                entry.textureResourceLocation = loadTexture(name, texFile);

                // 2. Cargar y "Hornear" Modelo (GeckoLib 4)
                File geoFile = new File(baseDir, name + ".geo.json");
                String json = readFile(geoFile);
                entry.geoResourceLocation = new ResourceLocation(ModConstants.MOD_ID, name.toLowerCase() + "_model");

                var bakedModel = BakedModelFactory.getForNamespace()
                        .constructGeoModel(KeyFramesAdapter.GEO_GSON
                                .fromJson(json, software.bernie.geckolib.loading.json.raw.GeometryModelData.class));

                GeckoLibCache.getBakedModels().put(entry.geoResourceLocation, bakedModel);
            } catch (Exception e) {
                return "Error al hornear modelo: " + e.getMessage();
            }
        }

        registry.put(name, entry);
        return "";
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    public static String getCustomModelsDir() {
        if (ServerLifecycleHooks.getCurrentServer() != null) return MOD_FOLDER;
        ServerData data = Minecraft.getInstance().getCurrentServer();
        String sub = (data == null) ? "singleplayer" : data.ip.split(":")[0];
        return BASE_DIR + "/" + sub;
    }

    private static ResourceLocation loadTexture(String name, File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        DynamicTexture tex = new DynamicTexture(img);
        return Minecraft.getInstance().getTextureManager().register("custom_" + name.toLowerCase(), tex);
    }

    private static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static void log(String msg, ChatFormatting color) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(msg).withStyle(color), false);
        } else {
            LOGGER.info("[CustomModelManager] " + msg);
        }
    }

    // ── Clase Interna: Entry (Datos del Modelo) ──────────────────────────────

    public static class Entry {
        public ClothingSlot clothingSlot;
        public HashSet<NpcType> compatibleTypes = new HashSet<>();
        public HashSet<String> bonesToHide = new HashSet<>();
        public LightingMode lightingMode;
        public ResourceLocation textureResourceLocation;
        public ResourceLocation geoResourceLocation;
        public String error;
        public float version;
        public String author;

        public Entry(File cfgFile, String name) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cfgFile)) {
                props.load(fis);
                this.clothingSlot = ClothingSlot.valueOf(props.getProperty("wear_type", "HEAD").toUpperCase());
                this.lightingMode = LightingMode.valueOf(props.getProperty("which_lighting", "DEFAULT").toUpperCase());
                this.author = props.getProperty("author", "anon");
                this.version = Float.parseFloat(props.getProperty("version", "1.0").replace(",", "."));

                String girls = props.getProperty("which_girls", "");
                for (String s : girls.split(",")) {
                    if (!s.trim().isEmpty()) compatibleTypes.add(NpcType.valueOf(s.trim().toUpperCase()));
                }
            } catch (Exception e) {
                this.error = "Error en .cfg: " + e.getMessage();
            }
        }
    }

    // ── Manejador de Eventos ─────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT)
    public static class ClientEventHandler {
        private static boolean joinedOnce = false;

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft.getInstance().execute(() -> reload(true));
            joinedOnce = false;
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getEntity() == Minecraft.getInstance().player && !joinedOnce) {
                joinedOnce = true;
                // Aquí podrías enviar el ModelListPacket al servidor
                ModNetwork.CHANNEL.sendToServer(new ModelListPacket());
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft.getInstance().execute(() -> clearModels(true));
            joinedOnce = false;
        }
    }
}