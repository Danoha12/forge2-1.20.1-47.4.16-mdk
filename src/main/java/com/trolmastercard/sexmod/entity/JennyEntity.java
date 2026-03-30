package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.InteractionMeterOverlay;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * JennyEntity — Portado a 1.20.1.
 * * Implementa la lógica específica de Jenny: búsqueda de camas,
 * * voces y controladores de animación de GeckoLib 4.
 */
public class JennyEntity extends BaseNpcEntity {

    public static final EntityDataAccessor<Boolean> IS_RIDING_SPECIAL = SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean movingToBed = false;
    private int bedTicks = 0;
    private int attackVariant = 0;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public JennyEntity(EntityType<? extends JennyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_RIDING_SPECIAL, false);
    }

    // ── Implementación de Métodos de BaseNpcEntity ───────────────────────────

    @Override
    public void triggerAction(String actionKey, UUID playerUUID) {
        // Aquí es donde la varita de editor o el menú de interacción mandan la orden
        switch (actionKey) {
            case "blowjob" -> setAnimStateFiltered(AnimState.STARTBLOWJOB);
            case "doggy" -> findAndGotoBed();
            case "strip" -> setAnimStateFiltered(AnimState.STRIP);
        }
    }

    @Override
    public Vec3 getBonePosition(String boneName) {
        // En una implementación real, esto se sincroniza con el modelo.
        // Por ahora, devolvemos un offset relativo a la cabeza.
        return this.position().add(0, this.getEyeHeight(), 0);
    }

    // ── Lógica de IA (Camas y Movimiento) ────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (movingToBed) tickBedApproach();
    }

    private void findAndGotoBed() {
        BlockPos bedPos = findNearestBed(this.blockPosition());
        if (bedPos == null) {
            this.sendSystemMessage(Component.translatable("jenny.dialogue.no_bed"));
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(bedPos).add(0, 0, 1.0);
        this.getNavigation().moveTo(target.x, target.y, target.z, 0.5D);
        this.movingToBed = true;
    }

    private void tickBedApproach() {
        if (this.getNavigation().isDone() || this.bedTicks++ > 200) {
            this.movingToBed = false;
            this.bedTicks = 0;
            this.setAnimStateFiltered(AnimState.INTERACTION_START_C); // Iniciar Doggy
        }
    }

    // ── Interacción Manual ───────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            // Aquí llamarías a tu pantalla de menú: NpcActionScreen.open(this);
            player.displayClientMessage(Component.literal("Menú de Jenny (Próximamente)"), true);
        }
        return InteractionResult.SUCCESS;
    }

    // ── GeckoLib 4: Controladores de Animación ───────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de cuerpo (Caminar, Estar quieta, Sentarse)
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;

            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.idle"));
        }));

        // Controlador de Escenas (Sex/Actions)
        AnimationController<JennyEntity> actionCtrl = new AnimationController<>(this, "action", 0, this::handleActionState);

        // Manejador de Sonidos y Eventos de la barra de placer
        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);

        registrar.add(actionCtrl);
    }

    private PlayState handleActionState(AnimationState<JennyEntity> state) {
        AnimState anim = getAnimState();
        if (anim == AnimState.NULL) return PlayState.STOP;

        String name = switch (anim) {
            case STRIP -> "animation.jenny.strip";
            case INTERACTION_START_A -> "animation.jenny.blowjobintro";
            case INTERACTION_LOOP_A -> "animation.jenny.blowjobsuck";
            case INTERACTION_FINISH_A -> "animation.jenny.blowjobcum";
            default -> "animation.jenny.idle";
        };

        return state.setAndContinue(RawAnimation.begin().thenLoop(name));
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundKeyframe(software.bernie.geckolib.core.animation.event.SoundKeyframeEvent<JennyEntity> event) {
        String key = event.getKeyframeData().getSound();
        Player localPlayer = Minecraft.getInstance().player;
        boolean isPartner = localPlayer != null && localPlayer.getUUID().equals(this.getSexPartnerUUID());

        switch (key) {
            case "suck" -> {
                this.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                if (isPartner) InteractionMeterOverlay.addValue(0.05D);
            }
            case "finish" -> {
                this.playSound(ModSounds.JENNY_VOICE_FINISH.get(), 1.0F, 1.0F);
                if (isPartner) InteractionMeterOverlay.onSexEnd();
            }
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private BlockPos findNearestBed(BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -2, -5), origin.offset(5, 2, 5))) {
            if (this.level().getBlockState(pos).getBlock() instanceof BedBlock) return pos.immutable();
        }
        return null;
    }

    @Override public float getModelScale() { return 1.0F; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}