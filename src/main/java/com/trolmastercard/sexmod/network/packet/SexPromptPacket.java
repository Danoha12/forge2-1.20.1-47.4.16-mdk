package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bidirectional packet used to transmit a sex proposal between players.
 *
 * Client - Server: routes the prompt to the correct target player.
 * Server - Client: displays the proposal UI.
 *
 * Fields:
 *   animationType  - animation/pose string
 *   femaleUUID     - UUID of the "female" player
 *   maleUUID       - UUID of the "male" player
 *   forFemale      - true if packet is destined for the female player
 *
 * Obfuscated name: g4
 */
public class SexPromptPacket {

    private final String animationType;
    private final UUID   femaleUUID;
    private final UUID   maleUUID;
    private final boolean forFemale;

    public SexPromptPacket(String animationType, UUID femaleUUID, UUID maleUUID, boolean forFemale) {
        this.animationType = animationType;
        this.femaleUUID    = femaleUUID;
        this.maleUUID      = maleUUID;
        this.forFemale     = forFemale;
    }

    // -- Codec -----------------------------------------------------------------

    public static SexPromptPacket decode(FriendlyByteBuf buf) {
        String anim   = buf.readUtf();
        UUID   female = UUID.fromString(buf.readUtf());
        UUID   male   = UUID.fromString(buf.readUtf());
        boolean ff    = buf.readBoolean();
        return new SexPromptPacket(anim, female, male, ff);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.animationType);
        buf.writeUtf(this.femaleUUID.toString());
        buf.writeUtf(this.maleUUID.toString());
        buf.writeBoolean(this.forFemale);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // Server - Client: show the proposal UI
            ctx.enqueueWork(() -> handleClient());
            ctx.setPacketHandled(true);
            return;
        }

        // Client - Server: forward to the correct target player
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            Level world = sender.level();

            net.minecraft.world.entity.player.Player female =
                    world.getPlayerByUUID(this.femaleUUID);
            net.minecraft.world.entity.player.Player male =
                    world.getPlayerByUUID(this.maleUUID);

            if (female == null) {
                System.out.println("Sex prompt invalid -> female player not found");
                return;
            }
            if (male == null) {
                System.out.println("Sex prompt invalid -> male player not found");
                return;
            }

            ServerPlayer target = (ServerPlayer)(this.forFemale ? female : male);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new SexPromptPacket(this.animationType, this.femaleUUID,
                            this.maleUUID, this.forFemale));
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        SexProposalManager.getInstance().addProposal(
                new SexProposalManager.Proposal(
                        this.animationType, this.femaleUUID,
                        this.maleUUID, this.forFemale));
    }
}
