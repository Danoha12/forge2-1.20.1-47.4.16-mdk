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

    // Slot sincronizado para la lámpara (vital para que Allie sepa sus usos)
    public static final EntityDataAccessor<ItemStack> LAMP_SLOT = SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.ITEM_STACK);

    private float disappearTimer = 1.0F; // Controla el efecto de "regreso a la lámpara"
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

    /** Chequea si es la primera vez que sale de la lámpara según el NBT del ítem */
    public boolean isFirstSummon() {
        ItemStack lamp = this.entityData.get(LAMP_SLOT);
        CompoundTag tag = lamp.getOrCreateTag();
        return tag.getInt("modUses") <= 1;
    }

    // ── Ticks y Partículas ───────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        // Lógica de desaparición: si el timer baja, Allie se vuelve humo y desaparece
        if (disappearTimer < 1.0F && disappearTimer > 0.0F) {
            disappearTimer -= 0.05F;
            if (disappearTimer <= 0.0F) {
                this.discard(); // Adiós, Allie
            }
        }

        if (this.level().isClientSide()) {
            spawnTailParticles();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnTailParticles() {
        // Genera partículas de portal en la base de la "cola" de humo de Allie
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
        // Mapeo de botones del menú UI a estados de animación
        switch (actionKey) {
            case "wish_wealth" -> {
                setAnimStateFiltered(isFirstSummon() ? AnimState.SPECIAL_WISH_START : AnimState.SPECIAL_WISH_NORMAL);
            }
            case "special_1" -> {
                setAnimStateFiltered(AnimState.INTERACTION_START_1);
            }
        }
    }

    // ── GeckoLib 4: Controladores ────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de la cola (siempre en movimiento, es humo mágico)
        registrar.add(new AnimationController<>(this, "tail", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail"))
        ));

        // Controlador de Cuerpo y Sexo
        AnimationController<AllieEntity> actionCtrl = new AnimationController<>(this, "action", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL) return PlayState.STOP;

            String name = switch (anim) {
                case SUMMON_START -> "animation.allie.summon";
                case INTERACTION_START_1 -> "animation.allie.reverse_cowgirl_start";
                case INTERACTION_LOOP_1 -> "animation.allie.reverse_cowgirl_slow";
                case SPECIAL_WISH_START -> "animation.allie.rich";
                default -> "animation.allie.idle";
            };
            return state.setAndContinue(RawAnimation.begin().thenLoop(name));
        });

        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundKeyframe(software.bernie.geckolib.core.animation.event.SoundKeyframeEvent<AllieEntity> event) {
        String key = event.getKeyframeData().getSound();
        Player localPlayer = Minecraft.getInstance().player;
        boolean isPartner = localPlayer != null && localPlayer.getUUID().equals(this.getSexPartnerUUID());

        switch (key) {
            case "wish_granted" -> {
                this.disappearTimer = 0.9F; // Empezar a desaparecer
                // Aquí se dispararía el paquete para dar esmeraldas/diamantes al jugador
            }
            case "moan" -> {
                this.playSound(ModSounds.ALLIE_VOICE_HAPPY.get(), 1.0F, 1.0F);
                if (isPartner) InteractionMeterOverlay.addValue(0.03D);
            }
        }
    }

    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 1, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}