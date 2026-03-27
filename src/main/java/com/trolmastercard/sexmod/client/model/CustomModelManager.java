package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.NpcType;
import com.trolmastercard.sexmod.ClothingSlot;
import com.trolmastercard.sexmod.entity.LightingMode;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ModelListPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedModelFactory;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * CustomModelManager - Gestor de Modelos Personalizados.
 * Portado a 1.20.1 / GeckoLib 4.
 * Administra la carga local de accesorios y vestimenta desde la carpeta del usuario.
 */
@OnlyIn(Dist.CLIENT)
public class CustomModelManager {

    // =========================================================================
    //  Constantes y Rutas (Mantenidas para no romper mods de la comunidad)
    // =========================================================================

    public static final String BASE_DIR       = "sexmod/custom_models";
    static final String        WHITELIST_FILE  = "sexmod/custom_models/whitelisted_servers.txt";
    public static final String MOD_FOLDER      = "sexmod_custom_models";

    static Map<String, Entry> registry = new HashMap<>();

    public static boolean whitelistEnabled = false;
    public static boolean loaded           = false;

    // =========================================================================
    //  API Pública
    // =========================================================================

    public static Map<String, Entry> getAll() { return registry; }

    public static boolean isRegistered(String name) { return registry.get(name) != null; }

    public static float getVersion(String name) {
        Entry e = registry.get(name);
        return e == null ? 0.0F : e.version;
    }

    public static int reload(boolean clientSide) {
        clearModels(clientSide);
        return loadAll(clientSide);
    }

    public static void clearModels(boolean clientSide) {
        if (clientSide) {
            for (Entry e : registry.values()) {
                if (e == null) continue;
                if (e.geoResourceLocation != null) GeckoLibCache.getBakedModels().remove(e.geoResourceLocation);
                if (e.textureResourceLocation != null) Minecraft.getInstance().getTextureManager().release(e.textureResourceLocation);
            }
        }
        registry.clear();
    }

    public static void requestServerSync() {
        ModNetwork.CHANNEL.sendToServer(ModelListPacket.request());
    }

    public static boolean isCurrentServerWhitelisted() {
        String server = getCurrentServerAddress();
        return server != null && isWhitelisted(server);
    }

    // =========================================================================
    //  Sistema de Carga
    // =========================================================================

    public static int loadAll(boolean clientSide) {
        log(Level.INFO, "Cargando modelos personalizados...");
        String dir = getCustomModelsDir();
        File folder = new File(dir);
        folder.mkdirs();

        String[] subDirs = folder.list((f, n) -> new File(f, n).isDirectory());
        if (subDirs == null) {
            log(Level.ERROR, "No se pudo leer la carpeta de modelos en: " + folder.getAbsolutePath());
            return -1;
        }

        log(Level.INFO, "Se encontraron " + subDirs.length + " modelos personalizados.");
        int count = 0;
        for (String name : subDirs) {
            String err = validateFiles(name, dir);
            if (!err.isEmpty()) { log(Level.ERROR, err); return -1; }
            err = registerModel(name, dir, clientSide);
            if (!err.isEmpty()) { log(Level.ERROR, err); return -1; }
            count++;
        }
        log(Level.DEBUG, "Se registraron " + count + " modelos correctamente.");
        loaded = true;
        return 0;
    }

    public static String validateFiles(String name, String dir) {
        String base = String.format("%s/%s", dir, name);
        if (!new File(base + "/" + name + ".geo.json").exists()) return "Falta el archivo .geo.json para: " + name;
        if (!new File(base + "/" + name + ".png").exists()) return "Falta la textura .png para: " + name;
        if (!new File(base + "/" + name + ".cfg").exists()) return "Falta el archivo de configuración .cfg para: " + name;
        return "";
    }

    public static String registerModel(String name, String dir, boolean clientSide) {
        if (registry.containsKey(name)) return "El modelo '" + name + "' ya estaba registrado.";

        String base = dir + "/" + name + "/";
        File cfgFile = new File(base + name + ".cfg");
        Entry entry = new Entry(cfgFile, name);
        if (entry.error != null) return entry.error;

        ResourceLocation texRL = null;
        ResourceLocation geoRL = new ResourceLocation("sexmod", name + "Model");

        if (clientSide) {
            try {
                texRL = loadTexture(name, new File(base + name + ".png"));
            } catch (Exception ex) {
                return "La textura de '" + name + "' está corrupta.";
            }

            try {
                String json = readFile(new File(base + name + ".geo.json"));
                var bakedModel = BakedModelFactory.getDefault().constructGeoModel(
                        software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter.GEO_GSON
                                .fromJson(json, software.bernie.geckolib.loading.json.raw.GeometryModelData.class));
                GeckoLibCache.getBakedModels().put(geoRL, bakedModel);
            } catch (Exception ex) {
                return "El modelo 3D de '" + name + "' está corrupto: " + ex.getMessage();
            }

            entry.geoResourceLocation = geoRL;
            entry.textureResourceLocation = texRL;
        }

        registry.put(name, entry);
        return "";
    }

    // =========================================================================
    //  Accesores de Registro
    // =========================================================================

    @Nullable public static Entry getEntry(String name) { return registry.get(name); }
    @Nullable public static ResourceLocation getGeoRL(String name) {
        Entry e = registry.get(name);
        if (e == null) { if (!"cross".equals(name)) warn(name, "geo"); return null; }
        return e.geoResourceLocation;
    }
    @Nullable public static ResourceLocation getTextureRL(String name) {
        Entry e = registry.get(name);
        if (e == null) { if (!"cross".equals(name)) warn(name, "texture"); return null; }
        return e.textureResourceLocation;
    }
    public static ClothingSlot getClothingSlot(String name) {
        Entry e = registry.get(name);
        return (e != null) ? e.clothingSlot : ClothingSlot.HEAD;
    }
    public static HashSet<NpcType> getCompatibleTypes(String name) {
        Entry e = registry.get(name);
        return (e != null) ? e.compatibleTypes : new HashSet<>();
    }
    public static HashSet<String> getBonesToHide(String name) {
        Entry e = registry.get(name);
        return (e != null) ? e.bonesToHide : new HashSet<>();
    }
    public static String getAuthor(String name) {
        Entry e = registry.get(name);
        return (e != null) ? e.author : "";
    }

    public static HashMap<ClothingSlot, java.util.List<String>> getModelsForNpc(BaseNpcEntity npc) {
        HashMap<ClothingSlot, java.util.List<String>> result = new HashMap<>();
        for (ClothingSlot slot : ClothingSlot.values()) result.put(slot, new ArrayList<>());
        for (Map.Entry<String, Entry> kv : registry.entrySet()) {
            Entry e = kv.getValue();
            if (!e.compatibleTypes.isEmpty() && !e.compatibleTypes.contains(NpcType.fromEntity(npc))) continue;
            result.get(e.clothingSlot).add(kv.getKey());
        }
        return result;
    }

    public static HashMap<String, Float> getAllVersions() {
        HashMap<String, Float> result = new HashMap<>();
        for (Map.Entry<String, Entry> kv : registry.entrySet())
            result.put(kv.getKey(), kv.getValue().version);
        return result;
    }

    // =========================================================================
    //  Lista Blanca (Whitelist)
    // =========================================================================

    public static void addToWhitelist(String server) {
        File file = new File(WHITELIST_FILE);
        file.getParentFile().mkdirs();
        HashSet<String> existing = file.exists() ? readWhitelist() : new HashSet<>();
        existing.add(server);
        try (FileWriter fw = new FileWriter(file)) {
            for (String s : existing) fw.write(s + "\n");
        } catch (IOException ignored) {}
    }

    public static boolean isWhitelisted(String server) {
        return readWhitelist().contains(server);
    }

    static HashSet<String> readWhitelist() {
        File file = new File(WHITELIST_FILE);
        HashSet<String> set = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) set.add(line);
        } catch (IOException ignored) {}
        return set;
    }

    // =========================================================================
    //  Helpers Internos
    // =========================================================================

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static String getCurrentServerAddress() {
        ServerData data = Minecraft.getInstance().getCurrentServer();
        if (data == null) return null;
        String addr = data.ip;
        int colon = addr.indexOf(':');
        return colon != -1 ? addr.substring(0, colon) : addr;
    }

    public static String getCustomModelsDir() {
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) return MOD_FOLDER;
        String server = getCurrentServerAddress();
        return server == null ? BASE_DIR + "/singleplayer" : BASE_DIR + "/" + server;
    }

    @OnlyIn(Dist.CLIENT)
    static ResourceLocation loadTexture(String name, File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        var tex = new net.minecraft.client.renderer.texture.DynamicTexture(image);
        return Minecraft.getInstance().getTextureManager().register(name, tex);
    }

    static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    static void log(Level level, String msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            if (level == Level.ERROR) System.err.println("[Tribu/CustomModels] " + msg);
            else System.out.println("[Tribu/CustomModels] " + msg);
            return;
        }
        ChatFormatting fmt = level == Level.DEBUG ? ChatFormatting.DARK_GREEN : (level == Level.ERROR ? ChatFormatting.RED : ChatFormatting.WHITE);
        player.displayClientMessage(Component.literal(fmt + msg), false);
    }

    private static void warn(String name, String field) {
        System.out.printf("[Tribu/CustomModels] Advertencia: Intentando acceder a '%s' en el modelo '%s' no registrado.%n", field, name);
    }

    // =========================================================================
    //  Clase Interna: Entry (Datos leídos del .cfg)
    // =========================================================================

    public static class Entry {
        public ClothingSlot clothingSlot;
        public HashSet<NpcType> compatibleTypes = new HashSet<>();
        public HashSet<String> bonesToHide = new HashSet<>();
        public String author;
        public String customBoneName;
        boolean enableWhenNude = false;
        public LightingMode lightingMode;
        public float guiSizeFactor = 1.0F;
        public float guiVertOffset = 0.0F;
        public ResourceLocation textureResourceLocation;
        public ResourceLocation geoResourceLocation;
        public String error;
        public float version;

        @OnlyIn(Dist.CLIENT)
        public Entry(File cfgFile, String name) {
            if (name.contains(" ") || name.contains("#") || name.contains("$")) {
                error = "El nombre del modelo no puede contener espacios, '#' o '$'."; return;
            }
            if ("cross".equalsIgnoreCase(name)) {
                error = "No puedes llamar a tu modelo 'cross'. Ese nombre está reservado."; return;
            }

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cfgFile)) {
                props.load(fis);
            } catch (Exception ex) {
                error = "No se pudo leer el archivo .cfg de " + name; return;
            }

            try { clothingSlot = ClothingSlot.valueOf(props.getProperty("wear_type").replace(" ", "")); }
            catch (Exception ex) { error = "El valor 'wear_type' es inválido en " + name; return; }

            if (clothingSlot == ClothingSlot.CUSTOM_BONE) {
                customBoneName = props.getProperty("custom_bone", "");
                if (customBoneName.isEmpty()) { error = "Falta el campo 'custom_bone' en " + name; return; }
            }

            String whichGirls = props.getProperty("which_girls", "").replace(" ", "");
            for (String typeName : whichGirls.split(",")) {
                if (typeName.isEmpty()) continue;
                try { compatibleTypes.add(NpcType.valueOf(typeName)); }
                catch (Exception ex) { error = "El tipo de NPC '" + typeName + "' no existe."; return; }
            }

            try { lightingMode = LightingMode.valueOf(props.getProperty("which_lighting").replace(" ", "")); }
            catch (Exception ex) { error = "El valor 'which_lighting' es inválido."; return; }

            author = props.getProperty("author", "anon");
            String bones = props.getProperty("bones_to_hide");
            if (bones != null && !bones.isEmpty()) bonesToHide.addAll(Arrays.asList(bones.replace(" ", "").split(",")));
            enableWhenNude = "yes".equalsIgnoreCase(props.getProperty("enable_when_nude", "").replace(" ", ""));

            try { guiSizeFactor = Float.parseFloat(props.getProperty("gui_size_factor", "1.0").replace(",",".")); } catch (Exception ignored) {}
            try { guiVertOffset = Float.parseFloat(props.getProperty("gui_vertical_positioning", "0.0").replace(",",".")); } catch (Exception ignored) {}
            try { version = Float.parseFloat(props.getProperty("version", "0").replace(",",".")); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    //  Eventos del Cliente (ClientEventHandler)
    // =========================================================================

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEventHandler {
        private static boolean joinedOnce = false;

        @SubscribeEvent
        public static void onClientConnect(ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft.getInstance().execute(() -> reload(true));
            joinedOnce = false;
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (!event.getEntity().equals(Minecraft.getInstance().player) || joinedOnce) return;
            joinedOnce = true;
            if (isCurrentServerWhitelisted()) requestServerSync();
        }

        @SubscribeEvent
        public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft.getInstance().execute(() -> clearModels(true));
            joinedOnce = false;
        }
    }
}