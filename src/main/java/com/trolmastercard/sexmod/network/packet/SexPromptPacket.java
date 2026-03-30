package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.handler.SexProposalManager;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SexPromptPacket — Portado a 1.20.1.
 * * Paquete bidireccional para gestionar propuestas entre jugadores.
 * * CLIENTE -> SERVIDOR: Enruta la petición al jugador objetivo.
 * * SERVIDOR -> CLIENTE: Muestra la interfaz de propuesta al destinatario.
 */
public class SexPromptPacket {

    private final String animationType;
    private final UUID femaleUUID;
    private final UUID maleUUID;
    private final boolean forFemale;

    public SexPromptPacket(String animationType, UUID femaleUUID, UUID maleUUID, boolean forFemale) {
        this.animationType = animationType;
        this.femaleUUID = femaleUUID;
        this.maleUUID = maleUUID;
        this.forFemale = forFemale;
    }

    // ── Serialización (Optimizado para 1.20.1) ───────────────────────────────

    public static void encode(SexPromptPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.animationType);
        buf.writeUUID(msg.femaleUUID);
        buf.writeUUID(msg.maleUUID);
        buf.writeBoolean(msg.forFemale);
    }

    public static SexPromptPacket decode(FriendlyByteBuf buf) {
        return new SexPromptPacket(
                buf.readUtf(),
                buf.readUUID(),
                buf.readUUID(),
                buf.readBoolean()
        );
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(SexPromptPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // LADO CLIENTE: Recibimos la invitación del servidor
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            });
        } else {
            // LADO SERVIDOR: Un jugador envió una petición, hay que redirigirla
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;

                ServerPlayer female = (ServerPlayer) sender.level().getPlayerByUUID(msg.femaleUUID);
                ServerPlayer male = (ServerPlayer) sender.level().getPlayerByUUID(msg.maleUUID);

                if (female == null || male == null) {
                    System.out.println("[SexMod] Error: Jugador no encontrado para la propuesta.");
                    return;
                }

                // Determinamos quién debe recibir el paquete (el destinatario)
                ServerPlayer target = msg.forFemale ? female : male;

                // Rebotamos el paquete al cliente del objetivo
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), msg);
            });
        }
        ctx.setPacketHandled(true);
    }

    /** * Lógica exclusiva de cliente para evitar errores NoClassDefFound en el servidor. */
    private static void handleClient(SexPromptPacket msg) {
        // Mapeamos los datos al formato que espera el ProposalManager
        UUID requesterUUID = msg.forFemale ? msg.maleUUID : msg.femaleUUID;
        UUID targetUUID = msg.forFemale ? msg.femaleUUID : msg.maleUUID;

        SexProposalManager.INSTANCE.showProposal(new SexProposalManager.Proposal(
                msg.animationType,
                targetUUID,
                requesterUUID,
                msg.forFemale // requesterIsTarget (Depende de tu lógica de visualización de nombres)
        ));
    }
}