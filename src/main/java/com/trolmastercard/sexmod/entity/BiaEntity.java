package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
// import com.trolmastercard.sexmod.registry.ModLootTables; // Descomentar cuando existan
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SendCompanionHomePacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import com.trolmastercard.sexmod.network.packet.OpenNpcInventoryPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BiaEntity — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Personaje NPC "Bia". Soporta dos secuencias de interacción:
 * - BACK (Mapeado de Anal)
 * - PRONE_DANCE (Mapeado de Prone Doggy)
 */
public class BiaEntity extends NpcInventoryEntity implements GeoEntity {

    // ── Constantes de tamaño ───────────────────────────────────────────────────
    static final float WIDTH  = 0.49F;
    static final float HEIGHT = 1.65F;

    /** Offset de aproximación relativo al compañero. */
    public static final Vec3 APPROACH_OFFSET = new Vec3(0.0, -0.03, -0.2);

    // ── GeckoLib 4 ────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── Constructor ────────────────────────────────────────────────────────────

    public BiaEntity(EntityType<? extends BiaEntity> type, Level level) {
        super(type, level);
        this.maxHealthStat  = 140;
        this.armourStat     = 50;
        this.attackStat     = 140;
        // this.approachOffset = APPROACH_OFFSET; // Descomentar si existe en la clase base
    }

    // ── NpcInventoryEntity Contract ────────────────────────────────────────────

    @Override
    public String getDefaultName() {
        return "Bia";
    }

    // @Override // Descomentar si existe en BaseNpcEntity
    public float getYOffset() {
        return -0.2F;
    }

    /** Saludo al aparecer en el mundo. */
    // @Override // Descomentar si el hook existe en BaseNpcEntity
    public void onSpawnAnnounce() {
        // sendProximityMessage("I am living here now nya~"); // Descomentar si existe el método
        playNpcSound(ModSounds.GIRLS_BIA_BREATH, 1.0F);
    }

    public void enterInteractiveMode() {
        this.isInteractiveMode = true;
    }

    // ── Guardias de transición (Evitan bucles infinitos al terminar) ──────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState current = getAnimState();

        // Mapeo SFW: ANAL_CUM -> BACK_FINISH
        if (current == AnimState.BACK_FINISH) {
            if (next == AnimState.BACK_FAST || next == AnimState.BACK_SLOW) return;
            setPartnerUUID((UUID) null); // Limpiar partner al finalizar
        }

        // Mapeo SFW: PRONE_DOGGY_CUM -> PRONE_DANCE_FINISH
        if (current == AnimState.PRONE_DANCE_FINISH) {
            if (next == AnimState.PRONE_DANCE_HARD || next == AnimState.PRONE_DANCE_SOFT) return;
            setPartnerUUID((UUID) null);
        }

        super.setAnimStateFiltered(next);
    }

    // ── triggerAction (Callbacks de la UI del cliente) ────────────────────────

    // @Override // Descomentar si existe en BaseNpcEntity
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.followme" -> setMaster(playerId.toString());
            case "action.names.stopfollowme" -> cancelCurrentAction();
            case "action.names.gohome" -> {
                cancelCurrentAction();
                ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()));
            }
            case "action.names.setnewhome" -> {
                setHomePos(new Vec3(this.getX(), this.getY(), this.getZ()));
                ModNetwork.CHANNEL.sendToServer(new SetNpcHomePacket(getNpcUUID(), getHomePos()));
            }
            case "action.names.equipment" -> {
                ModNetwork.CHANNEL.sendToServer(new OpenNpcInventoryPacket(getNpcUUID(), playerId));
            }
        }
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "movement", 5, state -> {
                    if (getAnimState() == AnimState.NULL) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.idle"));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
                }),
                new AnimationController<>(this, "action", 0, state -> {
                    AnimState anim = getAnimState();
                    if (anim == null) return PlayState.CONTINUE;

                    return switch (anim) {
                        // Mapeo SFW -> Nombres de animaciones originales de Blockbench
                        case BACK_SLOW         -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_slow"));
                        case BACK_FAST         -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_fast"));
                        case BACK_FINISH       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_cum"));
                        case PRONE_DANCE_SOFT  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft"));
                        case PRONE_DANCE_HARD  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard"));
                        case PRONE_DANCE_FINISH-> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.prone_doggy_cum"));
                        default                -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.null"));
                    };
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}