package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.screen.NpcTypeSelectScreen;
import com.trolmastercard.sexmod.entity.NpcType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * OpenModelSelectPacket - ported from bd.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Bidirectional packet:
 *
 * SERVER - CLIENT: opens the {@link NpcTypeSelectScreen} on the client with
 *   per-type model overrides (NBT key {@code sexmod:GirlSpecific<type>}).
 *
 * CLIENT - SERVER: the server reads the player's per-type model preferences
 *   from entity persistent data and sends them back.
 *
 * Field mapping:
 *   a = modelOverrides  (HashMap&lt;NpcType, String&gt;)
 *   b = player          (only used server-side during encode; not sent)
 *   c = valid
 *
 * In 1.12.2:
 *   - {@code ByteBufUtils.readUTF8String/writeUTF8String} - {@code buf.readUtf()/writeUtf()}
 *   - {@code minecraft.func_152344_a(r)} - {@code Minecraft.getInstance().execute(r)}
 *   - {@code minecraft.func_147108_a(screen)} - {@code minecraft.setScreen(screen)}
 *   - {@code player.getEntityData().func_74779_i(key)} - {@code player.getPersistentData().getString(key)}
 */
public class OpenModelSelectPacket {

    /** NBT key prefix for per-type model overrides. */
    public static final String NBT_PREFIX = "sexmod:GirlSpecific";

    private final HashMap<NpcType, String> modelOverrides;
    private final Player                   player;          // server-side only
    private final boolean                  valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    /** Client - Server constructor (no payload, just a request). */
    public OpenModelSelectPacket(Player serverPlayer) {
        this.modelOverrides = new HashMap<>();
        this.player         = serverPlayer;
        this.valid          = true;
    }

    /** Server - Client constructor with pre-built overrides map. */
    private OpenModelSelectPacket(HashMap<NpcType, String> overrides) {
        this.modelOverrides = overrides;
        this.player         = null;
        this.valid          = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static OpenModelSelectPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        HashMap<NpcType, String> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            NpcType type  = NpcType.valueOf(buf.readUtf());
            String  model = buf.readUtf();
            map.put(type, model);
        }
        return new OpenModelSelectPacket(map);
    }

    public void encode(FriendlyByteBuf buf) {
        // Build the map from the player's NBT when encoding (server-side)
        HashMap<NpcType, String> toSend = new HashMap<>();
        if (player != null) {
            for (NpcType type : NpcType.values()) {
                if (!type.hasSpecifics) continue;
                String val = player.getPersistentData().getString(NBT_PREFIX + type);
                if (!val.isEmpty()) toSend.put(type, val);
            }
        } else {
            toSend.putAll(modelOverrides);
        }

        buf.writeInt(toSend.size());
        for (Map.Entry<NpcType, String> entry : toSend.entrySet()) {
            buf.writeUtf(entry.getKey().toString());
            buf.writeUtf(entry.getValue());
        }
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) return;

            if (ctx.getSender() != null) {
                // This packet is server-client only
                System.out.println("received OpenModelSelect on wrong side");
                return;
            }

            // CLIENT side: open the screen
            openScreenOnClient(modelOverrides);
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreenOnClient(HashMap<NpcType, String> overrides) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new NpcTypeSelectScreen(overrides)));
    }
}
