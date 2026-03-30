package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.CustomModelManager;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SyncCustomModelsPacket — Portado a 1.20.1.
 * * BIDIRECCIONAL:
 * - CLIENTE -> SERVIDOR: Solicita descarga de una lista de modelos.
 * - SERVIDOR -> CLIENTE: Transfiere los bytes de los archivos (un paquete por archivo).
 */
public class SyncCustomModelsPacket {

    public enum FileType {
        CFG(".cfg"), PNG(".png"), GEO(".geo.json");
        public final String ending;
        FileType(String ending) { this.ending = ending; }
    }

    private List<String> modelNames = new ArrayList<>();
    private byte[] fileBytes;
    private FileType fileType;
    private String modelName;
    private int totalFiles = 0;

    /** CLIENTE -> SERVIDOR: Constructor para solicitar modelos. */
    public SyncCustomModelsPacket(List<String> modelNames) {
        this.modelNames = modelNames;
    }

    /** SERVIDOR -> CLIENTE: Constructor para enviar un archivo. */
    public SyncCustomModelsPacket(byte[] fileBytes, FileType fileType, String modelName) {
        this.fileBytes = fileBytes;
        this.fileType  = fileType;
        this.modelName = modelName;
    }

    public SyncCustomModelsPacket() {}

    public void setTotalFiles(int n) { this.totalFiles = n; }

    // ── Codificación y Decodificación (1.20.1) ───────────────────────────────

    public static void encode(SyncCustomModelsPacket msg, FriendlyByteBuf buf) {
        boolean isRequest = (msg.fileBytes == null);
        buf.writeBoolean(isRequest);

        if (isRequest) {
            // Enviar lista de nombres
            buf.writeCollection(msg.modelNames, FriendlyByteBuf::writeUtf);
        } else {
            // Enviar archivo
            buf.writeUtf(msg.modelName);
            buf.writeEnum(msg.fileType);
            buf.writeInt(msg.totalFiles);
            buf.writeByteArray(msg.fileBytes); // Mucho más rápido que un bucle manual
        }
    }

    public static SyncCustomModelsPacket decode(FriendlyByteBuf buf) {
        SyncCustomModelsPacket pkt = new SyncCustomModelsPacket();
        boolean isRequest = buf.readBoolean();

        if (isRequest) {
            pkt.modelNames = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        } else {
            pkt.modelName  = buf.readUtf();
            pkt.fileType   = buf.readEnum(FileType.class);
            pkt.totalFiles = buf.readInt();
            pkt.fileBytes  = buf.readByteArray();
        }
        return pkt;
    }

    // ── Manejadores (Handlers) ───────────────────────────────────────────────

    private static int receivedFileCount = 0;

    public static void handle(SyncCustomModelsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                handleClientReceive(msg);
            } else {
                handleServerRequest(msg, ctx.getSender());
            }
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientReceive(SyncCustomModelsPacket msg) {
        if (!CustomModelManager.isEnabled() || msg.fileBytes == null) return;

        File folder = new File(CustomModelManager.getCustomModelsDir(), msg.modelName);
        if (!folder.exists()) folder.mkdirs();

        File outFile = new File(folder, msg.modelName + msg.fileType.ending);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(msg.fileBytes);
        } catch (IOException e) {
            System.err.println("[SexMod] Error al escribir modelo: " + msg.modelName);
            e.printStackTrace();
        }

        // Feedback al jugador usando el sistema de componentes moderno
        int receivedLocal = 0;
        for (FileType type : FileType.values()) {
            if (new File(folder, msg.modelName + type.ending).exists()) receivedLocal++;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (receivedLocal == FileType.values().length) {
            mc.player.displayClientMessage(Component.literal("¡Modelo ")
                    .append(Component.literal(msg.modelName).withStyle(ChatFormatting.YELLOW))
                    .append(" descargado con éxito!")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            mc.player.displayClientMessage(Component.literal("Descargando modelo '")
                    .append(Component.literal(msg.modelName).withStyle(ChatFormatting.YELLOW))
                    .append("' (" + receivedLocal + "/" + FileType.values().length + ")...")
                    .withStyle(ChatFormatting.GRAY), false);
        }

        if (++receivedFileCount >= msg.totalFiles) {
            receivedFileCount = 0;
            CustomModelManager.reloadModels(true);
        }
    }

    private static void handleServerRequest(SyncCustomModelsPacket msg, ServerPlayer sender) {
        if (sender == null || msg.modelNames.isEmpty()) return;

        List<SyncCustomModelsPacket> packetsToSend = new ArrayList<>();
        for (String name : msg.modelNames) {
            File modelDir = new File("sexmod_custom_models", name);
            for (FileType type : FileType.values()) {
                File file = new File(modelDir, name + type.ending);
                if (file.exists()) {
                    try {
                        byte[] bytes = FileUtils.readFileToByteArray(file);
                        packetsToSend.add(new SyncCustomModelsPacket(bytes, type, name));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int total = packetsToSend.size();
        for (SyncCustomModelsPacket pkt : packetsToSend) {
            pkt.setTotalFiles(total);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), pkt);
        }
    }
}