package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.CustomModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SyncCustomModelsPacket - ported from cu.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * BIDIRECTIONAL:
 *   CLIENT - SERVER: sends a list of model names to request downloads for.
 *   SERVER - CLIENT: sends raw bytes of a single model file (one packet per file).
 *
 * Two modes are distinguished by which constructor was used:
 *   Mode A (list-request): {@code c} populated, {@code b} null.
 *   Mode B (file-transfer): {@code b} populated, {@code c} empty.
 *
 * File types enum {@link FileType}: CFG, PNG, GEO (one packet per file type per model).
 *
 * Field mapping:
 *   d = valid         (fromBytes guard)
 *   c = modelNames    (List<String>)
 *   b = fileBytes     (byte[])
 *   f = fileType      (b - FileType enum)
 *   e = modelName     (String)
 *   a = totalFiles    (int - tells client how many packets to expect)
 *
 * Inner class {@code a} (handler) fields:
 *   a (static) = receivedFiles counter
 *
 * In 1.12.2:
 *   ByteBufUtils.readUTF8String/write  - FriendlyByteBuf.readUtf/writeUtf
 *   IMessage/IMessageHandler           - FriendlyByteBuf encode/decode + handle
 *   Main.proxy instanceof ClientProxy  - EffectiveSide.get().isClient()
 *   br.b()                             - CustomModelManager.isEnabled()
 *   br.h()                             - CustomModelManager.getCustomModelsDir()
 *   br.b(true)                         - CustomModelManager.reloadModels()
 *   Minecraft.func_71410_x()           - Minecraft.getInstance()
 *   entity.func_145747_a(component)    - player.displayClientMessage(...)
 *   TextFormatting.GREEN/YELLOW/GRAY   - ChatFormatting.GREEN/YELLOW/GRAY
 *   ge.b.sendTo(cu, MP)                - ModNetwork.CHANNEL.send(PLAYER, packet)
 *   minecraftServer.func_152344_a(r)   - ctx.enqueueWork(r)
 *   FileUtils.readFileToByteArray      - same (Apache Commons IO)
 */
public class SyncCustomModelsPacket {

    public enum FileType {
        CFG(".cfg"), PNG(".png"), GEO(".geo.json");
        public final String ending;
        FileType(String ending) { this.ending = ending; }
    }

    boolean valid = false;
    List<String> modelNames = new ArrayList<>();
    byte[] fileBytes;
    FileType fileType;
    String modelName;
    int totalFiles = 0;

    /** CLIENT - SERVER: request these model names. */
    public SyncCustomModelsPacket(List<String> modelNames) {
        this.modelNames = modelNames;
    }

    /** SERVER - CLIENT: transfer one file. */
    public SyncCustomModelsPacket(byte[] fileBytes, FileType fileType, String modelName) {
        this.fileBytes = fileBytes;
        this.fileType  = fileType;
        this.modelName = modelName;
    }

    public SyncCustomModelsPacket() {}

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int n) { this.totalFiles = n; }

    // =========================================================================
    //  Encode / Decode - same packet, direction detected by field presence
    // =========================================================================

    public static void encode(SyncCustomModelsPacket msg, FriendlyByteBuf buf) {
        boolean isClientToServer = (msg.fileBytes == null);
        buf.writeBoolean(isClientToServer);
        if (isClientToServer) {
            buf.writeInt(msg.modelNames.size());
            for (String s : msg.modelNames) buf.writeUtf(s);
        } else {
            buf.writeUtf(msg.modelName);
            buf.writeUtf(msg.fileType.name());
            buf.writeInt(msg.totalFiles);
            buf.writeInt(msg.fileBytes.length);
            for (byte b : msg.fileBytes) buf.writeByte(b);
        }
    }

    public static SyncCustomModelsPacket decode(FriendlyByteBuf buf) {
        SyncCustomModelsPacket pkt = new SyncCustomModelsPacket();
        boolean isRequest = buf.readBoolean();
        if (!isRequest) {
            // Server - client file transfer
            pkt.modelName  = buf.readUtf();
            pkt.fileType   = FileType.valueOf(buf.readUtf());
            pkt.totalFiles = buf.readInt();
            int len        = buf.readInt();
            pkt.fileBytes  = new byte[len];
            for (int i = 0; i < len; i++) pkt.fileBytes[i] = buf.readByte();
        } else {
            // Client - server name list
            int count = buf.readInt();
            for (int i = 0; i < count; i++) pkt.modelNames.add(buf.readUtf());
        }
        pkt.valid = true;
        return pkt;
    }

    // =========================================================================
    //  Handle
    // =========================================================================

    private static int receivedFileCount = 0;

    public static void handle(SyncCustomModelsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!msg.valid) {
            System.out.println("received an invalid Message @DownloadServerModel :(");
            ctx.setPacketHandled(true);
            return;
        }

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // CLIENT receives file bytes
            ctx.enqueueWork(() -> handleClientReceive(msg));
        } else {
            // SERVER receives model name list
            ctx.enqueueWork(() -> handleServerRequest(msg, ctx));
        }
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientReceive(SyncCustomModelsPacket msg) {
        if (!CustomModelManager.isEnabled()) return;

        String dir  = CustomModelManager.getCustomModelsDir() + "/" + msg.modelName;
        File folder = new File(dir);
        folder.mkdirs();
        File outFile = new File(dir + "/" + msg.modelName + msg.fileType.ending);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(msg.fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Count received files
        int received = 0;
        for (FileType type : FileType.values()) {
            if (new File(dir + "/" + msg.modelName + type.ending).exists()) received++;
        }

        Minecraft mc = Minecraft.getInstance();
        String name = msg.modelName;
        if (received == FileType.values().length) {
            mc.player.displayClientMessage(Component.literal(
                "\u00a7aSuccessfully downloaded the custom model '\u00a7e" + name + "\u00a7a'!"), false);
        } else {
            mc.player.displayClientMessage(Component.literal(
                "\u00a77downloading custom model '\u00a7e" + name + "\u00a77' ("
                    + received + "/" + FileType.values().length + ")..."), false);
        }

        if (++receivedFileCount >= msg.totalFiles) {
            receivedFileCount = 0;
            mc.execute(() -> CustomModelManager.reloadModels(true));
        }
    }

    private static void handleServerRequest(SyncCustomModelsPacket msg, NetworkEvent.Context ctx) {
        List<SyncCustomModelsPacket> packets = new ArrayList<>();
        for (String modelName : msg.modelNames) {
            String baseDir = "sexmod_custom_models/" + modelName;
            for (FileType type : FileType.values()) {
                File file = new File(baseDir + "/" + modelName + type.ending);
                if (!file.exists()) {
                    System.out.println(file.getAbsolutePath() + " doesnt exist lol");
                    continue;
                }
                try {
                    byte[] bytes = FileUtils.readFileToByteArray(file);
                    packets.add(new SyncCustomModelsPacket(bytes, type, modelName));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        int total = packets.size();
        for (SyncCustomModelsPacket pkt : packets) {
            pkt.setTotalFiles(total);
            com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(ctx::getSender),
                pkt);
        }
    }
}
