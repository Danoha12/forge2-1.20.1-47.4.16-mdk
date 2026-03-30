package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ChangeDataParameterPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Actualiza parámetros específicos de un NPC en el servidor (IA, Modelos, Estados).
 */
public class ChangeDataParameterPacket {

  private final UUID npcUUID;
  private final String paramName;
  private final String value;

  public ChangeDataParameterPacket(UUID npcUUID, String paramName, String value) {
    this.npcUUID = npcUUID;
    this.paramName = paramName;
    this.value = value != null ? value : "null";
  }

  // ── Codec (Optimizado con UUID nativo) ───────────────────────────────────

  public static void encode(ChangeDataParameterPacket msg, FriendlyByteBuf buf) {
    buf.writeUUID(msg.npcUUID);
    buf.writeUtf(msg.paramName);
    buf.writeUtf(msg.value);
  }

  public static ChangeDataParameterPacket decode(FriendlyByteBuf buf) {
    return new ChangeDataParameterPacket(buf.readUUID(), buf.readUtf(), buf.readUtf());
  }

  // ── Manejador (Handler) ──────────────────────────────────────────────────

  public static void handle(ChangeDataParameterPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
    NetworkEvent.Context ctx = ctxSupplier.get();

    if (!ctx.getDirection().getReceptionSide().isServer()) return;

    ctx.enqueueWork(() -> {
      // Buscamos al NPC en el registro global que creamos en BaseNpcEntity
      BaseNpcEntity npc = null;
      for (BaseNpcEntity e : BaseNpcEntity.getAllActive()) {
        if (e.getNpcUUID().equals(msg.npcUUID)) {
          npc = e;
          break;
        }
      }

      if (npc == null) return;

      // Procesar el cambio según el nombre del parámetro
      switch (msg.paramName) {
        case "pregnant" -> {
          // Si es un Kobold o entidad con embarazo, actualizamos su contador
          // Nota: Asegúrate de tener este Accessor en PlayerKoboldEntity
          // npc.getEntityData().set(PlayerKoboldEntity.PREGNANCY_TICK, Integer.parseInt(msg.value));
        }

        case "currentModel" ->
                npc.setModelIndex(Integer.parseInt(msg.value));

        case "currentAction" -> {
          try {
            AnimState requested = AnimState.valueOf(msg.value);
            // No sobrescribimos una animación activa con un ataque simple
            if (requested == AnimState.ATTACK && npc.getAnimState() != AnimState.NULL) break;
            npc.setAnimStateFiltered(requested);
          } catch (IllegalArgumentException e) {
            System.err.println("[SexMod] Animación no válida enviada: " + msg.value);
          }
        }

        case "playerSheHasSexWith" -> {
          if ("null".equals(msg.value)) npc.setPartnerUUID((UUID) null);
          else npc.setPartnerUUID(UUID.fromString(msg.value));
        }

        case "targetPos" -> {
          // Formato esperado: "xfyfzf" (ej: 100f64f100f)
          String[] parts = msg.value.split("f");
          if (parts.length >= 3) {
            npc.setTargetPos(new Vec3(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
            ));
          }
        }

        case "master" ->
                npc.setMaster("null".equals(msg.value) ? "" : msg.value);

        case "shouldbeattargetpos" ->
                npc.setShouldBeAtTargetPos(Boolean.parseBoolean(msg.value));

        case "walk speed" -> {
          // Actualizamos el modo de velocidad (WALK, RUN, etc.)
          npc.getEntityData().set(BaseNpcEntity.WALK_SPEED_MODE, msg.value);
        }
      }
    });
    ctx.setPacketHandled(true);
  }
}