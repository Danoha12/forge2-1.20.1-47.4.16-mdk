package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.RgbaColor;
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
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.monster.WitherSkeleton;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import java.util.UUID;

public class GalathEntity extends BaseNpcEntity implements GeoEntity {

    public static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_R = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENERGY_BALL_L = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> MANGLE_UUID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_RUNNING = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public final List<WitherSkeleton> skeletons = new ArrayList<>();
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("Galath"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 bodyOffset = Vec3.ZERO;
    private float approachPhase = -1f;
    public boolean isSummonedAway() { return false; } // Implementar tu lógica
    public void setBodyOffset(Vec3 offset) { this.bodyOffset = offset; }
    public Vec3 getBodyOffset() { return this.bodyOffset; }
    public float getApproachPhase() { return this.approachPhase; }
    public net.minecraft.world.entity.LivingEntity getVehicleTarget() { return this.getTarget(); }
    public boolean isKnockOutFlyActive() { return false; }
    public boolean hasSword() { return !this.getMainHandItem().isEmpty(); }
    public boolean hasPlayerBound() { return false; }


    public float headRotX = 0.0F;
    public float bodyRotY = 0.0F;
    public float bodyScaleY = 1.0F;

    public GalathEntity(EntityType<? extends GalathEntity> type, Level level) {
        super(type, level);
        // PUNTO 4: Hacer la barra visible al instanciar
        this.bossBar.setVisible(true);
    }

    // PUNTO 4: Gestión de visibilidad de la Boss Bar para jugadores
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // PUNTO 4: Actualizar progreso de la barra en el servidor
        if (!this.level().isClientSide) {
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        }

        tickSeekPlayer();
        tickParticles();
    }

    // Lógica interna de partículas y búsqueda (Placeholders para que no de error)
    private void tickSeekPlayer() {}
    private void tickParticles() {}

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

    public void setMangleLie(@Nullable MangleLieEntity mangle) {
        this.entityData.set(MANGLE_UUID, mangle == null ? "" : mangle.getUUID().toString());
    }

    @Nullable
    public MangleLieEntity getMangleLie(boolean forceClient) {
        String s = this.entityData.get(MANGLE_UUID);
        if (s.isEmpty()) return null;
        for (MangleLieEntity e : this.level().getEntitiesOfClass(MangleLieEntity.class, this.getBoundingBox().inflate(10.0D), entity -> true)) {
            if (e.getUUID().toString().equals(s)) {
                return e;
            }
        }
        return null;
    }

    public boolean isNudeMode() {
        return this.entityData.get(DATA_OUTFIT_INDEX) == 0;
    }

    public boolean hasWings() {
        return true;
    }

    public int getAttackAnimIdx() {
        return this.tickCount % 40;
    }

    public RgbaColor getBodySwayAt(long gameTime) {
        float f = (float)Math.sin(gameTime * 0.1F) * 5.0F;
        return new RgbaColor((int)f, 0, 0);
    }

    public RgbaColor getHeadSwayAt(float tick) {
        float f = (float)Math.sin(tick * 0.2F) * 2.0F;
        return new RgbaColor((int)f, (int)(f * 0.5F), 0);
    }

    public YawPitch getYawPitch() {
        return new YawPitch(this.getYRot(), this.getXRot(), this.yRotO, this.xRotO);
    }

    public void backOff() {
        if (this.level().isClientSide()) return;
        this.setAnimState(AnimState.NULL);
        this.setTarget(null);
        this.entityData.set(TARGET_ID, -1);
        this.ejectPassengers();

        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0), e -> true)) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new CameraControlPacket(true)
            );
        }
    }

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

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        // PUNTO 2: Corregido el Spawn de Galath
        @SubscribeEvent
        public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
            Entity e = event.getEntity();
            if (!(e instanceof WitherSkeleton) && !(e instanceof Blaze)) return;

            Level world = e.level();
            BlockPos pos = e.blockPosition();

            if (!world.isClientSide() && world.random.nextFloat() < 0.05F) {
                event.setCanceled(true);

                // NOTA: Es vital usar el EntityType registrado para Galath
                // Si tienes un registro, usa: ModEntities.GALATH.get().create(world);
                GalathEntity galath = new GalathEntity((EntityType<? extends GalathEntity>) e.getType(), world);

                if (galath != null) {
                    // Copiar rotación del mob original para que no aparezca mirando a Cuenca
                    galath.moveTo(pos.getX(), pos.getY(), pos.getZ(), e.getYRot(), e.getXRot());
                    world.addFreshEntity(galath);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof GalathEntity g)) return;
            if (g.getHealth() <= 1.0F && !g.isRemoved()) {
                event.setCanceled(true);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keyJump.isDown() && mc.player != null && mc.player.getVehicle() instanceof GalathEntity g) {
                // Lógica de Boost
            }
        }
    }

    @Override public void triggerAction(String action, UUID playerId) {}
    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 2, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    public boolean isSexModeActive() {
        return getSexPartnerUUID() != null || getAnimState() != com.trolmastercard.sexmod.registry.AnimState.NULL;
    }

    public static void syncPreRenderAngles(GalathEntity entity, float partialTick) {
        // 1. Sincronizar la rotación del cuerpo (Yaw)
        entity.yBodyRot = net.minecraft.util.Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

        // 2. Sincronizar la rotación de la cabeza
        entity.yHeadRot = net.minecraft.util.Mth.lerp(partialTick, entity.yHeadRotO, entity.yHeadRot);

        // 3. Sincronizar el cabeceo (Pitch)
        float lerpedPitch = net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        entity.setXRot(lerpedPitch);
    }
}