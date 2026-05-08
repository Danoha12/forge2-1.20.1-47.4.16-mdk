package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.client.gui.TransitionScreen;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

/**
 * GoblinEntity — Portado a 1.20.1.
 * REPARADO: Fusión de mecánicas completas (Reina, Cargar, Aventar) con sintaxis moderna.
 */
public class GoblinEntity extends NpcModelCodeEntity implements GeoEntity { // Agrega 'GoblinInterface' si lo usas

    // -- Constantes --
    static final int STAND_UP_TICKS = 37;
    static final int PICK_UP_ANGLE  = 45;
    static final int BREEDING_TICKS = 8400;

    // -- Parámetros Sincronizados --
    public static final EntityDataAccessor<String> CARRIER_UUID = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> QUEEN_UUID = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_TAMED = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_PREGNANT = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER_BIND_UUID =
            SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    // -- Variables de Instancia --
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    public boolean isQueen = false;
    public float throneRot = 0.0F;
    public Vec3 thronePos = Vec3.ZERO;
    public long impregnationTick = -1L;
    public List<UUID> guardIds = new ArrayList<>();

    public int standUpTimer = 0;
    public int pickupAngleTimer = -1;
    public int frameIdx = -1;
    public int subFrame = -1;

    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CARRIER_UUID, "");
        this.entityData.define(QUEEN_UUID, "");
        this.entityData.define(IS_TAMED, false);
        this.entityData.define(IS_PREGNANT, false);
        this.entityData.define(OWNER_UUID, "");
        this.entityData.define(PLAYER_BIND_UUID, Optional.empty());
    }
    public java.util.UUID getPlayerBindUUID() {
        return this.entityData.get(PLAYER_BIND_UUID).orElse(null);
    }
    @Override
    protected void registerGoals() {
        // En 1.20.1 NpcFollowPlayerGoal debe coincidir con tu implementación
        // this.followPlayerGoal = new NpcFollowPlayerGoal(this, Player.class, 2.0F, 1.0F);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // this.goalSelector.addGoal(5, followPlayerGoal);
    }

    // ── Lógica de Interacción (1.20.1 InteractionResult) ──────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;
        if (isQueen) return InteractionResult.CONSUME;

        // Lógica de captura (BJ)
        if (getAnimState() == AnimState.RUN) {
            if (this.distanceTo(player) > 3.5F) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("get a bit closer..."), true);
            } else {
                setHomePos(player.blockPosition());
                setYRot(player.getYRot());
                setAnimState(AnimState.CATCH);

                // 🚨 REPARADO: Se usa setSexPartnerUUID en lugar de setPartnerUUID
                setOwnerUUID(player.getUUID());
                setSexPartnerUUID(player.getUUID());
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
            }
            return InteractionResult.SUCCESS;
        }

        // Lógica de Carga (Pick Up)
        if (getCarrierUUID() == null) {
            setCarrierUUID(player.getUUID());
            setAnimState(AnimState.PICK_UP);
            this.entityData.set(IS_TAMED, true);
            return InteractionResult.SUCCESS;
        } else if (isCarriedBy(player.getUUID())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("you are already carrying a Goblin"), true);
        }

        return super.mobInteract(player, hand);
    }

    // ── Lógica de Ticks (Mecánicas) ──────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        tickCarry();
        tickStandUp();
        tickThrown();
        tickBreeding();
    }

    private void tickStandUp() {
        if (getAnimState() != AnimState.STAND_UP) return;
        if (++standUpTimer >= STAND_UP_TICKS) {
            standUpTimer = 0;
            setAnimState(AnimState.NULL);
        }
    }

    private void tickThrown() {
        if (getAnimState() != AnimState.THROWN) return;
        if (!onGround()) return;
        int counter = frameIdx < 0 ? 0 : frameIdx + 1;
        frameIdx = counter;
        if (counter >= 3) {
            frameIdx = 0;
            setAnimState(AnimState.AWAIT_PICK_UP);
        }
    }

    private void tickBreeding() {
        if (!isQueen || impregnationTick < 0) return;
        if (level().getGameTime() - impregnationTick < BREEDING_TICKS) return;

        impregnationTick = -1L;
        getEntityData().set(IS_PREGNANT, false);

        if (!level().isClientSide()) {
            GoblinEntity baby = new GoblinEntity(ModEntityRegistry.GOBLIN.get(), level()); // Asegúrate de tener esto en tu Registry
            baby.moveTo(getX() + (random.nextBoolean() ? 1 : -1) * random.nextFloat(), getY(), getZ() + (random.nextBoolean() ? 1 : -1) * random.nextFloat());
            level().addFreshEntity(baby);
        }
    }

    private void tickCarry() {
        AnimState state = getAnimState();
        if (state != AnimState.PICK_UP && state != AnimState.SHOULDER_IDLE) return;
        if (getCarrierUUID() == null || level().getPlayerByUUID(getCarrierUUID()) == null) {
            setAnimState(AnimState.NULL);
        }
    }

    // ── Utilidades de UUID ───────────────────────────────────────────────────

    @Nullable
    public UUID getCarrierUUID() {
        String s = this.entityData.get(CARRIER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setCarrierUUID(@Nullable UUID id) {
        this.entityData.set(CARRIER_UUID, id == null ? "" : id.toString());
    }

    public boolean isCarriedBy(UUID playerId) {
        return playerId.equals(getCarrierUUID());
    }
// ── Utilidades de Posición ───────────────────────────────────────────────

    public void setHomePos(BlockPos pos) {
        // Guardamos la posición en los datos sincronizados de la clase base
        this.entityData.set(HOME_POS, pos);
    }

    public BlockPos getHomePos() {
        return this.entityData.get(HOME_POS);
    }
    // ── Disparadores de Acción ───────────────────────────────────────────────

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "take ur stuff back" -> setAnimState(AnimState.START_THROWING);
            case "use her"            -> startSexMode(playerId, true);
        }
    }

    public void startSexMode(UUID playerId, boolean front) {
        if (front) {
            frameIdx = 0;
        } else {
            subFrame = 0;
        }
        TransitionScreen.show();
        ClientStateManager.setFreeze(false);
        // 🚨 REPARADO: Se usa setSexPartnerUUID
        setSexPartnerUUID(playerId);
    }

    // ── Controladores GeckoLib 4 ─────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var movement = new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenLoop(state.isMoving() ? "animation.goblin.walk" : "animation.goblin.idle"));
        });

        var action = new AnimationController<>(this, "action", 0, this::actionController);

        action.setSoundKeyframeHandler(event -> {
            if (event.getKeyframeData().getSound().equals("cumSound")) {
                this.playSound(ModSounds.MISC_CUMINFLATION[this.random.nextInt(ModSounds.MISC_CUMINFLATION.length)].get(), 1.0F, 1.0F); // Asegúrate de que el sonido exista en tu registro
            }
        });

        registrar.add(movement, action);
    }

    private PlayState actionController(software.bernie.geckolib.core.animation.AnimationState<GoblinEntity> state) {
        AnimState anim = getAnimState();
        if (anim == null || anim == AnimState.NULL) return PlayState.STOP;

        boolean firstPerson = false;
        if (level().isClientSide()) {
            firstPerson = isClientFirstPerson();
        }

        String suffix = firstPerson ? "firstperson" : "thirdperson";

        RawAnimation raw = switch (anim) {
            case PICK_UP -> RawAnimation.begin().thenPlay("animation.goblin.pick_up_" + suffix);
            case PAIZURI_START -> RawAnimation.begin().thenPlay("animation.goblin.paizuri_start");
            case PAIZURI_FAST -> RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast");
            case NELSON_INTRO -> RawAnimation.begin().thenPlay("animation.goblin.nelson_intro");
            case START_THROWING -> RawAnimation.begin().thenPlay("animation.goblin.throw_" + suffix);
            case THROWN -> RawAnimation.begin().thenPlay("animation.goblin.thrown");
            case SIT -> RawAnimation.begin().thenLoop("animation.goblin.sit");
            case CATCH -> RawAnimation.begin().thenPlay("animation.goblin.catch_" + suffix);
            default -> RawAnimation.begin().thenLoop("animation.goblin.idle");
        };

        return state.setAndContinue(raw);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isClientFirstPerson() {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    @Override
    protected String buildInitialCode(StringBuilder sb) {
        return "";
    }
// ── Requisito de NpcModelCodeEntity ──────────────────────────────────────

    @Override
    public void onModelCodeChanged() {
        // Esta lógica se activa cuando el "ADN" del NPC cambia (color, pelo, etc.)
        if (this.level().isClientSide()) {
            // Aquí es donde normalmente se le avisa al Renderer que refresque la textura.
            // Por ahora lo dejamos vacío para que el compilador te deje pasar.
            // Ejemplo futuro si tienes un caché visual: GoblinRenderer.clearCache(this);
        }
    }
// ── Requisito de BaseNpcEntity ───────────────────────────────────────────

    // ── Requisito de BaseNpcEntity (Versión Avanzada / Real) ────────────────

    @Override
    public Vec3 getBonePosition(String boneName) {
        // 1. Los huesos solo existen en el Cliente (Gráficos), el Servidor es ciego.
        if (this.level().isClientSide()) {
            try {
                // 2. Le pedimos a Minecraft el "Dibujante" (Renderer) de esta duende
                var dispatcher = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
                var renderer = dispatcher.getRenderer(this);

                // 3. Confirmamos que sea un dibujante oficial de GeckoLib
                if ((Object) renderer instanceof software.bernie.geckolib.renderer.GeoEntityRenderer<?> geoRenderer) {

                    // 4. ¡BINGO! Entramos al Modelo -> Procesador -> Hueso
                    software.bernie.geckolib.cache.object.GeoBone bone =
                            (software.bernie.geckolib.cache.object.GeoBone) geoRenderer.getGeoModel().getAnimationProcessor().getBone(boneName);

                    if (bone != null) {
                        // 5. GeckoLib guarda la posición en formato de píxeles (1/16 de bloque)
                        double bX = bone.getPosX() / 16.0;
                        double bY = bone.getPosY() / 16.0;
                        double bZ = bone.getPosZ() / 16.0;

                        // 6. Rotamos ese punto para que coincida con hacia dónde mira la duende
                        Vec3 offset = new Vec3(bX, bY, bZ).yRot((float) Math.toRadians(-this.getYRot()));

                        // Devolvemos la posición exacta en el mundo
                        return this.position().add(offset);
                    }
                }
            } catch (Exception e) {
                // Si el juego apenas está cargando y el renderer no existe, ignoramos el error
            }
        }

        // 7. Fallback seguro para el Servidor o si no encontró el hueso
        return this.position().add(0, this.getEyeHeight(), 0);
    }
// ── Utilidades de Dueño (Owner) ──────────────────────────────────────────

    public void setOwnerUUID(@Nullable UUID id) {
        this.entityData.set(OWNER_UUID, id == null ? "" : id.toString());
    }

    @Nullable
    public UUID getOwnerUUID() {
        String s = this.entityData.get(OWNER_UUID);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    // Y por si las moscas, un método rápido para sacar al jugador dueño:
    @Nullable
    public Player getOwnerPlayer() {
        UUID ownerId = getOwnerUUID();
        return ownerId == null ? null : this.level().getPlayerByUUID(ownerId);
    }
    public boolean isSexModeActive() {
        // Si tiene un UUID de pareja registrado, significa que está activa
        return this.getSexPartnerUUID() != null;
    }
    public static Vec3 getBodyDisplacement(GoblinEntity entity) {
        AnimState state = entity.getAnimState();

        // 1. Si la están lanzando (THROWN)
        if (state == AnimState.THROWN) {
            // Ejemplo: 45 grados de inclinación, sube un poco y se echa hacia atrás
            return new Vec3(45.0D, 1.5D, -2.0D);
        }

        // 2. Si apenas va a empezar el lanzamiento (START_THROWING)
        if (state == AnimState.START_THROWING) {
            return new Vec3(20.0D, 0.5D, 0.0D);
        }

        // 3. Estado por defecto: sin movimiento extra
        return Vec3.ZERO;
    }
// ── ACCIONES DEL MENÚ RADIAL ──

    public void commitActionB(java.util.UUID playerId) {
        // 🚨 Lógica para la acción inferior (ej. iniciar animación específica)
    }

    public void commitActionC(java.util.UUID playerId) {
        // 🚨 Lógica para la acción superior
    }
}