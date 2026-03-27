package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.registry.ModLootTables; // Asegúrate de tener este import/clase
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.SendCompanionHomePacket; // Asegúrate de tener este paquete
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket; // Asegúrate de tener este paquete
import com.trolmastercard.sexmod.network.packet.OpenNpcInventoryPacket; // Asegúrate de tener este paquete
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BiaEntity - Compañera de la Tribu.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Extiende NpcInventoryEntity y permite gestión de inventario, seguimiento
 * y minijuegos de interacción.
 */
public class BiaEntity extends NpcInventoryEntity { // Asume que NpcInventoryEntity maneja inventario y DataParameters

    // =========================================================================
    //  Constantes y Dimensiones
    // =========================================================================

    static final float WIDTH  = 0.49F;
    static final float HEIGHT = 1.65F;

    /** Compensación de posicionamiento (Y/Z) relativa al compañero. */
    public static final Vec3 APPROACH_OFFSET = new Vec3(0.0, -0.03, -0.2);

    // =========================================================================
    //  Campos de Instancia (GeckoLib)
    // =========================================================================

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // =========================================================================
    //  Constructor
    // =========================================================================

    public BiaEntity(EntityType<? extends BiaEntity> type, Level level) {
        super(type, level);
        // Atributos base definidos en la clase original
        this.maxHealthStat  = 140;
        this.armourStat     = 50;
        this.attackStat     = 140;
        this.approachOffset = APPROACH_OFFSET;
    }

    // =========================================================================
    //  Implementación Base (NpcInventoryEntity)
    // =========================================================================

    @Override
    public String getDefaultName() { // O getNpcName(), dependiendo de tu clase base
        return "Bia";
    }

    @Override
    public float getYOffset() { // O getEyeHeightOffset(), dependiendo de tu clase base
        return -0.2F;
    }

    /** Se llama cuando Bia spawnea por primera vez en el mundo. */
    @Override
    public void onSpawnAnnounce() {
        sendProximityMessage("I am living here now nya~");
        playNpcSound(ModSounds.GIRLS_BIA_BREATH);
    }

    @Override
    public void enterInteractionMode() { // Antes enterSexMode
        this.interactionModeActive = true; // Antes sexModeActive
    }

    // =========================================================================
    //  Filtro de Transiciones de Animación
    // =========================================================================

    /**
     * Evita que los estados de finalización de evento regresen accidentalmente
     * a estados previos, creando bucles infinitos.
     */
    @Override
    public void setNextAnimState(AnimState next) { // O setAnimState, dependiendo de tu clase base
        AnimState current = getAnimState();

        // Minijuego 1
        if (current == AnimState.MINIGAME_1_FINISH) { // Antes ANAL_CUM
            if (next == AnimState.MINIGAME_1_FAST || next == AnimState.MINIGAME_1_SLOW) return;
            // Termina interacción: limpiar datos del compañero
            getEntityData().set(OWNER_UUID_STRING, "");
        }

        // Minijuego 2
        if (current == AnimState.MINIGAME_2_FINISH) { // Antes PRONE_DOGGY_CUM
            if (next == AnimState.MINIGAME_2_FAST || next == AnimState.MINIGAME_2_SLOW) return;
            getEntityData().set(OWNER_UUID_STRING, "");
        }

        super.setNextAnimState(next);
    }

    // =========================================================================
    //  Tabla de Botín
    // =========================================================================

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return ModLootTables.BIA;
    }

    // =========================================================================
    //  Acciones de Menú (Cliente -> Servidor)
    // =========================================================================

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.followme" -> setMaster("master", playerId.toString()); // Asume que setMaster existe
            case "action.names.stopfollowme" -> resetInteractionState(); // Antes resetSexState
            case "action.names.gohome" -> {
                resetInteractionState();
                ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getUUID())); // getNpcUUID() -> getUUID()
            }
            case "action.names.setnewhome" -> {
                onHomeSet(); // Asume que onHomeSet existe en la clase base
                ModNetwork.CHANNEL.sendToServer(new SetNpcHomePacket(getUUID(),
                        new Vec3(blockPosition().getX(), blockPosition().getY(), blockPosition().getZ())));
            }
            case "action.names.equipment" -> {
                Player player = level().getPlayerByUUID(playerId); // level() en lugar de level
                if (player != null)
                    ModNetwork.CHANNEL.sendToServer(
                            new OpenNpcInventoryPacket(getUUID(), playerId));
            }
        }
    }

    // =========================================================================
    //  Controladores GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var movement = new AnimationController<>(this, "movement", 5, this::movementController);
        var action   = new AnimationController<>(this, "action",   0, this::actionController);
        registrar.add(movement, action);
    }

    private PlayState movementController(AnimationState<BiaEntity> state) {
        AnimState anim = getAnimState();
        if (anim == AnimState.NULL) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.idle"));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.null"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<BiaEntity> state) {
        AnimState anim = getAnimState();
        if (anim == null) return PlayState.CONTINUE;

        // Los strings de los JSON de animación se mantienen idénticos al original
        return switch (anim) {
            case MINIGAME_1_SLOW  -> { state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.anal_slow")); yield PlayState.CONTINUE; }
            case MINIGAME_1_FAST  -> { state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.anal_fast")); yield PlayState.CONTINUE; }
            case MINIGAME_1_FINISH-> { state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.bia.anal_cum")); yield PlayState.CONTINUE; }
            case MINIGAME_2_SLOW  -> { state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft")); yield PlayState.CONTINUE; }
            case MINIGAME_2_FAST  -> { state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard")); yield PlayState.CONTINUE; }
            case MINIGAME_2_FINISH-> { state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.bia.prone_doggy_cum")); yield PlayState.CONTINUE; }
            default               -> { state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.bia.null")); yield PlayState.CONTINUE; }
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}