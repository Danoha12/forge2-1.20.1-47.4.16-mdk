package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SendCompanionHomePacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import com.trolmastercard.sexmod.network.packet.OpenNpcInventoryPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BiaEntity — Portado a 1.20.1 / GeckoLib 4.
 */
public class BiaEntity extends NpcInventoryBase implements GeoEntity {

    static final float WIDTH  = 0.49F;
    static final float HEIGHT = 1.65F;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BiaEntity(EntityType<? extends BiaEntity> type, Level level) {
        super(type, level);
        // Estas variables ahora funcionan porque las añadimos a la clase Base
        this.maxHealthStat  = 140;
        this.armourStat     = 50;
        this.attackStat     = 140;
    }

    @Override
    public String getNpcName() {
        return "Bia";
    }

    @Override
    public float getNametagOffsetY() {
        return -0.2F;
    }

    // ── ESTA ES LA PIEZA QUE FALTABA (Huesos) ────────────────────────────────
    @Override
    public Vec3 getBonePosition(String boneName) {
        // En 1.20.1/GeckoLib 4, esto sirve de ancla para efectos o partículas
        return this.position();
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState current = getAnimState();

        // Mapeo SFW: ANAL_CUM -> BACK_FINISH
        if (current == AnimState.ANAL_CUM) {
            if (next == AnimState.ANAL_FAST || next == AnimState.ANAL_SLOW) return;
            setSexPartnerUUID(null);
        }

        // Mapeo SFW: PRONE_DOGGY_CUM -> PRONE_DANCE_FINISH
        if (current == AnimState.PRONE_DOGGY_CUM) {
            if (next == AnimState.PRONE_DOGGY_HARD || next == AnimState.PRONE_DOGGY_SOFT) return;
            setSexPartnerUUID(null);
        }

        super.setAnimStateFiltered(next);
    }

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.followme" -> setMaster(playerId.toString());
            case "action.names.stopfollowme" -> stopFollow();
            case "action.names.gohome" -> {
                stopFollow();
                ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()));
            }
            case "action.names.setnewhome" -> {
                setHomePosition(this.position());
                ModNetwork.CHANNEL.sendToServer(new SetNpcHomePacket(getNpcUUID(), this.position()));
            }
            case "action.names.equipment" -> {
                ModNetwork.CHANNEL.sendToServer(new OpenNpcInventoryPacket(getNpcUUID()));
            }
        }
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "movement", 5, state -> {
                    if (getAnimState() == AnimState.NULL) {
                        String anim = state.isMoving() ? "animation.bia.walk" : "animation.bia.idle";
                        return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
                }),
                new AnimationController<>(this, "action", 0, state -> {
                    AnimState anim = getAnimState();
                    if (anim == null || anim == AnimState.NULL) return PlayState.STOP;

                    RawAnimation raw = switch (anim) {
                        case ANAL_SLOW         -> RawAnimation.begin().thenLoop("animation.bia.anal_slow");
                        case ANAL_FAST         -> RawAnimation.begin().thenLoop("animation.bia.anal_fast");
                        case ANAL_CUM          -> RawAnimation.begin().thenPlay("animation.bia.anal_cum");
                        case PRONE_DOGGY_SOFT  -> RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft");
                        case PRONE_DOGGY_HARD  -> RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard");
                        case PRONE_DOGGY_CUM   -> RawAnimation.begin().thenPlay("animation.bia.prone_doggy_cum");
                        default                -> RawAnimation.begin().thenPlay("animation.bia.null");
                    };
                    return state.setAndContinue(raw);
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}