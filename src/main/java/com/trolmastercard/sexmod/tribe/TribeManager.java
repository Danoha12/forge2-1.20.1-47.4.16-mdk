package com.trolmastercard.sexmod.tribe;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable; // ✅ REPARACIÓN: Import añadido
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 🚨 REPARACIÓN: Comentamos este paquete porque aún no existe en tu carpeta network
import com.trolmastercard.sexmod.network.packet.TribeHighlightPacket;

/**
 * TribeManager — Portado a 1.20.1.
 * * Cerebro logístico de las Tribus Kobold.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TribeManager {

  private static final Map<UUID, TribeData> tribes = new ConcurrentHashMap<>();
  public static final String DATA_ID = "sexmod_tribes";

  // ── Lógica de Spawn ──────────────────────────────────────────────────────

  public static void spawnTribeAt(ServerLevel level, Vec3 pos) {
    UUID tribeId = UUID.randomUUID();
    RandomSource rng = level.getRandom();

    EyeAndKoboldColor color = EyeAndKoboldColor.values()[rng.nextInt(EyeAndKoboldColor.values().length)];
    TribeData data = new TribeData(tribeId, color);
    tribes.put(tribeId, data);

    for (int i = 0; i < 4; i++) {
      float size = (i == 0) ? 0.25F : KoboldEntity.randomBodySize(rng);
      KoboldEntity kobold = new KoboldEntity(level, tribeId, size, i == 0);

      Vec3 offset = i == 0 ? Vec3.ZERO : new Vec3(rng.nextDouble() - 0.5, 0, rng.nextDouble() - 0.5);
      kobold.moveTo(pos.x + offset.x, pos.y, pos.z + offset.z, rng.nextFloat() * 360F, 0);

      level.addFreshEntity(kobold);
      data.addMember(kobold);
    }
    markDirty();
  }

  // ── Eventos de Territorio ────────────────────────────────────────────────

  @SubscribeEvent
  public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) return;
    BlockState state = event.getState();
    BlockPos pos = event.getPos();

    if (state.is(BlockTags.BEDS)) {
      UUID tribeId = findTribeOwnedBy(player.getUUID());
      if (tribeId != null) {
        addBedToTribe(tribeId, pos);
        player.sendSystemMessage(Component.literal("§aTribu: Cama reclamada."));
        markDirty();
      }
    }
  }

  @SubscribeEvent
  public static void onBlockBreak(BlockEvent.BreakEvent event) {
    BlockPos pos = event.getPos();
    for (TribeData data : tribes.values()) {
      if (data.getBedPositions().remove(pos) || data.getChestPositions().remove(pos)) {
        markDirty();
        break;
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerSleep(PlayerSleepInBedEvent event) {
    if (isBedClaimedByKobolds(event.getPos())) {
      event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
      event.getEntity().displayClientMessage(Component.literal("§cEsta cama le pertenece a la tribu."), true);
    }
  }

  // ── Puentes de Comunicación (Getters para KoboldEntity) ──────────────────

  public static void heartbeat(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    if (data != null) { data.cleanupThreats(); }
  }

  public static void trackPlayer(UUID tribeId, UUID playerUUID) {
    TribeData data = tribes.get(tribeId);
    if (data != null) { data.setMasterUUID(playerUUID); }
  }

  public static boolean isDefender(UUID tribeId, KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    return data != null; // Por ahora todos defienden si la tribu existe
  }

  public static boolean hasEnemies(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    return data != null && data.isAlarmed();
  }

  public static java.util.Set<net.minecraft.world.entity.LivingEntity> getEnemies(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    return data != null ? data.getThreats() : Collections.emptySet();
  }

  public static void removeEnemy(UUID tribeId, net.minecraft.world.entity.LivingEntity enemy) {
    TribeData data = tribes.get(tribeId);
    if (data != null) { data.getThreats().remove(enemy); }
  }

  public static TribePhase getPhase(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    return data != null ? data.getPhase() : TribePhase.ACTIVE;
  }

  public static void setPhase(UUID tribeId, TribePhase phase) {
    TribeData data = tribes.get(tribeId);
    if (data != null) { data.setPhase(phase); markDirty(); }
  }

  public static List<Task> getTasks(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    // 🚨 IMPORTANTE: Esto asume que en TribeData cambiaste Set<TribeTask> por Set<Task>
    return data != null ? new ArrayList<>(data.getTasks()) : Collections.emptyList();
  }

  public static void clearWorkTarget(java.util.UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    if (data != null) {
      // Por ahora lo dejamos como un "recibido", para que el Kobold compile.
      // Si en tu TribeData añades un 'private BlockPos workTarget', aquí pondrías:
      // data.setWorkTarget(null);
    }
  }
  public static void addTask(UUID tribeId, Task task) {
    TribeData data = tribes.get(tribeId);
    if (data != null) { data.getTasks().add(task); markDirty(); }
  }

  public static Set<BlockPos> getChests(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    return data != null ? data.getChestPositions() : Collections.emptySet();
  }

  public static Set<BlockPos> getBeds(UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    return data != null ? data.getBedPositions() : Collections.emptySet();
  }

  // ── Persistencia y Helpers ───────────────────────────────────────────────

  public static void markDirty() {
    ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
    level.getDataStorage().computeIfAbsent(TribesWorldData::load, TribesWorldData::new, DATA_ID).setDirty();
  }

  private static boolean isBedClaimedByKobolds(BlockPos pos) {
    return tribes.values().stream().anyMatch(d -> d.getBedPositions().contains(pos));
  }

  @Nullable
  private static UUID findTribeOwnedBy(UUID playerID) {
    return tribes.entrySet().stream()
            .filter(e -> playerID.equals(e.getValue().getMasterUUID()))
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
  }

  public static void addBedToTribe(UUID tribeId, BlockPos pos) {
    TribeData data = tribes.get(tribeId);
    if (data != null) data.getBedPositions().add(pos);
  }
  public static void removeMember(java.util.UUID tribeId, com.trolmastercard.sexmod.entity.KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    if (data != null) {
      data.removeMember(kobold); // Llama al método que ya tienes en TribeData
      markDirty(); // Guardamos los cambios en el mundo
    }
  }
  public static class TribesWorldData extends SavedData {
    @Override
    public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      tribes.forEach((id, data) -> {
        CompoundTag tTag = new CompoundTag();
        tTag.putUUID("Id", id);
        tTag.putString("Color", data.getColor().name());
        if (data.getMasterUUID() != null) tTag.putUUID("Master", data.getMasterUUID());
        long[] beds = data.getBedPositions().stream().mapToLong(BlockPos::asLong).toArray();
        tTag.putLongArray("Beds", beds);
        list.add(tTag);
      });
      tag.put("Tribes", list);
      return tag;
    }

    public static TribesWorldData load(CompoundTag tag) {
      tribes.clear();
      ListTag list = tag.getList("Tribes", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
        CompoundTag tTag = list.getCompound(i);
        TribeData data = new TribeData(tTag.getUUID("Id"), EyeAndKoboldColor.valueOf(tTag.getString("Color")));
        if (tTag.hasUUID("Master")) data.setMasterUUID(tTag.getUUID("Master"));
        for (long bPos : tTag.getLongArray("Beds")) data.getBedPositions().add(BlockPos.of(bPos));
        tribes.put(data.getTribeId(), data);
      }
      return new TribesWorldData();
    }
  }
  public static EyeAndKoboldColor getTribeColor(java.util.UUID tribeId) {
    TribeData data = tribes.get(tribeId);
    // Si la tribu existe, mandamos su color, si no, uno por defecto (ej. WHITE)
    return data != null ? data.getColor() : EyeAndKoboldColor.SILVER;
  }
  public static boolean isLeader(UUID tribeId, KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    // Comparamos si el bicho es el mismo que tenemos guardado como líder
    return data != null && kobold.equals(data.getLeader());
  }

  /**
   * 🚨 REPARACIÓN: Verifica si la tribu ya está cargada en la memoria.
   */
  public static boolean tribeExists(UUID tribeId) {
    return tribes.containsKey(tribeId);
  }

  /**
   * 🚨 REPARACIÓN: Crea una nueva tribu manualmente (útil al cargar el mundo).
   */
  public static void createTribe(UUID tribeId, EyeAndKoboldColor color) {
    if (!tribes.containsKey(tribeId)) {
      TribeData data = new TribeData(tribeId, color);
      tribes.put(tribeId, data);
      markDirty();
    }
  }
  public static void addMember(java.util.UUID tribeId, com.trolmastercard.sexmod.entity.KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    if (data != null) {
      data.addMember(kobold);
      markDirty();
    }
  }

  /**
   * 🚨 REPARACIÓN: Asigna a un Kobold específico como el líder de la tribu.
   */
  public static void setLeader(java.util.UUID tribeId, com.trolmastercard.sexmod.entity.KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    if (data != null) {
      data.setLeader(kobold);
      markDirty();
    }
  }
  public static void initializeNewMember(java.util.UUID tribeId, com.trolmastercard.sexmod.entity.KoboldEntity kobold) {
    TribeData data = tribes.get(tribeId);
    if (data != null) {
      // 1. Si tu KoboldEntity tiene un método para guardar su ID, se lo ponemos:
      // kobold.setTribeId(tribeId);

      // 2. Lo agregamos oficialmente a la base de datos de la tribu
      data.addMember(kobold);

      // 3. Guardamos los cambios en el mundo
      markDirty();
    }
  }
}
