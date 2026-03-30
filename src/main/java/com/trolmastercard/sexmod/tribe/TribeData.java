package com.trolmastercard.sexmod.tribe;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TribeData — Portado a 1.20.1.
 * * Representa el estado interno y la memoria colectiva de una tribu.
 */
public class TribeData {

    private final UUID tribeId;
    private final EyeAndKoboldColor color;

    @Nullable private UUID masterUUID;
    @Nullable private KoboldEntity leader;
    @Nullable private BlockPos meetingPoint;

    private TribePhase phase = TribePhase.REST;
    private boolean alarmed = false;

    // ── Colecciones ──────────────────────────────────────────────────────────
    private final List<KoboldEntity> members = Collections.synchronizedList(new ArrayList<>());
    private final Set<BlockPos> bedPositions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<BlockPos> chestPositions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<TribeTask> tasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, BlockPos> knownPositions = new ConcurrentHashMap<>();
    private final Set<LivingEntity> threats = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TribeData(UUID tribeId, EyeAndKoboldColor color) {
        this.tribeId = tribeId;
        this.color = color;
    }

    public TribeData(UUID tribeId, EyeAndKoboldColor color, KoboldEntity leader, List<KoboldEntity> initialMembers) {
        this(tribeId, color);
        this.leader = leader;
        this.members.addAll(initialMembers);
    }

    // ── Gestión de Liderazgo ─────────────────────────────────────────────────

    /**
     * El liderazgo Kobold es meritocrático por tamaño: el más pequeño manda.
     */
    public void updateLeader() {
        if (this.leader == null || this.leader.isRemoved()) {
            this.leader = findSmallestMember();
            if (this.leader != null) {
                this.leader.setLeader(true);
            }
        }
    }

    @Nullable
    public KoboldEntity findSmallestMember() {
        return members.stream()
                .filter(k -> !k.isRemoved() && k.isAlive())
                .min(Comparator.comparingDouble(KoboldEntity::getModelScale))
                .orElse(null);
    }

    // ── Gestión de Amenazas ──────────────────────────────────────────────────

    public void addThreat(LivingEntity entity) {
        if (entity != null && entity.isAlive()) {
            this.threats.add(entity);
            this.setAlarmed(true);
        }
    }

    /**
     * Limpia entidades muertas o descargadas de la lista de amenazas.
     */
    public void cleanupThreats() {
        this.threats.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
        if (this.threats.isEmpty()) {
            this.setAlarmed(false);
        }
    }

    // ── Getters y Setters ────────────────────────────────────────────────────

    public UUID getTribeId() { return tribeId; }
    public EyeAndKoboldColor getColor() { return color; }

    @Nullable public UUID getMasterUUID() { return masterUUID; }
    public void setMasterUUID(@Nullable UUID masterUUID) { this.masterUUID = masterUUID; }

    @Nullable public KoboldEntity getLeader() { return leader; }
    public void setLeader(@Nullable KoboldEntity leader) { this.leader = leader; }

    public TribePhase getPhase() { return phase; }
    public void setPhase(TribePhase phase) { this.phase = phase; }

    public boolean isAlarmed() { return alarmed; }
    public void setAlarmed(boolean alarmed) { this.alarmed = alarmed; }

    public List<KoboldEntity> getMembers() { return members; }
    public Set<BlockPos> getBedPositions() { return bedPositions; }
    public Set<BlockPos> getChestPositions() { return chestPositions; }
    public Set<TribeTask> getTasks() { return tasks; }
    public Map<UUID, BlockPos> getKnownPositions() { return knownPositions; }
    public Set<LivingEntity> getThreats() { return threats; }

    public void addMember(KoboldEntity kobold) {
        if (!members.contains(kobold)) members.add(kobold);
    }

    public void removeMember(KoboldEntity kobold) {
        members.remove(kobold);
        if (kobold == leader) updateLeader();
    }
}