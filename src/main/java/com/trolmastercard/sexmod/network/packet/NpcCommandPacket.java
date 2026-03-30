package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * NpcCommandPacket — Creado para 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Envía las órdenes del menú radial (GoblinContextMenuScreen) al servidor.
 */
public class NpcCommandPacket {

    public enum Command { START_THROWING, FOLLOW, STOP_FOLLOW }

    private final int entityId;
    private final Command command;

    public NpcCommandPacket(int entityId, Command command) {
        this.entityId = entityId;
        this.command = command;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(NpcCommandPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeEnum(msg.command);
    }

    public static NpcCommandPacket decode(FriendlyByteBuf buf) {
        return new NpcCommandPacket(buf.readInt(), buf.readEnum(Command.class));
    }

    // ── Manejador (Handler del Servidor) ─────────────────────────────────────

    public static void handle(NpcCommandPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;

                // Buscamos a la entidad en el mundo del servidor usando su ID
                Entity targetEntity = sender.serverLevel().getEntity(msg.entityId);

                if (!(targetEntity instanceof BaseNpcEntity npc)) {
                    System.out.println("[SexMod] Error: Se intentó comandar a una entidad que no es un NPC.");
                    return;
                }

                // Ejecutamos la orden real en el servidor
                switch (msg.command) {
                    case START_THROWING:
                        if (npc.getSexPartner() == null) {
                            npc.setAnimState(AnimState.START_THROWING);
                        }
                        break;
                    case FOLLOW:
                        if (npc instanceof GoblinEntity goblin) {
                            goblin.startFollowing(sender.getUUID());
                        }
                        break;
                    case STOP_FOLLOW:
                        if (npc instanceof GoblinEntity goblin) {
                            goblin.stopFollowing(sender.getUUID());
                        }
                        break;
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}