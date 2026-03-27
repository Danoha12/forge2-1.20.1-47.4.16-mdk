package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.network.packet.StartInteractionAnimationPacket; // El real (Enmascarado)
import net.minecraft.client.CameraType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerKoboldEntity - Gestor de Disfraces/Avatares de Jugador.
 * Portado a 1.20.1 y purgado de clases fantasma.
 */
public abstract class PlayerKoboldEntity extends NpcInventoryEntity {

    // =========================================================================
    //  Registro Estático Global
    // =========================================================================

    public static final Map<UUID, PlayerKoboldEntity> SERVER_BY_OWNER = new ConcurrentHashMap<>();
    public static final Map<UUID, PlayerKoboldEntity> CLIENT_BY_OWNER = new ConcurrentHashMap<>();

    public static final List<PlayerKoboldEntity> SERVER_PENDING = new ArrayList<>();
    public static final List<PlayerKoboldEntity> CLIENT_PENDING = new ArrayList<>();

    public static boolean allowFlying = true;

    @Nullable
    public static PlayerKoboldEntity getByOwner(UUID playerUUID, Level level) {
        flushPendingRegistry(level.isClientSide());
        return level.isClientSide() ? CLIENT_BY_OWNER.get(playerUUID) : SERVER_BY_OWNER.get(playerUUID);
    }

    @Nullable
    public static PlayerKoboldEntity getByOwner(Player player) {
        return getByOwner(player.getGameProfile().getId(), player.level());
    }

    public static boolean hasKobold(UUID playerUUID, Level level) {
        flushPendingRegistry(level.isClientSide());
        return level.isClientSide() ? CLIENT_BY_OWNER.containsKey(playerUUID) : SERVER_BY_OWNER.containsKey(playerUUID);
    }

    public static boolean isPlayerFemale(UUID playerUUID, Level level) {
        return getByOwner(playerUUID, level) != null;
    }

    public static void flushPendingRegistry(boolean isClient) {
        List<PlayerKoboldEntity> pendingList = isClient ? CLIENT_PENDING : SERVER_PENDING;
        Map<UUID, PlayerKoboldEntity> ownerMap = isClient ? CLIENT_BY_OWNER : SERVER_BY_OWNER;

        synchronized (pendingList) {
            List<PlayerKoboldEntity> done = new ArrayList<>();
            for (PlayerKoboldEntity e : pendingList) {
                UUID owner = e.getOwnerUUID();
                if (owner != null) {
                    ownerMap.put(owner, e);
                    done.add(e);
                }
            }
            pendingList.removeAll(done);
        }
        ownerMap.entrySet().removeIf(entry -> entry.getValue().isRemoved());
    }

    @Nullable
    public static PlayerKoboldEntity getByNpcUUID(UUID npcUUID, Level level) {
        for (BaseNpcEntity npc : getAllNpcs()) {
            if (npc.level() == level && npc instanceof PlayerKoboldEntity pk && npcUUID.equals(pk.getUUID())) {
                return pk;
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void triggerInteractionEndForLocalPlayer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        PlayerKoboldEntity pk = getByOwner(mc.player.getGameProfile().getId(), mc.level);
        if (pk == null) return;
        pk.onInteractionEndClient();
    }

    // =========================================================================
    //  DataParameters
    // =========================================================================

    public static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(PlayerKoboldEntity.class, EntityDataSerializers.STRING);

    // =========================================================================
    //  Campos
    // =========================================================================

    int clothingTimer = -1;
    public boolean actionSent = true;

    protected PlayerKoboldEntity(EntityType<? extends PlayerKoboldEntity> type, Level level) {
        super(type, level);
        addToPending(level.isClientSide());
    }

    protected PlayerKoboldEntity(Level level) {
        this(ModEntities.PLAYER_KOBOLD.get(), level);
    }

    protected PlayerKoboldEntity(Level level, UUID ownerUUID) {
        this(level);
        entityData.set(OWNER_UUID, ownerUUID.toString());
    }

    private void addToPending(boolean isClient) {
        if (isClient) CLIENT_PENDING.add(this);
        else SERVER_PENDING.add(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_UUID, "");
    }

    @Nullable public UUID getOwnerUUID() {
        String s = entityData.get(OWNER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    @Nullable public Player getOwnerPlayer() {
        UUID id = getOwnerUUID();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isOwnedByLocalPlayer() {
        UUID owner = getOwnerUUID();
        if (owner == null) return false;
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null && owner.equals(mc.player.getGameProfile().getId());
    }

    // =========================================================================
    //  Abstract & Overrides
    // =========================================================================

    public abstract void onActionSelected(String action, UUID playerId);
    public abstract Object createHandModel(int slot);
    public abstract String getHandTexturePath(int slot);

    @Nullable public AnimState getNextState(AnimState current) { return null; }
    @Nullable public AnimState getFinishState(AnimState current) { return null; }
    public boolean autoUnlockInteraction() { return true; }
    public boolean canQueueFollowUp() { return false; }

    @OnlyIn(Dist.CLIENT)
    public void onContextMenuOpen() {}

    public boolean onInteractionActionRequest(String action) { return false; }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return getModelScale();
    }

    public float getModelScale() { return 1.0F; }

    @Override
    public net.minecraft.network.chat.Component getName() {
        UUID ownerId = getOwnerUUID();
        if (ownerId != null) {
            Player owner = level().getPlayerByUUID(ownerId);
            if (owner != null) return owner.getName();
        }
        return net.minecraft.network.chat.Component.literal("Miembro Anónimo de la Tribu");
    }

    // =========================================================================
    //  Tick
    // =========================================================================

    @Override
    public void tick() {
        setNoGravity(true);
        noPhysics = true;
        super.tick();
        tickClothingTimer();
        if (level().isClientSide()) adjustLocalPlayerEyeHeight();
    }

    @Override
    public void aiStep() {
        flushPendingRegistry(level().isClientSide());

        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return;

        Player owner = level().getPlayerByUUID(ownerId);
        if (owner == null) {
            teleportTo(getX(), 0, getZ());
            return;
        }

        if (!level().isClientSide()) syncClothingFromPlayer(owner);

        moveTo(owner.getX(), owner.getY(), owner.getZ(), 0, 0);

        AnimState cur = getAnimState();
        if (!level().isClientSide()) {
            if (cur == AnimState.NULL && owner.isUsingItem()) setAnimState(AnimState.ATTACK);
            if (cur == AnimState.ATTACK && !owner.isUsingItem()) setAnimState(AnimState.NULL);
        }
    }

    void tickClothingTimer() {
        if (clothingTimer == -1) return;
        clothingTimer++;
        if (clothingTimer < 100) return;
        if (getAnimState() != AnimState.STRIP) return;

        if (level().isClientSide()) {
            onInteractionEndClient();
        } else {
            setAnimState(AnimState.NULL);
        }
    }

    @Override
    public void setAnimState(AnimState next) {
        if (next == AnimState.STRIP) {
            clothingTimer = level().isClientSide() ? 5 : 0;
        }
        super.setAnimState(next);
    }

    // =========================================================================
    //  Iniciación y Fin de Interacción
    // =========================================================================

    public void initiateInteraction(UUID partnerPlayerId) {
        if (level().isClientSide()) return;
        var partnerPlayer = (net.minecraft.server.level.ServerPlayer) level().getPlayerByUUID(partnerPlayerId);
        UUID ownerId = getOwnerUUID();
        var ownerPlayer = ownerId != null ? (net.minecraft.server.level.ServerPlayer) level().getPlayerByUUID(ownerId) : null;

        if (partnerPlayer == null) return;

        if (ownerPlayer != null)
            ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> ownerPlayer), new CameraControlPacket(false));
        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> partnerPlayer), new CameraControlPacket(false));

        setYRot(0); yBodyRot = 0; yHeadRot = 0;
        partnerPlayer.setYRot(180);
        partnerPlayer.yBodyRot = 180;
        partnerPlayer.setNoGravity(true);
        partnerPlayer.noPhysics = true;

        Vec3 npcPos = position();
        partnerPlayer.teleportTo(npcPos.x, npcPos.y, npcPos.z + 1);
        partnerPlayer.getAbilities().flying = true;
        if (ownerPlayer != null) ownerPlayer.getAbilities().flying = true;
        partnerPlayer.onUpdateAbilities();

        teleportTo(npcPos.x, npcPos.y, npcPos.z);
    }

    @OnlyIn(Dist.CLIENT)
    public void onInteractionEndClient() {
        boolean isOwner = isOwnedByLocalPlayer();
        if (isOwner) {
            // Restauramos la cámara y movimiento del jugador local de forma limpia (Sin clases inventadas)
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setPose(Pose.STANDING);
                mc.player.setNoGravity(false);
            }
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    public void syncClothingFromPlayer(Player player) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty() || stack.getItem() instanceof net.minecraft.world.item.ElytraItem) continue;
            if (!(stack.getItem() instanceof ArmorItem armor)) continue;
            // switch (armor.getEquipmentSlot()) ...
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean onPlayerInteract(Player player) { return false; }

    @Nullable public Player getTargetPlayer() {
        Vec3 offset = position();
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        UUID ownerId = getOwnerUUID();
        for (Player p : level().players()) {
            if (ownerId != null && p.getGameProfile().getId().equals(ownerId)) continue;
            double d = p.distanceToSqr(offset.x, offset.y, offset.z);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isLocalPlayerNearest() {
        Player nearest = getTargetPlayer();
        if (nearest == null) return false;
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null && nearest.getGameProfile().getId().equals(mc.player.getGameProfile().getId());
    }

    protected boolean isPartnerFemale(UUID playerUUID) { return isPlayerFemale(playerUUID, this.level()); }

    protected void stopPlayerMovement(net.minecraft.server.level.ServerPlayer sp, boolean teleport) {
        sp.setDeltaMovement(Vec3.ZERO);
        if (teleport) {
            Vec3 offset = position().add(0.35, 0, 0);
            sp.teleportTo(offset.x, offset.y, offset.z);
        }
    }

    protected void sendFaceInteractionRequest(UUID partnerId) {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return;
        // Aquí usamos el paquete real que confirmaste que existe, pero lo hemos enmascarado
        ModNetwork