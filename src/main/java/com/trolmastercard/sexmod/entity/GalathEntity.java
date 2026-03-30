package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.GalathFlightController;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.network.packet.OwnershipSyncPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.NpcType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.network.syncher.EntityDataAccessor;
import net.minecraft.world.network.syncher.EntityDataSerializers;
import net.minecraft.world.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * GalathEntity — Portado a 1.20.1.
 * * Entidad tipo Boss con mecánicas de vuelo, combate avanzado y escenas de corrupción.
 */
public class GalathEntity extends BaseNpcEntity implements GeoEntity {

    // ── Data Sync 1.20.1 ─────────────────────────────────────────────────────
    public static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_R = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_L = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> MANGLE_UUID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_RUNNING = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("Galath"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GalathEntity(EntityType<? extends GalathEntity> type, Level level) {
        super(type, level);
        this.bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseNpcEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 110.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 50.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(ENERGY_BALL_R, true);
        this.entityData.define(ENERGY_BALL_L, true);
        this.entityData.define(MANGLE_UUID, "");
        this.entityData.define(IS_RUNNING, false);
    }

    // ── Lógica de MangleLie (El Asiento) ─────────────────────────────────────

    public void setMangleLie(@Nullable MangleLieEntity mangle) {
        this.entityData.set(MANGLE_UUID, mangle == null ? "" : mangle.getUUID().toString());
    }

    @Nullable
    public MangleLieEntity getMangleLie() {
        String s = this.entityData.get(MANGLE_UUID);
        if (s.isEmpty()) return null;
        Entity e = this.level().isClientSide() ? null : ((net.minecraft.server.level.ServerLevel)this.level()).getEntity(UUID.fromString(s));
        return (e instanceof MangleLieEntity m) ? m : null;
    }

    // ── Lógica de Escape (Minijuego) ─────────────────────────────────────────

    /**
     * Llamado por GalathBackOffPacket cuando el jugador gana el minijuego de escape (WASD).
     * Libera al jugador, reinicia las animaciones y le da un pequeño respiro.
     */
    public void backOff() {
        if (this.level().isClientSide()) return;

        // 1. Detener la animación de asalto (volver al idle)
        this.setAnimState(AnimState.NULL);

        // 2. Limpiar el objetivo de combate
        this.setTarget(null);
        this.entityData.set(TARGET_ID, -1);

        // 3. Expulsar al jugador si estaba atrapado como pasajero
        this.ejectPassengers();

        // 4. Devolverle el control de la cámara a los jugadores cercanos
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0))) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new CameraControlPacket(true) // true = restaurar cámara normal
            );
        }
    }

    // ── GeckoLib 4: Controladores de Animación ───────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de Movimiento (Vuelo vs Tierra)
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;

            if (!this.onGround()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));
            }

            if (state.isMoving()) {
                String anim = this.entityData.get(IS_RUNNING) ? "animation.galath.run" : "animation.galath.walk";
                return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.idle"));
        }));

        // Controlador de Acciones (Combate y Escenas)
        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL) return PlayState.STOP;

            String name = switch (anim) {
                case GALATH_SUMMON -> "animation.galath.summon";
                case ATTACK_SWORD -> "animation.galath.attack";
                case RAPE_INTRO -> "animation.galath.rape_charge";
                case RAPE_CUM_IDLE -> "animation.galath.rape_cum_idle";
                case BOOST -> "animation.galath.boost";
                case MASTERBATE -> "animation.galath.masterbate";
                default -> "animation.galath.idle";
            };
            return state.setAndContinue(RawAnimation.begin().thenLoop(name));
        }));
    }

    // ── Eventos de Red y Servidor ────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        /** Bloquea el spawn de mobs hostiles cerca de Galath y spawnea a Galath en su lugar */
        @SubscribeEvent
        public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
            if (event.isSpawner()) return;
            Entity e = event.getEntity();
            if (!(e instanceof WitherSkeleton) && !(e instanceof Blaze)) return;

            Level world = e.level();
            BlockPos pos = e.blockPosition();

            // Lógica de probabilidad de spawn de Galath (Frecuencia baja)
            if (world.getRandom().nextFloat() < 0.05F) {
                event.setResult(Event.Result.DENY); // Cancelar spawn original
                GalathEntity galath = new GalathEntity((EntityType<? extends GalathEntity>) e.getType(), world);
                galath.moveTo(pos.getX(), pos.getY(), pos.getZ());
                world.addFreshEntity(galath);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof GalathEntity g)) return;
            // Si Galath muere, en lugar de desaparecer, se "neutraliza" (se vuelve tamed)
            if (g.getHealth() <= 1.0F && !g.isRemoved()) {
                event.setCanceled(true);
                g.setAnimState(AnimState.KNOCK_OUT_GROUND);
                // Lógica de tameo aquí...
            }
        }
    }

    // ── Eventos de Cliente ───────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            // Lógica del Boost al presionar Espacio mientras vuelas con Galath
            if (mc.options.keyJump.isDown() && mc.player != null && mc.player.getVehicle() instanceof GalathEntity g) {
                g.setAnimStateFiltered(AnimState.BOOST);
            }
        }
    }

    @Override public void triggerAction(String action, UUID playerId) {}
    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 2, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}