package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bidirectional packet for custom-model version negotiation.
 *
 * Server - Client: sends map of {modelName - version} for all models the server has.
 * Client - Server (response): sends a {@link SyncCustomModelsPacket} listing which
 *   model names are either missing or out-of-date.
 *
 * Obfuscated name: g6
 */
public class RequestServerModelAvailabilityPacket {

    private final HashMap<String, Float> models;

    public RequestServerModelAvailabilityPacket(HashMap<String, Float> models) {
        this.models = models;
    }

    // -- Codec -----------------------------------------------------------------

    public static RequestServerModelAvailabilityPacket decode(FriendlyByteBuf buf) {
        HashMap<String, Float> map = new HashMap<>();
        // Client-side shortcut: if not a client proxy, skip map decode.
        if (!(Main.proxy instanceof ClientProxy)) {
            return new RequestServerModelAvailabilityPacket(map);
        }
        if (!CustomModelManager.isEnabled()) {
            return new RequestServerModelAvailabilityPacket(map);
        }
        try {
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                String name = buf.readUtf();
                float  ver  = buf.readFloat();
                map.put(name, ver);
            }
        } catch (Exception ignored) {}
        return new RequestServerModelAvailabilityPacket(map);
    }

    public void encode(FriendlyByteBuf buf) {
        if (Main.proxy instanceof ClientProxy) return; // server only writes
        buf.writeInt(this.models.size());
        for (Map.Entry<String, Float> entry : this.models.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // Client received the server's model list - figure out what to request
            ctx.enqueueWork(() -> {
                if (!CustomModelManager.isEnabled()) return;
                List<String> needed = new ArrayList<>();
                for (Map.Entry<String, Float> entry : this.models.entrySet()) {
                    String name = entry.getKey();
                    if (!CustomModelManager.hasModel(name)) {
                        needed.add(name);
                    } else {
                        float clientVer = CustomModelManager.getModelVersion(name);
                        float serverVer = entry.getValue();
                        if (serverVer > clientVer) needed.add(name);
                    }
                }
                // respond with list of models we need
                ModNetwork.CHANNEL.sendToServer(new SyncCustomModelsPacket(needed));
            });
        } else {
            // Server received a request (initial handshake) - send our model list
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new RequestServerModelAvailabilityPacket(CustomModelManager.getServerModelVersions()));
            });
        }
        ctx.setPacketHandled(true);
    }
}
