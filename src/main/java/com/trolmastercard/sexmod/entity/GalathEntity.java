package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.YawPitch;
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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent; // 🚨 NUEVO IMPORT PARA 1.20.1
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

public class GalathEntity extends BaseNpcEntity implements GeoEntity {

    public static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_R = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_L = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> MANGLE_UUID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_RUNNING = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("Galath"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // Variables de renderizado (Usadas por GalathModel y MangleLieModel)
    public float headRotX = 0.0F;
    public float bodyRotY = 0.0F;
    public float bodyScaleY = 1.0F;

    // Constructor
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

    // ── Métodos para Modelos (GalathModel y MangleLieModel) ──────────────────

    public void setMangleLie(@Nullable MangleLieEntity mangle) {
        this.entityData.set(MANGLE_UUID, mangle == null ? "" : mangle.getUUID().toString());
    }

    // 🚨 Este es el método que tu GalathModel estaba pidiendo a gritos
    @Nullable
    public MangleLieEntity getMangleLie(boolean forceClient) {
        String s = this.entityData.get(MANGLE_UUID);
        if (s.isEmpty()) return null;

        // 🚨 AÑADIDO: El filtro "entity -> true" exigido por la 1.20.1
        for (MangleLieEntity e : this.level().getEntitiesOfClass(MangleLieEntity.class, this.getBoundingBox().inflate(10.0D), entity -> true)) {
            if (e.getUUID().toString().equals(s)) {
                return e;
            }
        }
        return null;
    }

    public boolean isNudeMode() {
        // Devuelve true si el índice de ropa indica desnudez
        return this.entityData.get(DATA_OUTFIT_INDEX) == 0;
    }

    public boolean hasWings() {
        return true; // Por defecto Galath tiene alas. Añade lógica NBT si pueden perderse.
    }

    public int getAttackAnimIdx() {
        return this.tickCount % 40; // Simulación de frames de ataque. Ajustar a tu lógica real.
    }

    public RgbColor getBodySwayAt(long gameTime) {
        float f = (float)Math.sin(gameTime * 0.1F) * 5.0F;
        return new RgbColor(f, 0, 0); // Balanceo simulado en X (pitch)
    }

    public RgbColor getHeadSwayAt(float tick) {
        float f = (float)Math.sin(tick * 0.2F) * 2.0F;
        return new RgbColor(f, f * 0.5F, 0);
    }

    public YawPitch getYawPitch() {
        return new YawPitch(this.getYRot(), this.getXRot(), this.yRotO, this.xRotO);
    }

    // ── Lógica de Escape ─────────────────────────────────────────────────────

    public void backOff() {
        if (this.level().isClientSide()) return;
        this.setAnimState(AnimState.NULL);
        this.setTarget(null);
        this.entityData.set(TARGET_ID, -1);
        this.ejectPassengers();

        // 🚨 AÑADIDO: El filtro "e -> true" al final de los parámetros
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0), e -> true)) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new CameraControlPacket(true)
            );
        }
    }

    // ── GeckoLib 4 ───────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
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
    }

    // ── Eventos Servidor ─────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        // 🚨 CORREGIDO: Nuevo evento de Spawn en 1.20.1
        @SubscribeEvent
        public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
            Entity e = event.getEntity();
            if (!(e instanceof WitherSkeleton) && !(e instanceof Blaze)) return;

            Level world = e.level();
            BlockPos pos = e.blockPosition();

            // 🚨 CAMBIADO: En la 1.20.1 se usa "world.random" directo, sin el get()
            if (!world.isClientSide() && world.random.nextFloat() < 0.05F) {
                event.setCanceled(true); // Cancela el original
                GalathEntity galath = new GalathEntity((EntityType<? extends GalathEntity>) e.getType(), world);
                galath.moveTo(pos.getX(), pos.getY(), pos.getZ());
                world.addFreshEntity(galath);
            }
        }
        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof GalathEntity g)) return;
            if (g.getHealth() <= 1.0F && !g.isRemoved()) {
                event.setCanceled(true);
                // 🚨 OJO: Revisa si KNOCK_OUT_GROUND es correcto en tu AnimState.java
                // g.setAnimState(AnimState.KNOCK_OUT_GROUND);
            }
        }
    }

    // ── Eventos Cliente ──────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {

        // 🚨 CORREGIDO: Nuevo nombre del evento de teclado
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keyJump.isDown() && mc.player != null && mc.player.getVehicle() instanceof GalathEntity g) {
                // g.setAnimState(AnimState.BOOST); // Descomentar si BOOST está en AnimState
            }
        }
    }

    @Override public void triggerAction(String action, UUID playerId) {}
    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 2, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}