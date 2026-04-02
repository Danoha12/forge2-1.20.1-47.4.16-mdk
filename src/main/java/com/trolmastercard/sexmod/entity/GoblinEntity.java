package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.client.gui.TransitionScreen;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

public class GoblinEntity extends NpcModelCodeEntity implements GeoEntity {

    // -- Parámetros Sincronizados ---------------------------------------------
    public static final EntityDataAccessor<String> CARRIER_UUID = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_TAMED = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_PREGNANT = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    public boolean isQueen = false;

    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CARRIER_UUID, "");
        this.entityData.define(IS_TAMED, false);
        this.entityData.define(IS_PREGNANT, false);
    }

    @Override
    public Vec3 getBonePosition(String boneName) {
        return null;
    }

    @Override
    public void triggerAction(String action, UUID playerId) {

    }

    // -- Lógica de Interacción ------------------------------------------------
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;
        if (isQueen) return InteractionResult.CONSUME;

        // Lógica de captura (BJ)
        if (getAnimState() == AnimState.RUN) {
            if (this.distanceTo(player) < 3.5F) {
                setPartnerUUID(player.getUUID());
                setAnimStateFiltered(AnimState.CATCH);
                return InteractionResult.SUCCESS;
            }
        }

        // Lógica de carga
        if (getCarrierUUID() == null) {
            setCarrierUUID(player.getUUID());
            setAnimStateFiltered(AnimState.PICK_UP);
            this.entityData.set(IS_TAMED, true);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Nullable
    public UUID getCarrierUUID() {
        String s = this.entityData.get(CARRIER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setCarrierUUID(@Nullable UUID id) {
        this.entityData.set(CARRIER_UUID, id == null ? "" : id.toString());
    }

    // -- GeckoLib 4: Controladores --------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenLoop(state.isMoving() ? "animation.goblin.walk" : "animation.goblin.idle"));
        }));

        AnimationController<GoblinEntity> actionCtrl = new AnimationController<>(this, "action", 0, this::actionController);

        // CORRECCIÓN DE SONIDO PARA GECKOLIB 4
        actionCtrl.setSoundKeyframeHandler(event -> {
            if (event.getKeyframeData().getSound().equals("cumSound")) {
                this.playSound(ModSounds.GIRLS_GOBLIN_CUM.get(), 1.0F, 1.0F);
            }
        });

        registrar.add(actionCtrl);
    }

    private PlayState actionController(AnimationState<GoblinEntity> state) {
        AnimState anim = getAnimState();
        if (anim == null || anim == AnimState.NULL) return PlayState.STOP;

        // PROTECCIÓN DE CLIENTE PARA EVITAR CRASH EN SERVIDOR
        boolean firstPerson = false;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            firstPerson = isClientFirstPerson();
        }

        String suffix = firstPerson ? "firstperson" : "thirdperson";

        RawAnimation raw = switch (anim) {
            case PICK_UP -> RawAnimation.begin().thenPlay("animation.goblin.pick_up_" + suffix);
            case PAIZURI_START -> RawAnimation.begin().thenPlay("animation.goblin.paizuri_start");
            case PAIZURI_FAST -> RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast");
            case NELSON_INTRO -> RawAnimation.begin().thenPlay("animation.goblin.nelson_intro");
            case THROWN -> RawAnimation.begin().thenPlay("animation.goblin.thrown");
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

// ── Requisito de NpcModelCodeEntity ──────────────────────────────────────

    @Override
    public void onModelCodeChanged() {
        // Esta lógica se activa cuando el "ADN" del NPC cambia (color, pelo, etc.)
        if (this.level().isClientSide()) {
            // Aquí es donde normalmente se le avisa al Renderer que refresque la textura.
            // Por ahora lo dejamos vacío para que el compilador te deje pasar.
            // Ejemplo futuro: GoblinRenderer.clearCache(this);
        }
    }

    @Override
    protected String buildInitialCode(StringBuilder sb) {
        return "";
    }
}