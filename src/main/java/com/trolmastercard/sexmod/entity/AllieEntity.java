package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.client.ClientStateManager;
import com.trolmastercard.sexmod.client.HornyMeterOverlay;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.DespawnClothingPacket;
import com.trolmastercard.sexmod.network.packet.MakeRichWishPacket;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Allie - Entidad de Compañía Mágica.
 * Portada a 1.20.1 con enmascaramiento de estados para estabilidad total.
 */
public class AllieEntity extends BaseNpcEntity {

    public static final int SUMMON_PARTICLE_COUNT = 300;
    public static final int TAIL_BONE_COUNT = 8;
    public static final Vec3 HITBOX = new Vec3(0.5D, 1.0D, 0.0D);

    public static final EntityDataAccessor<ItemStack> LAMP_SLOT =
            SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.ITEM_STACK);

    private float disappearTimer = 1.0F;
    public boolean pendingSummon = false;

    // Enmascaramos las variables internas para que coincidan con la nueva temática
    private int danceSlowVariant = 1;
    private int danceFastVariant = 1;
    private boolean sparkleToggle = false;

    private boolean animVariantLocked = false;
    private boolean isSandVariant = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public AllieEntity(EntityType<? extends AllieEntity> type, Level level) {
        super(type, level);
        setBoundingBox(getBoundingBoxForPose(getPose()));
    }

    public AllieEntity(EntityType<? extends AllieEntity> type, Level level, ItemStack lamp) {
        this(type, level);
        entityData.set(LAMP_SLOT, lamp);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LAMP_SLOT, ItemStack.EMPTY);
    }

    @Override
    public String getNpcName() { return "Allie"; }

    @Override
    public float getEyeHeightOffset() { return 1.0F; }

    public boolean isFirstSummon() {
        CompoundTag tag = entityData.get(LAMP_SLOT).getTag();
        if (tag == null) return true;
        // Mantenemos "sexmodUses" si tu NBT ya tiene esos datos guardados
        return tag.getInt("sexmodUses") == 1;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (disappearTimer != 1.0F && disappearTimer != -69.0F) {
            if (disappearTimer <= 0.0F) {
                if (isOwnerLocal()) {
                    ModNetwork.CHANNEL.sendToServer(new DespawnClothingPacket(isFirstSummon()));
                    ClientStateManager.setAllieActive(true);
                }
                disappearTimer = -69.0F;
            }
        }

        if (!level().isClientSide()) return;
        if (pendingSummon) triggerClientSummon();
        spawnTailParticles();
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnTailParticles() {
        if (tickCount % 10 != 0) return;
        int bone = getRandom().nextInt(TAIL_BONE_COUNT);
        Vec3 bonePos = getBoneWorldPos("tail" + bone).add(position());
        level().addParticle(ParticleTypes.PORTAL, bonePos.x, bonePos.y, bonePos.z, 0, 0.01, 0);
    }

    @OnlyIn(Dist.CLIENT)
    private void triggerClientSummon() {
        openActionMenu((Player) Minecraft.getInstance().player);
        pendingSummon = false;
    }

    public boolean openMenu(Player player) {
        isSandVariant = false;
        String[] actions = {
                "action.names.makemerichallie",
                "action.names.magicwish",
                "action.names.magicdance"
        };
        openActionMenu(player, this, actions, false);
        return true;
    }

    @Override
    public void onActionChosen(String actionKey, UUID playerUUID) {
        isSandVariant = true;
        if ("action.names.makemerichallie".equals(actionKey)) {
            setAnimState(isFirstSummon() ? AnimState.RICH_FIRST_TIME : AnimState.RICH_NORMAL);
            return;
        }

        // Mapeo a los nuevos estados del enum masivo
        setAnimationFollowUp(isFirstSummon() ? "magic_wish" : "magic_dance");
        setAnimState(isFirstSummon() ? AnimState.ALLIE_PREPARE_FIRST_TIME : AnimState.ALLIE_PREPARE_NORMAL);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "eyes", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"))
        ));

        registrar.add(new AnimationController<>(this, "movement", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail"))
        ));

        var actionCtrl = new AnimationController<>(this, "action", 0, this::handleActionState);
        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    private PlayState handleActionState(AnimationState<AllieEntity> state) {
        AnimState anim = getAnimState();
        // IMPORTANTE: Los strings de las animaciones deben coincidir con tu .json de Blockbench
        RawAnimation raw = switch (anim) {
            case SUMMON              -> RawAnimation.begin().thenPlay("animation.allie.summon");
            case SUMMON_NORMAL       -> RawAnimation.begin().thenPlay("animation.allie.summon_normal");
            case SUMMON_WAIT         -> RawAnimation.begin().thenLoop("animation.allie.summon_wait");
            case ALLIE_PREPARE_FIRST_TIME -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_prepare");
            case ALLIE_PREPARE_NORMAL -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_normal_prepare");
            case MAGIC_DEEP_START    -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_start");
            case MAGIC_DEEP_SLOW     -> RawAnimation.begin().thenLoop("animation.allie.deepthroat_slow");
            case MAGIC_DEEP_FAST     -> RawAnimation.begin().thenLoop("animation.allie.deepthroat_fast");
            case MAGIC_DEEP_FINISH   -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_cum");
            case RICH_FIRST_TIME     -> RawAnimation.begin().thenPlay("animation.allie.rich");
            case RICH_NORMAL         -> RawAnimation.begin().thenPlay("animation.allie.rich_normal");
            case REV_DANCE_START     -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_start");
            case REV_DANCE_SLOW      -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_slow" + danceSlowVariant);
            case REV_DANCE_FAST_CONTINUE -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fastc" + danceFastVariant);
            case REV_DANCE_FINISH    -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_cum");
            default                  -> RawAnimation.begin().thenLoop("animation.allie.null");
        };
        return state.setAndContinue(raw);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundKeyframe(AnimationController.KeyframeEvent<AllieEntity> event) {
        String sound = event.getKeyframeData().sound();
        // Los casos del switch deben coincidir EXACTAMENTE con los nombres en el JSON de animación
        switch (sound) {
            case "summonMSG1" -> { displayDialogue("allie.dialogue.summon1"); playRandSound(ModSounds.GIRLS_ALLIE_HUH); }
            case "summonMSG8" -> { if (isOwnerLocal()) openMenu(Minecraft.getInstance().player); }
            case "rich_MSG1"  -> {
                displayDialogue("allie.dialogue.wishgranted");
                if (isOwnerLocal()) ModNetwork.CHANNEL.sendToServer(new MakeRichWishPacket(position()));
            }
            case "summonDone" -> setAnimState(AnimState.SUMMON_WAIT);
            case "deepthroat_prepareDone" -> {
                if (!isOwnerLocal()) break;
                if ("magic_dance".equals(getAnimFollowUp())) {
                    setAnimState(AnimState.REV_DANCE_START);
                } else {
                    setAnimState(AnimState.MAGIC_DEEP_START);
                    HornyMeterOverlay.onInteractionStart();
                }
            }
            case "deepthroat_startDone" -> setAnimState(AnimState.MAGIC_DEEP_SLOW);
            case "deepthroat_fastMSG1", "deepthroat_slowMSG1", "fastMoan", "slowMoan" -> {
                playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE); // Risas en lugar de gemidos
                if (isOwnerLocal()) { HornyMeterOverlay.setVisible(true); HornyMeterOverlay.addValue(0.02D); }
            }
            case "deepthroat_cumDone", "cowgirl_cumDone" -> {
                if (isOwnerLocal()) { onSessionEnd(); ModNetwork.CHANNEL.sendToServer(new DespawnClothingPacket(isFirstSummon())); }
            }
            case "disappear" -> disappearTimer = 0.99F;
            case "giggle" -> playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}