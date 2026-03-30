package com.trolmastercard.sexmod.data;

import com.mojang.logging.LogUtils;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.OwnershipSyncPacket;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GalathOwnershipData — Portado a 1.20.1.
 * * Maneja el vínculo 1:1 entre un jugador y su chica principal.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GalathOwnershipData extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String KEY = "galath_ownership_data";

    // ── Estado en Memoria ────────────────────────────────────────────────────
    private static final BiMap<UUID, UUID> ownershipMap = new BiMap<>();
    private static final Map<UUID, Long> lastDosageTime = new ConcurrentHashMap<>();
    private static final Set<UUID> mangoOwners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ── API de Propiedad (Ownership) ─────────────────────────────────────────

    public static void setOwnership(Player player, BaseNpcEntity npc) {
        ownershipMap.put(player.getUUID(), npc.getUUID());
        markDirtyStatic();
    }

    public static void removeOwnership(BaseNpcEntity npc) {
        UUID playerUUID = ownershipMap.getByValue(npc.getUUID());
        if (playerUUID != null) {
            ownershipMap.removeByKey(playerUUID);

            ServerPlayer sp = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
            if (sp != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new OwnershipSyncPacket(false));
            }
        }
        npc.remove(Entity.RemovalReason.DISCARDED);
        markDirtyStatic();
    }

    public static boolean hasNpc(UUID playerUUID) {
        return ownershipMap.getByKey(playerUUID) != null;
    }

    @Nullable
    public static UUID getNpcForPlayer(UUID playerUUID) {
        return ownershipMap.getByKey(playerUUID);
    }

    @Nullable
    public static UUID getOwnerForNpc(UUID npcUUID) {
        return ownershipMap.getByValue(npcUUID);
    }

    public static boolean isOwner(Player player, BaseNpcEntity npc) {
        return npc.getUUID().equals(ownershipMap.getByKey(player.getUUID()));
    }

    // ── Lógica de Mango & Cooldown ───────────────────────────────────────────

    public static void markMangoOwned(UUID npcUUID) {
        UUID ownerUUID = ownershipMap.getByValue(npcUUID);
        if (ownerUUID != null) {
            mangoOwners.add(ownerUUID);
            markDirtyStatic();
        }
    }

    public static boolean isMangoOwned(UUID playerUUID) {
        return mangoOwners.contains(playerUUID);
    }

    public static void setLastDosageTime(UUID playerUUID, long worldTime) {
        lastDosageTime.put(playerUUID, worldTime);
        markDirtyStatic();
    }

    // ── Persistencia (NBT) ───────────────────────────────────────────────────

    public static GalathOwnershipData load(CompoundTag tag) {
        GalathOwnershipData data = new GalathOwnershipData();
        ownershipMap.clear();
        lastDosageTime.clear();
        mangoOwners.clear();

        // Forma moderna de leer listas de diccionarios en 1.20.1
        if (tag.contains("OwnershipList", Tag.TAG_LIST)) {
            ListTag list = tag.getList("OwnershipList", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("Player") && entry.hasUUID("NPC")) {
                    UUID pId = entry.getUUID("Player");
                    ownershipMap.put(pId, entry.getUUID("NPC"));
                    if (entry.contains("Cooldown")) {
                        lastDosageTime.put(pId, entry.getLong("Cooldown"));
                    }
                }
            }
        }

        if (tag.contains("MangoList", Tag.TAG_LIST)) {
            ListTag mangoList = tag.getList("MangoList", Tag.TAG_COMPOUND);
            for (int j = 0; j < mangoList.size(); j++) {
                mangoOwners.add(mangoList.getCompound(j).getUUID("UUID"));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, UUID> entry : ownershipMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Player", entry.getKey());
            entryTag.putUUID("NPC", entry.getValue());
            entryTag.putLong("Cooldown", lastDosageTime.getOrDefault(entry.getKey(), 0L));
            list.add(entryTag);
        }
        tag.put("OwnershipList", list);

        ListTag mangoList = new ListTag();
        for (UUID uuid : mangoOwners) {
            CompoundTag uTag = new CompoundTag();
            uTag.putUUID("UUID", uuid);
            mangoList.add(uTag);
        }
        tag.put("MangoList", mangoList);

        return tag;
    }

    // ── Helpers y Eventos ────────────────────────────────────────────────────

    private static void markDirtyStatic() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().overworld();
            overworld.getDataStorage().computeIfAbsent(GalathOwnershipData::load, GalathOwnershipData::new, KEY).setDirty();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Limpieza segura: 200 ticks (10 segundos)
        if (ServerLifecycleHooks.getCurrentServer().getTickCount() % 200 == 0) {
            List<UUID> toRemove = new ArrayList<>();
            ServerLevel world = ServerLifecycleHooks.getCurrentServer().overworld();

            for (Map.Entry<UUID, UUID> entry : ownershipMap.entrySet()) {
                Entity npc = world.getEntity(entry.getValue());
                // IMPORTANTE: Solo borramos el vínculo si la entidad ESTÁ CARGADA y ESTÁ MUERTA.
                // Si getEntity devuelve null, significa que el chunk se descargó, NO que murió.
                if (npc != null && (npc.isRemoved() || !npc.isAlive())) {
                    toRemove.add(entry.getKey());
                }
            }

            if (!toRemove.isEmpty()) {
                for (UUID pId : toRemove) {
                    ownershipMap.removeByKey(pId);
                }
                markDirtyStatic();
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD) {
            ownershipMap.clear();
            lastDosageTime.clear();
            mangoOwners.clear();
        }
    }

    // ── Utilidad BiMap ───────────────────────────────────────────────────────

    private static class BiMap<K, V> implements Iterable<Map.Entry<K, V>> {
        private final Map<K, V> kToV = new ConcurrentHashMap<>();
        private final Map<V, K> vToK = new ConcurrentHashMap<>();

        public void put(K key, V value) {
            kToV.put(key, value);
            vToK.put(value, key);
        }
        public V getByKey(K key) { return kToV.get(key); }
        public K getByValue(V value) { return vToK.get(value); }
        public void removeByKey(K key) {
            V val = kToV.remove(key);
            if (val != null) vToK.remove(val);
        }
        public void clear() { kToV.clear(); vToK.clear(); }
        public Set<Map.Entry<K, V>> entrySet() { return kToV.entrySet(); }
        @Override public Iterator<Map.Entry<K, V>> iterator() { return kToV.entrySet().iterator(); }
    }
}