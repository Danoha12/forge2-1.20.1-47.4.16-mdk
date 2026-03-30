package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerKoboldEntity — Portado a 1.20.1.
 * * Entidad "Avatar" que representa al jugador cuando se transforma.
 * * Se mantiene oculta a Y+69 hasta que se activa una escena.
 */
public abstract class PlayerKoboldEntity extends NpcInventoryEntity {

    // Registro global para encontrar qué avatar pertenece a qué jugador
    private static final Map<UUID, PlayerKoboldEntity> BY_OWNER = new ConcurrentHashMap<>();

    public static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(PlayerKoboldEntity.class, EntityDataSerializers.STRING);

    protected PlayerKoboldEntity(EntityType<? extends PlayerKoboldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true); // El avatar no debe morir por daño ambiental
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, "");
    }

    // ── Gestión del Registro (Anti-Memory Leak) ──────────────────────────────

    public static void register(UUID owner, PlayerKoboldEntity entity) {
        BY_OWNER.put(owner, entity);
    }

    @Override
    public void remove(RemovalReason reason) {
        // ¡VITAL! Limpiamos la RAM cuando la entidad desaparece del mundo
        UUID owner = getOwnerUUID();
        if (owner != null) {
            BY_OWNER.remove(owner);
        }
        super.remove(reason);
    }

    @Nullable
    public static PlayerKoboldEntity getByPlayerUUID(UUID owner) {
        return BY_OWNER.get(owner);
    }

    @Nullable
    public UUID getOwnerUUID() {
        String s = this.entityData.get(OWNER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, uuid.toString());
    }

    // ── Lógica de Comportamiento (Tick) ──────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            adjustPlayerView();
            return;
        }

        Player owner = this.getOwnerPlayer();
        if (owner == null) {
            // Si el jugador se desconectó, el avatar se autodestruye
            this.discard();
            return;
        }

        // Sincronizar el inventario visual del jugador al NPC
        syncArmorFromPlayer(owner);

        // LÓGICA DE POSICIONAMIENTO
        if (this.shouldBeAtTargetPos()) {
            Vec3 target = this.getTargetPos();
            this.moveTo(target.x, target.y, target.z, this.getYRot(), this.getXRot());
        } else {
            // Mantener oculto a Y+69
            this.moveTo(owner.getX(), owner.getY() + 69.0, owner.getZ(), owner.getYRot(), owner.getXRot());
        }

        // Sincronización de Cabeza y Cuerpo (Para evitar torsiones de cuello en GeckoLib)
        this.setYHeadRot(owner.getYHeadRot());
        this.setYBodyRot(owner.yBodyRot);

        // Sincronizar estado de ataque/uso
        updateAnimationFromOwner(owner);
    }

    private void updateAnimationFromOwner(Player owner) {
        if (this.getAnimState() == AnimState.NULL && (owner.swinging || owner.isUsingItem())) {
            this.setAnimStateFiltered(AnimState.ATTACK);
        } else if (this.getAnimState() == AnimState.ATTACK && !owner.swinging && !owner.isUsingItem()) {
            this.setAnimStateFiltered(AnimState.NULL);
        }
    }

    private void syncArmorFromPlayer(Player player) {
        this.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST));
        this.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS));
        this.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET));
        this.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem());
        this.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem());
    }

    // ── Interacción y Cámaras ────────────────────────────────────────────────

    public void startInteraction(UUID partnerId) {
        if (this.level().isClientSide) return;

        ServerPlayer partner = (ServerPlayer) this.level().getPlayerByUUID(partnerId);
        Player owner = this.getOwnerPlayer();
        if (partner == null || owner == null) return;

        // Desactivar cámaras para que el jugador real vea a través del NPC
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)owner), new CameraControlPacket(false));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> partner), new CameraControlPacket(false));

        this.setPartnerUUID(partnerId);
        this.setShouldBeAtTargetPos(true);
        this.setTargetPos(partner.position());
    }

    @OnlyIn(Dist.CLIENT)
    private void adjustPlayerView() {
        if (isOwnedByLocalPlayer()) {
            // Lógica de cliente aquí (Invisibilidad, etc.)
        }
    }

    @Nullable
    public Player getOwnerPlayer() {
        UUID uuid = this.getOwnerUUID();
        return uuid == null ? null : this.level().getPlayerByUUID(uuid);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isOwnedByLocalPlayer() {
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(Minecraft.getInstance().player.getUUID());
    }

    public abstract void onActionSelected(String action, UUID playerId);
    public abstract void initDefaultState();
}