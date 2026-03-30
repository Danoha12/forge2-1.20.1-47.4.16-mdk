package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.InteractionMeterOverlay;
import com.trolmastercard.sexmod.client.gui.TransitionScreen;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/**
 * LunaEntity — Portado a 1.20.1.
 * * Personaje tipo gato con IA de búsqueda de camas y sistema de regalos (Salmón).
 */
public class LunaEntity extends BaseNpcEntity implements GeoEntity {

    public static final EntityDataAccessor<Float> ANIM_TIMER = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.FLOAT);
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
        this.entityData.define(PAYMENT_ITEM, ItemStack.EMPTY);
    }

    @Override
    public String getNpcName() { return "Luna"; }

    @Override
    public float getEyeHeight(Pose pose) { return 1.34F; }

    // ── Interacción con Salmón y Menús ───────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Si le das salmón, Luna se pone feliz y busca una cama
        if (held.is(Items.SALMON)) {
            if (!this.level().isClientSide()) {
                held.shrink(1);
                this.playSound(ModSounds.GIRLS_LUNA_OWO.get(), 1.0F, 1.0F);
                findAndGoToBed();
            }
            return InteractionResult.SUCCESS;
        }

        if (this.level().isClientSide()) {
            // Abrir menú de acciones (Mapear tus botones de la UI aquí)
            player.displayClientMessage(Component.literal("Luna: ¿Quieres jugar conmigo? nya~"), true);
        }

        return InteractionResult.SUCCESS;
    }

    // ── Lógica de "Wait Cat" (Acecho de Gato) ────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickWaitCatLogic();
        }
    }

    private void tickWaitCatLogic() {
        if (getAnimState() != AnimState.WAIT_CAT) {
            sitDownTimer = 0;
            return;
        }

        Player nearest = this.level().getNearestPlayer(this, 2.0D);
        if (nearest != null && this.distanceTo(nearest) < 1.5F) {
            sitDownTimer++;

            // Si el jugador se queda cerca 25 ticks (1.2 segundos), Luna salta
            if (sitDownTimer >= 25) {
                this.setPartnerUUID(nearest.getUUID());
                this.setAnimStateFiltered(AnimState.CAT_SITTING_INTRO);
                sitDownTimer = 0;
            }
        }
    }

    // ── Búsqueda de Camas ─────────────────────────────────────────────────────

    private void findAndGoToBed() {
        BlockPos bedPos = findNearestBed(this.blockPosition());
        if (bedPos == null) {
            this.sendSystemMessage(Component.literal("<Luna> No hay camas cerca nya~"));
            return;
        }
        this.getNavigation().moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 0.5D);
        this.approachingBed = true;
    }

    private BlockPos findNearestBed(BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 2, 8))) {
            if (this.level().getBlockState(pos).getBlock() instanceof BedBlock) return pos.immutable();
        }
        return null;
    }

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "tickle" -> setAnimStateFiltered(AnimState.TICKLE_INTRO);
            case "headpat" -> setAnimStateFiltered(AnimState.HEAD_PAT);
            case "wait" -> setAnimStateFiltered(AnimState.WAIT_CAT);
        }
    }

    // ── GeckoLib 4: Controladores de Luna ────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de ojos (Parpadeo felino)
        registrar.add(new AnimationController<>(this, "eyes", 5, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.blink"))
        ));

        // Movimiento base
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.idle"));
        }));

        // Escenas y Acciones
        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState as = getAnimState();
            if (as == AnimState.NULL) return PlayState.STOP;

            String name = switch (as) {
                case TOUCH_BOOBS_INTRO -> "animation.cat.touch_boobs_intro";
                case TOUCH_BOOBS_SLOW  -> "animation.cat.touch_boobs_slow";
                case HEAD_PAT          -> "animation.cat.head_pat";
                case WAIT_CAT          -> "animation.cat.wait";
                case COWGIRL_SITTING_INTRO -> "animation.cat.sitting_intro";
                case COWGIRL_SITTING_SLOW  -> "animation.cat.sitting_slow";
                default                -> "animation.cat.null";
            };
            return state.setAndContinue(RawAnimation.begin().thenLoop(name));
        }));
    }

    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 0.7, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}