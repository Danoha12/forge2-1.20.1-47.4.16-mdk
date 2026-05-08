package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.SexAnimationTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity; // Importación necesaria para RemovalReason
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob; // CAMBIADO: Antes estaba en .monster
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseNpcEntity extends PathfinderMob implements GeoEntity, NpcStateAccessor {

    private static final Set<BaseNpcEntity> ALL_ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static Set<BaseNpcEntity> getAllActive() { return ALL_ACTIVE; }

    // DataParameters
    public static final EntityDataAccessor<String> MASTER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> SHOULD_AT_TARGET = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_POS = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> NPC_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> MODEL_INDEX = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> ANIM_STATE = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> PARTNER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> ANIM_TICK = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> INTERACTION_LEVEL = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final java.util.Set<BaseNpcEntity> ALL_NPCS =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static java.util.Set<BaseNpcEntity> getAllNpcs() {
        return ALL_NPCS;
    }
    public float bobScale = 0.0f;
    public Vec3 homePos = Vec3.ZERO;
    public static final EntityDataAccessor<Float> SCALE_PROGRESS = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    protected BaseNpcEntity(EntityType<? extends BaseNpcEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            ALL_ACTIVE.add(this);
            ALL_NPCS.add(this);
        }

    }

    public static List<BaseNpcEntity> getAllWithMaster(UUID masterUUID) {
        return List.of();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_UUID, UUID.randomUUID().toString());
        this.entityData.define(MODEL_INDEX, 0);
        this.entityData.define(ANIM_STATE, AnimState.NULL.name());
        this.entityData.define(PARTNER_UUID, "null");
        this.entityData.define(SHOULD_AT_TARGET, false);
        this.entityData.define(TARGET_YAW, 0.0F);
        this.entityData.define(TARGET_POS, "0|0|0");
        this.entityData.define(MASTER_UUID, "");
        this.entityData.define(INTERACTION_LEVEL, 0.0F);
        this.entityData.define(ANIM_TICK, 0);
        this.entityData.define(SCALE_PROGRESS, 0.25F);
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_IMMOVABLE, false);
    }
    public static final EntityDataAccessor<Boolean> DATA_IMMOVABLE =
            SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public float getInteractionLevel() { return this.entityData.get(INTERACTION_LEVEL); }
    public void setInteractionLevel(float level) { this.entityData.set(INTERACTION_LEVEL, Mth.clamp(level, 0.0F, 1.0F)); }
    public int getAnimTick() { return this.entityData.get(ANIM_TICK); }
    public void setAnimTick(int tick) { this.entityData.set(ANIM_TICK, tick); }

    @Override
    public UUID getSexPartnerUUID() {
        String s = this.entityData.get(PARTNER_UUID);
        return "null".equals(s) ? null : UUID.fromString(s);
    }
    // Añade esto debajo de getSexPartnerUUID()
    public UUID getSexPartner() {
        return this.getSexPartnerUUID();
    }// ── GESTIÓN DEL DUEÑO (MASTER) ──────────────────────────────────────────

    /**
     * Obtiene el UUID del jugador que el NPC está siguiendo.
     */
    @javax.annotation.Nullable
    public UUID getMasterUUID() {
        String s = this.entityData.get(MASTER_UUID);
        // Si el string está vacío o es "null", devolvemos null
        return (s == null || s.isEmpty() || s.equals("null")) ? null : UUID.fromString(s);
    }

    /**
     * Asigna un dueño al NPC mediante su UUID.
     */
    public void setMasterUUID(@javax.annotation.Nullable UUID uuid) {
        this.entityData.set(MASTER_UUID, uuid == null ? "" : uuid.toString());
    }

    @Override
    public void setSexPartnerUUID(@Nullable UUID partnerUUID) {
        this.entityData.set(PARTNER_UUID, partnerUUID == null ? "null" : partnerUUID.toString());
    }

    @Override public int getModelIndex() { return this.entityData.get(MODEL_INDEX); }
    @Override public void setModelIndex(int index) { this.entityData.set(MODEL_INDEX, index); }
    @Override public int getAnimationIndex() { return getAnimState().ordinal(); }

    @Override
    public void setAnimationIndex(int index) {
        AnimState[] states = AnimState.values();
        if (index >= 0 && index < states.length) setAnimStateFiltered(states[index]);
    }

    @Override public int getCumCounter() { return getAnimTick(); }
    @Override public void setCumCounter(int count) { setAnimTick(count); }
    @Override public void setAnimState(AnimState state) { this.setAnimStateFiltered(state); }

    @Override
    public AnimState getAnimState() {
        try { return AnimState.valueOf(this.entityData.get(ANIM_STATE)); }
        catch (Exception e) { return AnimState.NULL; }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasSmoothPos() { return getAnimState() != null && getAnimState().useBoyCam; }

    @OnlyIn(Dist.CLIENT)
    public Vec3 getSmoothPos() {
        float pt = Minecraft.getInstance().getFrameTime();
        return new Vec3(Mth.lerp(pt, this.xo, this.getX()), Mth.lerp(pt, this.yo, this.getY()), Mth.lerp(pt, this.zo, this.getZ()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    // -- Control de Seguimiento --
    public void stopFollow() {
        this.setMaster(""); // Llama al método de abajo con un String vacío
    }

    public void setMaster(String s) {
        this.entityData.set(MASTER_UUID, s);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.4, Ingredient.of(Items.BREAD, Items.WHEAT), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            SexAnimationTracker.serverTick(this);
        }
    }

    public void setAnimStateFiltered(AnimState next) {
        if (getAnimState() == next) return;
        this.entityData.set(ANIM_STATE, next.name());
        setAnimTick(0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcUUID", this.entityData.get(NPC_UUID));
        tag.putString("MasterUUID", this.entityData.get(MASTER_UUID));
        tag.putInt("ModelIndex", getModelIndex());
        tag.putFloat("InteractionLevel", getInteractionLevel());
        tag.putDouble("homeX", homePos.x); tag.putDouble("homeY", homePos.y); tag.putDouble("homeZ", homePos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcUUID")) this.entityData.set(NPC_UUID, tag.getString("NpcUUID"));
        this.entityData.set(MASTER_UUID, tag.getString("MasterUUID"));
        this.setModelIndex(tag.getInt("ModelIndex"));
        this.setInteractionLevel(tag.getFloat("InteractionLevel"));
        this.homePos = new Vec3(tag.getDouble("homeX"), tag.getDouble("homeY"), tag.getDouble("homeZ"));
    }

    public UUID getNpcUUID() { return UUID.fromString(this.entityData.get(NPC_UUID)); }

    // CORREGIDO: RemovalReason es una clase interna de Entity en 1.20.1
    @Override
    public void remove(Entity.RemovalReason reason) {
        ALL_ACTIVE.remove(this);
        super.remove(reason);
        ALL_NPCS.remove(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) { return super.getDimensions(pose).scale(getModelScale()); }

    public float getModelScale() { return 1.0F; }

    public abstract Vec3 getBonePosition(String boneName);

    public abstract void triggerAction(String action, UUID playerId);

    public void setHomePosition(Vec3 snapped) {

    }
// ── GESTIÓN DE ANIMACIONES DESDE EL CLIENTE ──────────────────────────────

    /**
     * Busca al NPC que está interactuando con el jugador y detiene la animación.
     */
    public static void cancelAnimation(UUID playerId) {
        for (BaseNpcEntity npc : ALL_ACTIVE) {
            if (playerId.equals(npc.getSexPartnerUUID())) {
                // Devolvemos al NPC al estado inicial
                npc.setAnimStateFiltered(AnimState.NULL);
                npc.setSexPartnerUUID(null);

                // IMPORTANTE: En 1.20.1, aquí deberías enviar un paquete al servidor
                // para que el cambio sea oficial para todos.
                // ModNetwork.CHANNEL.sendToServer(new AnimationSyncPacket(npc.getUUID(), AnimState.NULL));
            }
        }
    }

    /**
     * Avanza a la siguiente etapa de la animación (ej. de SLOW a FAST).
     */
    public static void advanceAnimation(UUID playerId) {
        for (BaseNpcEntity npc : ALL_ACTIVE) {
            if (playerId.equals(npc.getSexPartnerUUID())) {
                AnimState next = npc.getNextState(npc.getAnimState());
                if (next != null) {
                    npc.setAnimStateFiltered(next);
                }
            }
        }
    }

    // Método helper para que las clases hijas definan su flujo de animación
    public AnimState getNextState(AnimState current) {
        return null; // Se sobrescribe en Jenny, Goblin, etc.
    }
    // -- Integración con GeckoLib 4 --
    @OnlyIn(Dist.CLIENT)
    public AnimationController<?> getMainAnimationController() {
        // Buscamos el controlador de "action" que definimos en las entidades hijas
        return this.getAnimatableInstanceCache()
                .getManagerForId(this.getUUID().hashCode())
                .getAnimationControllers()
                .get("action"); // O "movement", según lo que quieras trackear
    }
    /**
     * Sincroniza visualmente si la entidad está planeando/volando.
     */
    public void setFlying(boolean flying) {
        this.setSharedFlag(7, flying);
    }
    // ── HELPERS DE POSICIONAMIENTO (Añadir a BaseNpcEntity) ──────────────────

    public boolean shouldBeAtTargetPos() {
        return this.entityData.get(SHOULD_AT_TARGET);
    }

    public void setShouldBeAtTargetPos(boolean value) {
        this.entityData.set(SHOULD_AT_TARGET, value);
    }

    public Vec3 getTargetPos() {
        String[] parts = this.entityData.get(TARGET_POS).split("\\|");
        if (parts.length < 3) return Vec3.ZERO;
        return new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    public void setTargetPos(Vec3 pos) {
        this.entityData.set(TARGET_POS, pos.x + "|" + pos.y + "|" + pos.z);
    }

    public float getTargetYaw() {
        return this.entityData.get(TARGET_YAW);
    }

    public void setTargetYaw(float yaw) {
        this.entityData.set(TARGET_YAW, yaw);
    }
    // Añade esto en la parte superior de BaseNpcEntity (o NpcInventoryBase)
    public int maxHealthStat = 20;
    public int armourStat = 0;
    public int attackStat = 2;
    public boolean isInteractiveMode = false;

    // 1. Añade el ID de los trajes (Outfits)
    public static final EntityDataAccessor<Integer> DATA_OUTFIT_INDEX = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);

    // 2. En tu método defineSynchedData(), inicialízalo:
    // this.entityData.define(DATA_OUTFIT_INDEX, 0);

    // 3. Método para obtener al dueño (lo pide GalathModel)
    @javax.annotation.Nullable
    public net.minecraft.world.entity.player.Player getOwnerPlayer() {
        String uuidStr = this.entityData.get(MASTER_UUID);
        if (uuidStr == null || uuidStr.isEmpty()) return null;
        try {
            return this.level().getPlayerByUUID(java.util.UUID.fromString(uuidStr));
        } catch (Exception e) { return null; }
    }

    // 4. Método para offsets de huesos (cinemática inversa)
    public Vec3 getBoneOffset(String boneName) {
        // Por defecto no hay offset, pero Galath lo necesita para sus ojos e IK
        return Vec3.ZERO;
    }
    public float getScaleProgress() {
        return this.entityData.get(SCALE_PROGRESS);
    }
    public void setScaleProgress(float progress) {
        this.entityData.set(SCALE_PROGRESS, progress);
    }
// Dentro de BaseNpcEntity.java

    // 1. Nombre de la entidad
    public String getNpcName() { return "BaseNPC"; }

    // 2. Velocidad de caminata
    public float getWalkSpeed() { return 1.0F; }

    // 3. Control de UI (Solo Cliente)
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderHornyOverlay() { return true; }

    // 4. Lógica de estados de animación (Usando tu enum AnimState)
    @Nullable
    protected AnimState getCumStateFor(AnimState state) { return null; }

    // 5. Lógica de transición de estados
    @Nullable
    protected AnimState getNextAnimStateOnNull(AnimState state) { return null; }
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }
    public void spawnHeartParticles(net.minecraft.world.entity.Entity entity) {
        // Generamos un par de partículas arriba de la cabeza
        for (int i = 0; i < 3; i++) {
            double x = entity.getRandomX(0.5D);
            double y = entity.getRandomY() + 0.5D;
            double z = entity.getRandomZ(0.5D);

            // addParticle solo funciona en el cliente.
            // Si quieres que otros jugadores lo vean en multi, se usa serverLevel.sendParticles
            entity.level().addParticle(
                    ParticleTypes.HEART,
                    x, y, z,
                    0.0D, 0.1D, 0.0D
            );
        }
    }
    public void playSoundEffect(RegistryObject<SoundEvent> sound) {
        this.playSoundEffect(sound, 1.0F, 1.0F);
    }

    public void playSoundEffect(RegistryObject<SoundEvent> sound, float volume, float pitch) {
        // En 1.20.1, playSound maneja la sincronización entre servidor y cliente
        this.level().playSound(
                null,                // Si es null, lo escuchan todos los jugadores cerca
                this.getX(),
                this.getY(),
                this.getZ(),
                sound.get(),         // Sacamos el SoundEvent del RegistryObject
                this.getSoundSource(),
                volume,
                pitch
        );
    }
    @Nullable
    public static BaseNpcEntity getSexPartner(net.minecraft.world.entity.player.Player player) {
        // En un futuro, aquí puedes poner la lógica para buscar si el jugador tiene a alguien.
        // Por ahora, para que el mod compile y no se trabe, le decimos al motor
        // que el jugador siempre está "libre" devolviendo null.
        return null;
    }
    public boolean isLocalPlayer() {
        if (this.level().isClientSide) {
            return checkLocalPlayerClient();
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean checkLocalPlayerClient() {
        // Buscamos al jugador que está sentado frente a la pantalla
        net.minecraft.client.player.LocalPlayer clientPlayer = net.minecraft.client.Minecraft.getInstance().player;

        // Verificamos si existe, si la chica tiene dueño, y si los IDs coinciden
        return clientPlayer != null &&
                this.getOwnerUUID() != null &&
                this.getOwnerUUID().equals(clientPlayer.getUUID());
    }
// ── CONTROL DE CÁMARA Y EVENTOS UI ────────────────────────────────────────

    /**
     * Ajusta el ángulo y posición de la cámara del jugador durante la animación.
     */
    public void sendCameraAngle(double xOffset, double yOffset, double zOffset, float pitch, float yaw) {
        if (this.level().isClientSide) {
            // 💡 NOTA DE MAISTRO:
            // Aquí es donde en un futuro conectarás tu manejador de cámara (CameraHandler o ClientStateManager).
            // Por ahora, el método está aquí para que el mod compile perfectamente y no crashee.
        }
    }

    /**
     * Dispara un evento en la interfaz gráfica (UI) cuando la animación está lista.
     * (¡Reparación preventiva para que no te falle la línea 388!)
     */
    public void triggerNpcUiEvent() {
        if (this.level().isClientSide) {
            // Aquí va la lógica si necesitas que un botón de la UI parpadee o se active.
        }
    }
// ── SISTEMA DE SONIDOS BLINDADO ───────────────────────────────────────────

    // 1. Adaptadores para sonidos de Vanilla (ej. SoundEvents.PLAYER_BREATH)
    public void playSoundEffect(SoundEvent sound) {
        this.playSoundEffect(sound, 1.0F, 1.0F);
    }

    public void playSoundEffect(SoundEvent sound, float volume) {
        this.playSoundEffect(sound, volume, 1.0F); // Pitch por defecto a 1.0F
    }

    public void playSoundEffect(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }
// ── SENSORES DE CÁMARA PARA BOY-CAM (GeckoLib 4) ──

    /**
     * En GeckoLib 4 los huesos viven en el renderizador.
     * Esta es una aproximación usando los ojos de la entidad para evitar crasheos.
     */
    public net.minecraft.world.phys.Vec3 getBoneWorldPosition(String boneName) {
        // En lugar de leer el hueso exacto de la animación, lo anclamos a la cara del NPC
        // mirando un poquito hacia adelante para el POV.
        double x = this.getX();
        double y = this.getY() + this.getEyeHeight();
        double z = this.getZ();

        net.minecraft.world.phys.Vec3 look = this.getLookAngle();
        // Empujamos la cámara medio bloque hacia enfrente basándonos en a dónde mira la chica
        return new net.minecraft.world.phys.Vec3(x + (look.x * 0.5), y, z + (look.z * 0.5));
    }

    /**
     * Posición del tick anterior para interpolar y evitar tirones en la cámara.
     */
    /**
     * Posición del tick anterior para interpolar y evitar tirones en la cámara.
     */
    public net.minecraft.world.phys.Vec3 getLastTickBonePosition(String boneName) {
        double x = this.xo; // xo = xOld en 1.20.1
        double y = this.yo + this.getEyeHeight();
        double z = this.zo;

        return new net.minecraft.world.phys.Vec3(x, y, z);
    }
// ── SENSOR DE ESTADO ACTIVO (Para las chicas) ──

    /**
     * Devuelve true si la chica está actualmente en una escena.
     * Usado por la cámara y otros sistemas para saber si está ocupada.
     */
    public boolean isSexModeActive() {
        // Verificamos que el estado de animación no sea nulo ni el estado "NULL" base
        return this.getAnimState() != null && this.getAnimState() != com.trolmastercard.sexmod.registry.AnimState.NULL;
    }
// ── SISTEMA DE VESTUARIO (Conexión con NpcCustomizeScreen) ──────────────────

    /**
     * 1. Lee la ropa actual (Requerido para clonar al maniquí)
     */
    public java.util.List<String> getClothingSet() {
        // En un futuro, aquí conviertes tu DATA_OUTFIT_INDEX o tu inventario en una lista.
        return new java.util.ArrayList<>();
    }

    /**
     * 2. Le pone la ropa original al clon del probador.
     */
    public void setClothingFromSet(java.util.List<String> clothingSet) {
        // En un futuro, aquí lees la lista y se la aplicas al maniquí.
    }

    /**
     * 3. Cambia una prenda en tiempo real cuando el jugador le da clic a la flechita.
     */
    public void updateClothingVisuals(com.trolmastercard.sexmod.registry.ClothingSlot slot, String clothingName) {
        // En un futuro, aquí actualizas la textura del hueso específico en GeckoLib.
    }
}