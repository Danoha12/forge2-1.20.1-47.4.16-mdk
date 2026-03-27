package com.trolmastercard.sexmod.network;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server - Client packet: forces an update of a PlayerKoboldEntity's
 * animation state (J / em.J) and subtype integer (D / em.D).
 * Obfuscated name: gd
 */
public class ForcePlayerGirlUpdatePacket {

    private final UUID playerUUID;
    private final int  subtype;
    private final AnimState animState;

    public ForcePlayerGirlUpdatePacket(UUID playerUUID, int subtype, AnimState animState) {
        this.playerUUID = playerUUID;
        this.subtype    = subtype;
        this.animState  = animState;
    }

    // -- Codec -----------------------------------------------------------------

    public static ForcePlayerGirlUpdatePacket decode(FriendlyByteBuf buf) {
        UUID uuid = UUID.fromString(buf.readUtf());
        int  sub  = buf.readInt();
        AnimState state = AnimState.valueOf(buf.readUtf());
        return new ForcePlayerGirlUpdatePacket(uuid, sub, state);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.playerUUID.toString());
        buf.writeInt(this.subtype);
        buf.writeUtf(this.animState.toString());
    }

    // -- Handler (client-only) -------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            PlayerKoboldEntity player = PlayerKoboldEntity.getByUUID(this.playerUUID);
            if (player == null) {
                System.out.println("received an invalid message @ForcePlayerGirlUpdate :(");
                return;
            }
            player.getEntityData().set(BaseNpcEntity.DATA_ANIM_STATE, this.animState.toString());
            player.getEntityData().set(BaseNpcEntity.DATA_SUBTYPE,    this.subtype);
        });
        ctx.setPacketHandled(true);
    }
}
