package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendChatMessagePacket — Portado a 1.20.1.
 * * Bidireccional:
 * * C -> S: Pide al servidor que un NPC transmita un mensaje.
 * * S -> C: Envía el mensaje de texto puro al cliente para mostrarlo en el chat.
 */
public class SendChatMessagePacket {

    private final String message;
    private final UUID npcUUID;

    // ── Constructores ────────────────────────────────────────────────────────

    public SendChatMessagePacket(String message, UUID npcUUID) {
        this.message = message;
        this.npcUUID = npcUUID;
    }

    // ── Codec (Limpio y Nativo de 1.20.1) ────────────────────────────────────

    public static void encode(SendChatMessagePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.message); // Automáticamente maneja el tamaño y UTF-8

        // Escribimos el UUID directamente si existe, si no, enviamos uno vacío para evitar nulos
        boolean hasUUID = msg.npcUUID != null;
        buf.writeBoolean(hasUUID);
        if (hasUUID) {
            buf.writeUUID(msg.npcUUID);
        }
    }

    public static SendChatMessagePacket decode(FriendlyByteBuf buf) {
        String msg = buf.readUtf();
        UUID uid = buf.readBoolean() ? buf.readUUID() : null;
        return new SendChatMessagePacket(msg, uid);
    }

    // ── Manejador Bidireccional ──────────────────────────────────────────────

    public static void handle(SendChatMessagePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // 1. LÓGICA DEL CLIENTE (Mostrar en el chat)
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg.message));
            });
        } else {
            // 2. LÓGICA DEL SERVIDOR (Buscar al NPC y retransmitir)
            ctx.enqueueWork(() -> handleServer(msg));
        }
        ctx.setPacketHandled(true);
    }

    // ── Aislamiento Estricto ─────────────────────────────────────────────────

    private static void handleClient(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && message != null && !message.isEmpty()) {
            // En 1.20.1 displayClientMessage toma un Component y un boolean (true = actionbar, false = chat)
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private static void handleServer(SendChatMessagePacket msg) {
        if (msg.npcUUID == null) return;

        // Buscar el NPC en el servidor
        List<BaseNpcEntity> npcs = BaseNpcEntity.getAllByUUIDServer(msg.npcUUID);
        if (npcs.isEmpty()) return;

        BaseNpcEntity npc = npcs.get(0);
        Vec3 pos = npc.position();

        // 🚨 Retransmitir a todos los jugadores en un radio de 40 bloques
        // Ya no necesitamos la dimensión en el paquete, la sacamos del NPC directamente.
        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.x, pos.y, pos.z, 40.0D, npc.level().dimension())),
                new SendChatMessagePacket(msg.message, msg.npcUUID)
        );
    }
}