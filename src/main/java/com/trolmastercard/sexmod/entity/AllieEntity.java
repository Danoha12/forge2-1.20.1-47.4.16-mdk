package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.InteractionMeterOverlay;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * AllieEntity — Portado a 1.20.1.
 * * Maneja deseos, efectos de desvanecimiento y partículas de portal.
 */
public class AllieEntity extends BaseNpcEntity implements GeoEntity {

    public static final EntityDataAccessor<ItemStack> LAMP_SLOT = SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.ITEM_STACK);

    private float disappearTimer = 1.0F;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public AllieEntity(EntityType<? extends AllieEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LAMP_SLOT, ItemStack.EMPTY);
    }

    // ── Lógica de "ADN" de Allie ─────────────────────────────────────────────

    public boolean isFirstSummon() {
        ItemStack lamp = this.entityData.get(LAMP_SLOT);
        CompoundTag tag = lamp.getOrCreateTag();
        return tag.getInt("modUses") <= 1;
    }

    // ── Ticks y Partículas ───────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        if (disappearTimer < 1.0F && disappearTimer > 0.0F) {
            disappearTimer -= 0.05F;
            if (disappearTimer <= 0.0F) {
                this.discard();
            }
        }

        if (this.level().isClientSide()) {
            spawnTailParticles();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnTailParticles() {
        if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5D),
                    this.getY() + 0.2D,
                    this.getZ() + (this.random.nextDouble() - 0.5D),
                    0, -0.2, 0);
        }
    }

    // ── Interacción y Decisiones ─────────────────────────────────────────────

    @Override
    public void triggerAction(String actionKey, UUID playerUUID) {
        // Usamos los nombres estandarizados de tu AnimState
        switch (actionKey) {
            case "wish_wealth" -> {
                setAnimStateFiltered(isFirstSummon() ? AnimState.RICH_FIRST_TIME : AnimState.RICH_NORMAL);
            }
            case "special_1" -> {
                setAnimStateFiltered(AnimState.REVERSE_COWGIRL_START);
            }
        }
    }

    // ── GeckoLib 4: Controladores ────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "tail", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail"))
        ));

        AnimationController<AllieEntity> actionCtrl = new AnimationController<>(this, "action", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL) return PlayState.STOP;

            // Mapeo sincronizado con los nombres de AnimState
            String name = switch (anim) {
                case SUMMON -> "animation.allie.summon";
                case REVERSE_COWGIRL_START -> "animation.allie.reverse_cowgirl_start";
                case REVERSE_COWGIRL_SLOW -> "animation.allie.reverse_cowgirl_slow";
                case RICH_FIRST_TIME -> "animation.allie.rich";
                case RICH_NORMAL -> "animation.allie.rich_normal";
                default -> "animation.allie.idle";
            };
            return state.setAndContinue(RawAnimation.begin().thenLoop(name));
        });

        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    @OnlyIn(Dist.CLIENT)
    // 🚨 CORREGIDO AQUÍ: Se cambió animation.event a keyframe.event
    private void handleSoundKeyframe(software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent<AllieEntity> event) {
        String key = event.getKeyframeData().getSound();
        Player localPlayer = Minecraft.getInstance().player;

        // Se cambió getSexPartnerUUID() a un valor dummy temporal si no existe en BaseNpcEntity
        // Si tu BaseNpcEntity tiene el método, descomenta la siguiente línea y borra el 'boolean isPartner = false;'
        // boolean isPartner = localPlayer != null && localPlayer.getUUID().equals(this.getSexPartnerUUID());
        boolean isPartner = false;

        switch (key) {
            case "wish_granted" -> {
                this.disappearTimer = 0.9F;
            }
            case "moan" -> {
                this.playSound(ModSounds.GIRLS_ALLIE_BJMOAN.get(), 1.0F, 1.0F);
                if (isPartner) InteractionMeterOverlay.addValue(0.03D);
            }
        }
    }

    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 1, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}