package com.trolmastercard.sexmod.tribe;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.ai.TribeAttackGoal;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.TribeHighlightPacket;
import com.trolmastercard.sexmod.registry.EyeAndKoboldColor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    // El color se decide al azar para toda la tribu
    EyeAndKoboldColor color = EyeAndKoboldColor.values()[rng.nextInt(EyeAndKoboldColor.values().length)];
    TribeData data = new TribeData(tribeId, color);
    tribes.put(tribeId, data);

    // Spawnear al líder y 3 seguidores
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

  // ── Eventos de Territorio (Camas y Cofres) ───────────────────────────────

  @SubscribeEvent
  public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) return;

    BlockState state = event.getState();
    BlockPos pos = event.getPos();

    // Si el jugador pone una cama cerca de su tribu, la tribu la reclama
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
    // Evita que el jugador duerma en camas que pertenecen a los Kobolds
    if (isBedClaimedByKobolds(event.getPos())) {
      event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
      event.getEntity().displayClientMessage(Component.literal("§cEsta cama le pertenece a la tribu."), true);
    }
  }

  // ── Persistencia (World Saved Data) ──────────────────────────────────────

  public static class TribesWorldData extends SavedData {
    @Override
    public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      tribes.forEach((id, data) -> {
        CompoundTag tTag = new CompoundTag();
        tTag.putUUID("Id", id);
        tTag.putString("Color", data.getColor().name());
        if (data.getMasterUUID() != null) tTag.putUUID("Master", data.getMasterUUID());

        // Guardar posiciones de bloques (Long arrays son más rápidos)
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
        UUID id = tTag.getUUID("Id");
        TribeData data = new TribeData(id, EyeAndKoboldColor.valueOf(tTag.getString("Color")));
        if (tTag.hasUUID("Master")) data.setMasterUUID(tTag.getUUID("Master"));

        for (long bPos : tTag.getLongArray("Beds")) data.getBedPositions().add(BlockPos.of(bPos));

        tribes.put(id, data);
      }
      return new TribesWorldData();
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

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
}