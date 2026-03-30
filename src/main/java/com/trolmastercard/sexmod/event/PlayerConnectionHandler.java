package com.trolmastercard.sexmod.event; // Sugerido mover a un paquete de eventos

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
// Importa tus clases DevNpcA, DevNpcB, etc.
import com.trolmastercard.sexmod.registry.ModItems;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.IsGirlPacket;
import com.trolmastercard.sexmod.network.packet.SyncOwnershipPacket;
import com.trolmastercard.sexmod.network.packet.TribeHighlightPacket;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.core.BlockPos; // Asumiendo que BlockPos2 ahora es BlockPos normal
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PlayerConnectionHandler — Portado a 1.20.1.
 * * Maneja el inicio y cierre de sesión de los jugadores.
 * * Sincroniza datos, previene duplicación de NPCs y limpia estados.
 */
public class PlayerConnectionHandler {

  // ── UUIDs de Desarrolladores (Huevos de Pascua) ──────────────────────────

  private static final UUID DEV_UUID_A = UUID.fromString("b91e6484-8911-4def-ab04-9fa3452fca5f");
  private static final UUID DEV_UUID_B = UUID.fromString("adf20149-2adc-4a9d-9af5-8e9aeda019d6");

  // ── Evento de Inicio de Sesión (Login) ───────────────────────────────────

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

    // Limpiar estado residual de invulnerabilidad / vuelo (1.20.1 standard)
    serverPlayer.getAbilities().invulnerable = false;
    serverPlayer.setNoGravity(false);
    if (!serverPlayer.getAbilities().instabuild) {
      serverPlayer.getAbilities().flying = false;
    }
    serverPlayer.onUpdateAbilities(); // Obligatorio para notificar al cliente

    // Enviar paquetes de sincronización
    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new IsGirlPacket(true));
    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
            new SyncOwnershipPacket(GalathOwnershipData.getOwnershipSnapshot(serverPlayer.getUUID())));

    // Aleatorizar UUID de las Varitas de Galath para evitar dupes
    for (ItemStack stack : serverPlayer.getInventory().items) {
      if (stack.is(ModItems.GALATH_WAND.get())) {
        stack.getOrCreateTag().putUUID("user", UUID.randomUUID());
      }
    }

    // Enviar bloques resaltados de la tribu
    UUID tribeId = TribeManager.getTribeIdForMaster(serverPlayer.getUUID());
    if (tribeId != null) {
      Set<BlockPos> blocks = TribeManager.getAllClaimedBlocksAsSet(tribeId);
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new TribeHighlightPacket(blocks, true));
    }

    PlayerKoboldEntity.rebuildIndex();

    // Regenerar / Limpiar el NPC del jugador
    PlayerKoboldEntity existingNpc = PlayerKoboldEntity.getForPlayer(serverPlayer.getUUID());
    ServerLevel world = ServerLifecycleHooks.getCurrentServer().overworld();

    removeStaleNpcs(world, serverPlayer, existingNpc);

    if (existingNpc != null) {
      existingNpc.setActive(false);
      existingNpc.setAnimState(AnimState.NULL);
      // KoboldIdleScheduler.schedule(existingNpc); // Asegúrate de que esta clase exista
    }

    // Spawns de desarrollador
    UUID playerUUID = serverPlayer.getUUID();
    if (playerUUID.equals(DEV_UUID_A)) spawnDevNpcA(world, serverPlayer, playerUUID);
    if (playerUUID.equals(DEV_UUID_B)) spawnDevNpcB(world, serverPlayer, playerUUID);

    // GiftMenuHelper.syncOnLogin(serverPlayer); // Descomenta si la clase está lista
  }

  // ── Evento de Cierre de Sesión (Logout) ──────────────────────────────────

  @SubscribeEvent
  public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    Player player = event.getEntity();
    List<BaseNpcEntity> allNpcs = BaseNpcEntity.getAllActive();

    for (BaseNpcEntity npc : allNpcs) {
      boolean ownedByPlayer = player.getUUID().equals(npc.getMasterUUID()) || player.getUUID().equals(npc.getGameUUID());
      boolean isPlayerKobold = npc instanceof PlayerKoboldEntity pk && player.getUUID().equals(pk.getPlayerUUID());

      if (ownedByPlayer || isPlayerKobold) {
        // Notificar a la pareja si aplica
        if (npc.getSexTarget() != null) {
          ServerPlayer partner = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(npc.getSexTarget());
          if (partner != null) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> partner), new IsGirlPacket(true));
            resetPlayerState(partner);
          }
          if (player instanceof ServerPlayer sp) {
            resetPlayerState(sp);
          }
        }

        resetNpcState(npc);
      }

      // Si el NPC pertenece al jugador que se desconecta, limpiar objetivo
      if (isPlayerKobold && npc.getMasterUUID() != null) {
        ServerPlayer master = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(npc.getMasterUUID());
        if (master != null) {
          ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> master), new IsGirlPacket(true));
          resetPlayerState(master);
        }
        npc.setSexTarget(null);
      }
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private void spawnDevNpcA(Level world, ServerPlayer player, UUID uuid) {
    // Asegúrate de importar y adaptar DevNpcA
        /* DevNpcA npc = new DevNpcA(world, uuid);
        npc.setNoGravity(true);
        npc.setInvulnerable(true);
        npc.setNoAi(false);
        npc.setDeltaMovement(0, 0, 0);
        npc.setPos(player.getX(), player.getY() + 69.0, player.getZ());
        world.addFreshEntity(npc);
        npc.init();
        */
  }

  private void spawnDevNpcB(Level world, ServerPlayer player, UUID uuid) {
    // Asegúrate de importar y adaptar DevNpcB
  }

  /**
   * 🚨 SOLUCIÓN AL CRASH DE MEMORIA: Iterar sobre todas las entidades del servidor
   * es mucho más seguro que pedirle a Minecraft que calcule colisiones en una caja de tamaño infinito.
   */
  private void removeStaleNpcs(ServerLevel world, Player player, PlayerKoboldEntity keepNpc) {
    for (Entity entity : world.getAllEntities()) {
      if (entity instanceof PlayerKoboldEntity npc) {
        if (npc.getPlayerUUID().equals(player.getUUID())) {
          if (keepNpc == null || npc.getId() != keepNpc.getId()) {
            npc.discard(); // 1.20.1 Standard para eliminar entidades
          }
        }
      }
    }
  }

  private static void resetPlayerState(ServerPlayer player) {
    player.getAbilities().invulnerable = false;
    player.getAbilities().flying = false;
    player.onUpdateAbilities();
    player.setNoGravity(false);
  }

  private static void resetNpcState(BaseNpcEntity npc) {
    npc.setSexTarget(null);
    npc.setActive(false);
    npc.setAnimState(AnimState.NULL);
    npc.setNoGravity(false);
    npc.setInvulnerable(false);

    // Desatascar al NPC empujándolo hacia arriba
    Vec3 pos = npc.position();
    Level level = npc.level();
    while (level.getBlockState(BlockPos.containing(pos)).isSolidRender(level, BlockPos.containing(pos))) {
      pos = pos.add(0, 1, 0);
    }
    npc.teleportTo(pos.x, pos.y, pos.z);
  }
}