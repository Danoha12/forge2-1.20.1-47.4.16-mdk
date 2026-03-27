package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.NpcType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ModelListPacket - ported from bd.class / g6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Bidirectional:
 *
 * CLIENT - SERVER (empty):
 *   Server reads the player's {@code getEntityData()} NBT for per-type model overrides
 *   and replies with the populated SERVER-CLIENT payload.
 *
 * SERVER - CLIENT (payload):
 *   Delivers {@code HashMap<NpcType, String>} model-override keys to the client
 *   and immediately opens the {@link com.trolmastercard.sexmod.client.screen.NpcTypeSelectScreen}.
 *
 * Field mapping:
 *   a = overrides  HashMap<NpcType, String>
 *   b = player     (sender, not serialised - server-side only)
 *   c = valid      boolean
 *
 * In 1.12.2:
 *   - {@code ByteBufUtils.readUTF8String/writeUTF8String} - {@code buf.readUtf()/writeUtf()}
 *   - {@code player.getEntityData().func_74779_i(...)} - {@code player.getPersistentData().getString(...)}
 *   - {@code fy.hasSpecifics} - {@code NpcType.hasSpecifics}
 *   - {@code minecraft.func_152344_a(() - minecraft.func_147108_a(...))} -
 *     {@code mc.execute(() - mc.setScreen(new NpcTypeSelectScreen(...)))}
 */
public class ModelListPacket {

    private final HashMap<NpcType, String> overrides;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public ModelListPacket(HashMap<NpcType, String> overrides) {
        this.overrides = overrides;
        this.valid     = true;
    }

    /** Empty request packet (CLIENT - SERVER). */
    public static ModelListPacket request() {
        return new ModelListPacket(new HashMap<>());
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static ModelListPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        HashMap<NpcType, String> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            NpcType type  = NpcType.valueOf(buf.readUtf());
            String  model = buf.readUtf();
            map.put(type, model);
        }
        return new ModelListPacket(map);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(overrides.size());
        for (Map.Entry<NpcType, String> e : overrides.entrySet()) {
            buf.writeUtf(e.getKey().name());
            buf.writeUtf(e.getValue());
        }
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @ModelListPacket :(");
                return;
            }

            net.minecraft.server.level.ServerPlayer sender = ctx.getSender();

            if (sender == null) {
                // CLIENT side - open NpcTypeSelectScreen with the overrides map
                openScreenClient(overrides);
                return;
            }

            // SERVER side - read player NBT and send back the overrides
            HashMap<NpcType, String> result = new HashMap<>();
            for (NpcType type : NpcType.values()) {
                if (!type.hasSpecifics) continue;
                String val = sender.getPersistentData().getString("sexmod:GirlSpecific" + type);
                if (!val.isEmpty()) result.put(type, val);
            }

            com.trolmastercard.sexmod.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                new ModelListPacket(result));
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreenClient(HashMap<NpcType, String> overrides) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(
            new com.trolmastercard.sexmod.client.screen.NpcTypeSelectScreen(overrides)));
    }
}
