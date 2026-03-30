package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CustomizeNpcPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Aplica un código de modelo personalizado y/o datos específicos (pecho, cabello, etc.) a un NPC.
 */
public class CustomizeNpcPacket {

    private final String modelCode;
    private final UUID npcUUID;
    private final List<Integer> specificData;

    public CustomizeNpcPacket(String modelCode, UUID npcUUID, List<Integer> specificData) {
        this.modelCode = modelCode;
        this.npcUUID = npcUUID;
        this.specificData = specificData != null ? specificData : new ArrayList<>();
    }

    public CustomizeNpcPacket(String modelCode, UUID npcUUID) {
        this(modelCode, npcUUID, new ArrayList<>());
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(CustomizeNpcPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.modelCode);
        buf.writeUUID(msg.npcUUID);
        // Forge/Minecraft 1.20.1 maneja listas de forma nativa y segura así:
        buf.writeCollection(msg.specificData, FriendlyByteBuf::writeInt);
    }

    public static CustomizeNpcPacket decode(FriendlyByteBuf buf) {
        return new CustomizeNpcPacket(
                buf.readUtf(),
                buf.readUUID(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt)
        );
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(CustomizeNpcPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (!ctx.getDirection().getReceptionSide().isServer()) return;

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // Buscar al NPC en nuestra lista activa optimizada
            BaseNpcEntity targetNpc = null;
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (npc.getNpcUUID().equals(msg.npcUUID)) {
                    targetNpc = npc;
                    break;
                }
            }

            if (targetNpc == null) {
                System.out.println("[SexMod] Intento de customizar un NPC que no existe o no está cargado.");
                return;
            }

            boolean hasSpecific = !msg.specificData.isEmpty();
            boolean specificValid = false;

            // Validar y aplicar datos específicos (ropa, peinado, proporciones)
            if (hasSpecific) {
                specificValid = validateSpecificData(targetNpc, msg.specificData);
                if (specificValid) {
                    targetNpc.applySpecificData(msg.specificData);
                }
            }

            // Si es un Avatar de jugador, guardamos en el NBT persistente del jugador
            if (targetNpc instanceof PlayerKoboldEntity) {
                // En Forge, "Player.PERSISTED_NBT_TAG" asegura que los datos sobrevivan si el jugador muere
                CompoundTag persistentData = sender.getPersistentData();
                CompoundTag modData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);

                String typeName = targetNpc.getType().toShortString(); // ej: "jenny", "ellie"

                modData.putString("sexmod:CustomModel_" + typeName, msg.modelCode);

                if (hasSpecific && specificValid) {
                    modData.putString("sexmod:GirlSpecific_" + typeName, targetNpc.encodeSpecificData(msg.specificData));
                }

                // Guardar la sub-etiqueta de vuelta
                persistentData.put(Player.PERSISTED_NBT_TAG, modData);
            } else {
                // Si es un NPC normal, solo aplicamos el código a su entidad
                targetNpc.setCustomModelCode(msg.modelCode);
            }
        });

        ctx.setPacketHandled(true);
    }

    // ── Helpers de Validación ────────────────────────────────────────────────

    /**
     * Valida que los datos propuestos no excedan los límites máximos permitidos por el NPC.
     * Esto evita crasheos de renderizado (Out of Bounds) si un jugador inyecta un código malicioso.
     */
    private static boolean validateSpecificData(BaseNpcEntity npc, List<Integer> proposed) {
        List<Integer> maxima = npc.getSpecificDataMaxima();
        if (maxima == null || maxima.isEmpty()) return false;

        for (int i = 0; i < maxima.size(); i++) {
            if (i >= proposed.size()) return false;
            // Si el valor propuesto es mayor o igual al límite máximo, es inválido
            if (maxima.get(i) <= proposed.get(i)) return false;
        }
        return true;
    }
}