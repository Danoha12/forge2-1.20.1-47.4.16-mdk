package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.TransitionScreen;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.OpenNpcInventoryPacket;
import com.trolmastercard.sexmod.network.packet.SendCompanionHomePacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class LunaEntity extends NpcInventoryBase implements GeoEntity {

    // -- DataAccessors porteados --
    public static final EntityDataAccessor<Float> ANIM_TIMER = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<ItemStack> ENCHANT_ITEM = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Boolean> IS_ON_BACK = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<ItemStack> PAYMENT_ITEM = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);

    private int sitDownTimer = 0;
    private boolean approachingBed = false;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public LunaEntity(EntityType<? extends LunaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_TIMER, 0.0F);
        this.entityData.define(ENCHANT_ITEM, ItemStack.EMPTY);
        this.entityData.define(IS_ON_BACK, false);
        this.entityData.define(PAYMENT_ITEM, ItemStack.EMPTY);
    }

    @Override
    public String getNpcName() { return "Luna"; }

    // -- Lógica de Encantamientos (Visual) --
    private void applyEnchantmentsToVisual(ItemStack visualItem) {
        ItemStack enchSrc = this.entityData.get(ENCHANT_ITEM);
        if (!enchSrc.isEmpty()) {
            var enchants = EnchantmentHelper.getEnchantments(enchSrc);
            EnchantmentHelper.setEnchantments(enchants, visualItem);
        }
    }

    // -- Interacción Porteada --
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(Items.SALMON)) {
            if (!this.level().isClientSide()) {
                held.shrink(1);
                this.playSound(ModSounds.GIRLS_LUNA_OWO[0].get(), 1.0F, 1.0F);
                findAndGoToBed();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // Si no tiene salmón, abrir menú de acciones (Lógica de tu Screen)
        return super.mobInteract(player, hand);
    }

    // -- Búsqueda de Camas Inteligente (Tu lógica de offsets) --
    private void findAndGoToBed() {
        BlockPos origin = this.blockPosition();
        BlockPos targetBed = null;

        // Buscamos cama en rango de 8 bloques
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 2, 8))) {
            if (this.level().getBlockState(pos).getBlock() instanceof BedBlock) {
                targetBed = pos.immutable();
                break;
            }
        }

        if (targetBed != null) {
            this.getNavigation().moveTo(targetBed.getX(), targetBed.getY(), targetBed.getZ(), 0.5D);
            this.approachingBed = true;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            tickWaitCatLogic();
        }
    }

    private void tickWaitCatLogic() {
        if (getAnimState() != AnimState.WAIT_CAT) {
            sitDownTimer = 0;
            return;
        }

        Player nearest = this.level().getNearestPlayer(this, 1.5D);
        if (nearest != null) {
            sitDownTimer++;
            if (sitDownTimer >= 25) {
                this.setSexPartnerUUID(nearest.getUUID());
                this.setAnimStateFiltered(AnimState.COWGIRL_SITTING_INTRO);
                sitDownTimer = 0;
            }
        }
    }

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.sex" -> setAnimStateFiltered(AnimState.WAIT_CAT);
            case "action.names.touchboobs" -> setAnimStateFiltered(AnimState.TOUCH_BOOBS_INTRO);
            case "action.names.headpat" -> setAnimStateFiltered(AnimState.HEAD_PAT);
            case "action.names.followme" -> setMaster(playerId.toString());
            case "action.names.stopfollowme" -> stopFollow();
            case "action.names.equipment" -> ModNetwork.CHANNEL.sendToServer(new OpenNpcInventoryPacket(getNpcUUID()));
        }
    }

    // -- GeckoLib 4 Controllers --
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;
            String anim = state.isMoving() ? "animation.cat.walk" : "animation.cat.idle";
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        }));

        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState as = getAnimState();
            if (as == AnimState.NULL || as == null) return PlayState.STOP;

            // Mapeo dinámico de animaciones con prefijo "animation.cat."
            String name = "animation.cat." + as.name().toLowerCase();
            return state.setAndContinue(RawAnimation.begin().thenLoop(name));
        }));
    }

    @Override public Vec3 getBonePosition(String boneName) { return this.position(); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
// ── SISTEMA DE PESCA (Conexión con el Anzuelo) ───────────────────────────

    // Variable para guardar el anzuelo que Luna tiene lanzado
    private com.trolmastercard.sexmod.entity.FishingHookEntity activeHook;

    /**
     * 1. Conecta o desconecta el anzuelo actual.
     */
    public void setFishingHook(com.trolmastercard.sexmod.entity.FishingHookEntity hook) {
        this.activeHook = hook;
    }

    /**
     * 2. El anzuelo llama a este método cuando toca el suelo o el agua.
     */
    public void onFishingHookLanded() {
        // 🚨 Aquí va tu lógica si quieres que Luna reaccione o cambie de animación
    }

    /**
     * 3. El anzuelo llama a este método cuando atrapa un botín (Loot).
     */
    public void receiveItem(net.minecraft.world.item.ItemStack item) {
        // 🚨 Aquí decides qué hace Luna con el ítem (ej. tirarlo al piso para el jugador)
        this.spawnAtLocation(item);
    }
}