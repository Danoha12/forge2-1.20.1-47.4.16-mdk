package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.trolmastercard.sexmod.network.ModNetwork.CHANNEL;

/**
 * ResetNpcPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Resetea el estado de todos los NPCs con un masterUUID específico, o solo del jugador.
 * * Contiene utilidades estáticas para desenbuguear entidades y jugadores.
 */
public class ResetNpcPacket {

  private final UUID masterUUID;
  private final boolean onlyResetPlayer;

  // ── Constructores ────────────────────────────────────────────────────────

  public ResetNpcPacket(UUID masterUUID) {
    this(masterUUID, false);
  }

  public ResetNpcPacket(UUID masterUUID, boolean onlyResetPlayer) {
    this.masterUUID = masterUUID;
    this.onlyResetPlayer = onlyResetPlayer;
  }

  // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

  public static void encode(ResetNpcPacket msg, FriendlyByteBuf buf) {
    buf.writeUUID(msg.masterUUID); // Escribe los 128 bits nativos
    buf.writeBoolean(msg.onlyResetPlayer);
  }

  public static ResetNpcPacket decode(FriendlyByteBuf buf) {
    return new ResetNpcPacket(buf.readUUID(), buf.readBoolean());
  }

  // ── Manejador ────────────────────────────────────────────────────────────

  public static void handle(ResetNpcPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
    NetworkEvent.Context ctx = ctxSupplier.get();
    ctx.enqueueWork(() -> {

      // Recolectar NPCs del dueño
      List<BaseNpcEntity> matches = BaseNpcEntity.getAllWithMaster(msg.masterUUID);

      for (BaseNpcEntity npc : matches) {
        if (npc.level().isClientSide()) continue;

        // Resetear al compañero/a si aplica
        if (npc.getSexTarget() != null) {
          ServerPlayer partner = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(npc.getSexTarget());
          resetPlayer(partner);
        }

        if (msg.onlyResetPlayer) continue;

        resetNpc(npc);
      }
    });
    ctx.setPacketHandled(true);
  }

  // ── Utilidades Estáticas ─────────────────────────────────────────────────

  /**
   * Reseteo total de un NPC post-cinemática.
   */
  public static void resetNpc(BaseNpcEntity npc) {
    if (npc instanceof PlayerKoboldEntity pk) {
      UUID playerUUID = pk.getPlayerUUID();
      if (playerUUID != null) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
        if (player != null) {
          // Remover vuelo y modo espectador
          player.getAbilities().flying = false;
          player.setNoGravity(false);
          if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(GameType.SURVIVAL);
          }
          player.onUpdateAbilities();

          CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IsGirlPacket(true));

          npc.getEntityData().set(BaseNpcEntity.MODEL_INDEX, 1);
        }
      }
    }

    npc.setActive(false);
    npc.setAnimState(AnimState.NULL);
    npc.setSexTarget(null);
    npc.bedPos = null;
    npc.setNoGravity(false);
    npc.setInvulnerable(false);

    // 🚨 Anti-Bug "Hacia la Luna": Teletransportar hacia arriba SOLO si el bloque tiene colisión (ej. piedra, tierra).
    Level level = npc.level();
    Vec3 pos = npc.position();
    BlockPos.MutableBlockPos mutPos = BlockPos.containing(pos).mutable();

    while (!level.getBlockState(mutPos).getCollisionShape(level, mutPos).isEmpty()) {
      mutPos.move(0, 1, 0);
    }
    npc.teleportTo(pos.x, mutPos.getY(), pos.z);
  }

  /**
   * Resetea el modo de juego y la física del jugador.
   */
  public static void resetPlayer(ServerPlayer player) {
    if (player == null) return;

    Level level = player.level();
    Vec3 pos = player.position();
    BlockPos.MutableBlockPos mutPos = BlockPos.containing(pos).mutable();

    // Misma lógica segura de colisión para el jugador
    while (!level.getBlockState(mutPos).getCollisionShape(level, mutPos).isEmpty()) {
      mutPos.move(0, 1, 0);
    }
    player.teleportTo(pos.x, mutPos.getY(), pos.z);

    // Limpiar "God Mode" (invulnerabilidad)
    player.getAbilities().invulnerable = false;
    player.setInvulnerable(false); // Por si acaso
    player.setNoGravity(false);
    player.getAbilities().flying = false;

    if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
      player.setGameMode(GameType.SURVIVAL);
    }

    player.onUpdateAbilities();

    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IsGirlPacket(true));
  }
}