package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CatActivateFishingPacket - ported from ej.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT - SERVER. Triggers Luna (LunaEntity / eb) to use her fishing rod item.
 * Finds all LunaEntity instances matching the given NPC UUID and calls the
 * fishing rod item's use() method on the server.
 *
 * 1.12.2 - 1.20.1:
 *   - IMessage/IMessageHandler - FriendlyByteBuf + handle
 *   - ByteBufUtils - buf.readUtf
 *   - em.g(uuid) - BaseNpcEntity.getByUUID(uuid)
 *   - eb - LunaEntity; gp - LunaRodItem
 *   - gp.a(world, eb, MAIN_HAND) - item.use(level, entity, MAIN_HAND)
 *   - FMLCommonHandler.getMinecraftServerInstance().func_152344_a - server.execute
 */
public class CatActivateFishingPacket {

    private final UUID npcUUID;

    public CatActivateFishingPacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // -- Serialisation ----------------------------------------------------------

    public static void encode(CatActivateFishingPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcUUID.toString());
    }

    public static CatActivateFishingPacket decode(FriendlyByteBuf buf) {
        return new CatActivateFishingPacket(UUID.fromString(buf.readUtf()));
    }

    // -- Handler ----------------------------------------------------------------

    public static void handle(CatActivateFishingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            System.out.println("received an invalid message @CatActivateFishing :(");
            ctx.setPacketHandled(true);
            return;
        }
        ctx.enqueueWork(() -> {
            for (BaseNpcEntity npc : new ArrayList<>(BaseNpcEntity.getAllNpcs())) {
                if (npc.level.isClientSide()) continue;
                if (!(npc instanceof LunaEntity luna)) continue;
                if (!luna.getNpcUUID().equals(msg.npcUUID)) continue;

                // Trigger the fishing rod item use
                var rodStack = luna.heldVisualItem;
                if (!rodStack.isEmpty() && rodStack.getItem() instanceof LunaRodItem rod) {
                    rod.use(luna.level, luna, InteractionHand.MAIN_HAND);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
