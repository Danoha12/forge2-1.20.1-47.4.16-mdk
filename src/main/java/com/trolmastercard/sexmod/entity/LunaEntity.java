package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.*;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.registry.ModItems; // Asegúrate de tener portado ModItems
import com.trolmastercard.sexmod.client.ClientStateManager;
import com.trolmastercard.sexmod.client.screen.TransitionScreen; // Enmascarado a nivel técnico

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * LunaEntity - Compañera Felina de la Tribu.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Maneja interacciones basadas en recompensas (Salmón).
 * * Posee mecánicas de navegación hacia camas y personalización de equipo.
 */
public class LunaEntity extends NpcInventoryEntity {

    // =========================================================================
    //  DataParameters Sincronizados
    // =========================================================================

    public static final EntityDataAccessor<Float>     ANIM_TIMER =
            SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<ItemStack> ENCHANT_ITEM =
            SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);

    public static final EntityDataAccessor<Boolean>    IS_ON_BACK =
            SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<ItemStack>  PAYMENT_ITEM =
            SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);

    // =========================================================================
    //  Campos de Instancia
    // =========================================================================

    public ItemStack heldVisualItem = new ItemStack(ModItems.GOBLIN_ITEM.get());

    private boolean interactionActive = false; // Antes onBack
    private int sitDownTimer = 0;
    private boolean approachingBed = false;
    private int bedTimer = 0;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // =========================================================================
    //  Constructor e Identidad
    // =========================================================================

    public LunaEntity(EntityType<? extends LunaEntity> type, Level level) {
        super(type, level);
        // Inicialización de inventario por defecto
        if (getInventory().getStackInSlot(0).isEmpty())
            getInventory().setStackInSlot(0, new ItemStack(Items.SALMON));
    }

    @Override
    public String getDefaultName() { return "Luna"; }

    @Override
    public float getYOffset() { return -0.2F; }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.34F; }

    @Override
    public void onSpawnAnnounce() {
        sendProximityMessage("Love it here owo");
        playNpcSound(ModSounds.GIRLS_LUNA_OWO);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM_TIMER, 0.0F);
        entityData.define(ENCHANT_ITEM, ItemStack.EMPTY);
        entityData.define(IS_ON_BACK, false);
        entityData.define(PAYMENT_ITEM, ItemStack.EMPTY);
    }

    // =========================================================================
    //  IA y Metas
    // =========================================================================

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // followPlayerGoal debe estar definido en NpcInventoryEntity
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
    }

    // =========================================================================
    //  Interacciones
    // =========================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Primero intentamos la interacción base (Inventario, etc)
        InteractionResult baseResult = super.mobInteract(player, hand);
        if (baseResult.consumesAction()) return baseResult;

        ItemStack held = player.getItemInHand(hand);

        // Mecánica rápida: Dar salmón para iniciar secuencia
        if (held.is(Items.SALMON)) {
            if (!level().isClientSide()) {
                held.shrink(1);
                findAndNavigateToBed();
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }

        // Abrir menú de acciones si no está ocupada
        if (level().isClientSide()) {
            if (!openActionMenuIfFree(player)) {
                sendProximityMessage(net.minecraft.client.resources.language.I18n.get("bia.dialogue.busy"));
            }
        }

        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean openActionMenuIfFree(Player player) {
        String[] actions = { "action.names.interact_a", "action.names.interact_b", "action.names.headpat" };
        ItemStack[] costs = {
                new ItemStack(Items.SALMON, 3),
                new ItemStack(Items.SALMON, 2),
                null
        };
        openActionMenu(player, this, actions, costs);
        return true;
    }

    // =========================================================================
    //  Lógica de Ticks
    // =========================================================================

    @Override
    public void tick() {
        super.tick();
        applyVisualEnchantments();
        tickWaitSequence();
    }

    private void applyVisualEnchantments() {
        ItemStack source = entityData.get(ENCHANT_ITEM);
        if (source.isEmpty()) return;
        var enchants = EnchantmentHelper.getEnchantments(source);
        EnchantmentHelper.setEnchantments(enchants, heldVisualItem);
    }

    private void tickWaitSequence() {
        if (getAnimState() != AnimState.WAIT_CAT) {
            sitDownTimer = 0;
            return;
        }

        Player nearest = level().getNearestPlayer(this, 10.0);
        if (nearest == null || distanceTo(nearest) > 1.25F) return;

        if (level().isClientSide()) {
            handleClientWait(nearest, sitDownTimer);
        } else {
            if (sitDownTimer == 25) {
                startInteractionSequence(nearest);
            }
        }
        sitDownTimer++;
    }

    private void startInteractionSequence(Player partner) {
        setPartnerUUID(partner.getUUID());
        partner.setDeltaMovement(Vec3.ZERO);
        partner.teleportTo(getX(), getY(), getZ());
        setAnimState(AnimState.COWGIRL_SITTING_INTRO);
        partner.setYRot(getYRot() + 180.0F);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClientWait(Player player, int tick) {
        Player local = net.minecraft.client.Minecraft.getInstance().player;
        if (local != null && local.getUUID().equals(player.getUUID())) {
            if (tick == 0) {
                TransitionScreen.show();
                local.setDeltaMovement(Vec3.ZERO);
                ClientStateManager.setFrozen(false);
            }
            if (tick == 25) {
                net.minecraft.client.Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
        }
    }

    // =========================================================================
    //  Navegación y Camas
    // =========================================================================

    private void findAndNavigateToBed() {
        BlockPos nearest = findNearestBed(blockPosition());
        if (nearest != null) {
            navigateToBed(nearest);
        } else {
            playNpcSound(ModSounds.GIRLS_LUNA_GIGGLE);
            sendProximityMessage("Heh.. no bed nearby, but thanks for the fish! nya~");
        }
    }

    @Nullable
    private BlockPos findNearestBed(BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 2, 8))) {
            if (level().getBlockState(pos).getBlock() instanceof BedBlock) return pos.immutable();
        }
        return null;
    }

    private void navigateToBed(BlockPos pos) {
        // Lógica de búsqueda de lado libre
        Vec3 target = Vec3.atBottomCenterOf(pos).add(0, 0, 1.0); // Simplificado para el ejemplo
        getNavigation().moveTo(target.x, target.y, target.z, 0.3);
        approachingBed = true;
    }

    // =========================================================================
    //  Callbacks de Acción (triggerAction)
    // =========================================================================

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.interact_a" -> { setAnimState(AnimState.WAIT_CAT); sitDownTimer = 0; }
            case "action.names.interact_b" -> {
                setAnimState(AnimState.TOUCH_BOOBS_INTRO);
                setPartnerUUID(playerId);
            }
            case "action.names.headpat"    -> setAnimState(AnimState.HEAD_PAT);
            case "action.names.followme"   -> setMaster("master", playerId.toString());
            case "action.names.stopfollowme" -> resetInteractionState();
            case "action.names.gohome" -> {
                resetInteractionState();
                ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getUUID()));
            }
        }
    }

    @Override
    public void resetInteractionState() { // Antes resetSexState
        super.resetInteractionState();
        entityData.set(IS_ON_BACK, false);
        setAnimState(AnimState.NULL);
    }

    // =========================================================================
    //  Controladores GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "eyes", 5, state -> {
            return state.setAndContinue(getAnimState() == AnimState.NULL
                    ? RawAnimation.begin().thenLoop("animation.cat.blink")
                    : RawAnimation.begin().thenLoop("animation.cat.null"));
        }));

        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.null"));
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.idle"));
        }));

        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == null) return PlayState.STOP;

            // Los strings de los JSON se mantienen para no romper modelos
            RawAnimation raw = switch (anim) {
                case TOUCH_BOOBS_INTRO     -> RawAnimation.begin().thenPlay("animation.cat.touch_boobs_intro");
                case TOUCH_BOOBS_SLOW      -> RawAnimation.begin().thenLoop("animation.cat.touch_boobs_slow");
                case COWGIRL_SITTING_INTRO -> RawAnimation.begin().thenPlay("animation.cat.sitting_intro");
                case COWGIRL_SITTING_SLOW  -> RawAnimation.begin().thenLoop("animation.cat.sitting_slow");
                case WAIT_CAT              -> RawAnimation.begin().thenLoop("animation.cat.wait");
                case HEAD_PAT              -> RawAnimation.begin().thenPlay("animation.cat.head_pat");
                default -> RawAnimation.begin().thenLoop("animation.cat.null");
            };
            return state.setAndContinue(raw);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}