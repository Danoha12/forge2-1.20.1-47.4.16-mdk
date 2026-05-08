package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import org.joml.Vector2f;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerKoboldEntity — Portado a 1.20.1.
 * Entidad "Avatar" que representa al jugador cuando se transforma.
 */
public abstract class PlayerKoboldEntity extends NpcInventoryBase {

    private static final Map<UUID, PlayerKoboldEntity> BY_OWNER = new ConcurrentHashMap<>();
    public static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(PlayerKoboldEntity.class, EntityDataSerializers.STRING);

    // ── CAMPOS DE RENDERIZADO ────────────────────────────────────────────────
    public Vector2f footOffset = new Vector2f(0.0F, 0.0F);

    protected PlayerKoboldEntity(EntityType<? extends PlayerKoboldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    // Constructor para registro automático (usado por las subclases como GoblinPlayerKobold)
    public PlayerKoboldEntity(Level level, UUID owner) {
        this((EntityType<? extends PlayerKoboldEntity>)null, level);
        this.setOwnerUUID(owner);
        register(owner, this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, "");
    }

    // ── MÉTODOS DE COMPATIBILIDAD (Para RenderHandlers) ──────────────────────

    public static PlayerKoboldEntity getForPlayer(UUID uuid) {
        return BY_OWNER.get(uuid);
    }

    public static void cleanup() {
        BY_OWNER.clear();
    }

    public static void register(UUID owner, PlayerKoboldEntity entity) {
        BY_OWNER.put(owner, entity);
    }

    /**
     * Puente público para setSharedFlag.
     * Soluciona el error "protected access in Entity".
     */
    public void setFlying(boolean flying) {
        this.setSharedFlag(7, flying); // Flag 7 = Fall Flying
    }

    // ── GESTIÓN DE DUEÑO ─────────────────────────────────────────────────────

    @Override
    public void remove(RemovalReason reason) {
        UUID owner = getOwnerUUID();
        if (owner != null) BY_OWNER.remove(owner);
        super.remove(reason);
    }

    @Nullable
    public UUID getOwnerUUID() {
        String s = this.entityData.get(OWNER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, uuid.toString());
    }

    // ── LÓGICA DE COMPORTAMIENTO ─────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player owner = this.getOwnerPlayer();
        if (owner == null) {
            this.discard();
            return;
        }

        syncArmorFromPlayer(owner);

        if (this.shouldBeAtTargetPos()) {
            Vec3 target = this.getTargetPos();
            this.moveTo(target.x, target.y, target.z, this.getYRot(), this.getXRot());
        } else {
            this.moveTo(owner.getX(), owner.getY() + 69.0, owner.getZ(), owner.getYRot(), owner.getXRot());
        }

        this.setYHeadRot(owner.getYHeadRot());
        this.setYBodyRot(owner.yBodyRot);
    }

    private void syncArmorFromPlayer(Player player) {
        this.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST));
        this.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS));
        this.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET));
        this.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem());
        this.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem());
    }

    @Nullable
    public Player getOwnerPlayer() {
        UUID uuid = this.getOwnerUUID();
        return uuid == null ? null : this.level().getPlayerByUUID(uuid);
    }

    public abstract void onActionSelected(String action, UUID playerId);
    public abstract void initDefaultState();

    // ── UTILIDADES DE ANIMACIÓN ──────────────────────────────────────────────

    public static void exitAnimationForAll() {
        com.trolmastercard.sexmod.client.handler.ClientStateManager.setCanMove(true);
    }

    public static boolean isAnimating(Player player) {
        PlayerKoboldEntity avatar = getForPlayer(player.getUUID());
        return avatar != null && avatar.getAnimState() != AnimState.NULL;
    }
    // 🚨 1. El buscador por UUID (Puedes usar un Map o buscar en la lista de entidades)
    public static PlayerKoboldEntity getByPlayerUUID(java.util.UUID uuid) {
        // Aquí deberías tener una lógica para recuperar la instancia asociada al jugador
        // Por ahora, un ejemplo de cómo se suele implementar:
        return null; // Cambia esto por tu lógica de búsqueda (ej. un HashMap estático)
    }

    // 🚨 2. Índice del modelo de mano
    public int getHandModelIndex() {
        return this.entityData.get(MODEL_INDEX); // O el valor que guardes en tu EntityData
    }

    // 🚨 3. Obtener el hueso del modelo (IBoneAccessor)
    public com.trolmastercard.sexmod.client.model.IBoneAccessor getHandModel(int index) {
        // Aquí devuelves el modelo de GeckoLib convertido a IBoneAccessor
        return null;
    }

    // 🚨 4. Ruta de la textura
    public String getHandTexturePath(int index) {
        return "textures/entity/kobold/hand_default.png"; // Ajusta según tu lógica
    }

    // 🚨 5. Color de la mano (RGB)
    public net.minecraft.core.Vec3i getHandColor(int index) {
        return new net.minecraft.core.Vec3i(255, 255, 255); // Blanco por defecto
    }
    public static void clearAll() {
        // 🚨 AQUÍ ADENTRO DEBES LIMPIAR TUS LISTAS ESTÁTICAS 🚨
        // Si en esta clase tienes variables "static List" o "static Map"
        // donde guardas a los Kobolds de los jugadores, dales .clear() aquí.

        // Ejemplo de cómo se vería si tuvieras un mapa llamado "playerKobolds":
        // if (playerKobolds != null) {
        //     playerKobolds.clear();
        // }
    }
// ── SENSOR DE ESTADO ACTIVO ──
    /**
     * Devuelve true si el Kobold está actualmente en una escena.
     * Requerido por el PlayerCamEventHandler para ocultar las manos.
     */
    public boolean isSexModeActive() {
        // 🚨 Ajusta esta línea dependiendo de cómo guardes el estado de tu Kobold.
        // Si usa el mismo sistema de AnimState que las chicas, sería algo así:
        // return this.getAnimState() != AnimState.NULL;

        // Si no tienes un estado complejo para el Kobold, por ahora ponle un true
        // o devuelve la variable booleana que uses para saber si está interactuando.
        return true;
    }
// ── SENSOR DE SUSPENSIÓN (Efecto Bobbing) ──

    /**
     * Le dice al renderizador si debe aplicar el movimiento de cámara al caminar.
     */
    public boolean isShouldRideOffset() {
        // Queremos que rebote normalmente, EXCEPTO cuando está en una escena (para no romper la cámara).
        // ¡Usamos el sensor que le instalamos hace unos mensajes!
        return !this.isSexModeActive();
    }
}