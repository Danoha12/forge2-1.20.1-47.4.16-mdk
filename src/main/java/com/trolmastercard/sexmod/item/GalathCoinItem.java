package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.OwnershipSyncPacket;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.data.GalathOwnershipData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;
import java.util.UUID;

/**
 * GalathCoinItem - Portado a 1.20.1 / GeckoLib 4.
 * * Objeto de invocación: Click derecho para invocar a Galath.
 * * Click derecho sobre Galath para regresarla a la moneda.
 */
public class GalathCoinItem extends Item implements GeoItem {

    // ---- Llaves NBT (Mantenidas intactas para no corromper mundos guardados) ----
    public static final String ACTIVATION_KEY   = "sexmod:galath_coin_activation_time";
    public static final String DEACTIVATION_KEY = "sexmod:galath_coin_deactivation_time";
    public static final String DE_SUMMON_KEY    = "sexmod:galath_coin_de_summoning_animation_time";

    // ---- Temporizadores (ms) ----
    public static final long  SUMMON_DURATION      = 4000L;
    public static final long  PARTICLE_START_DELAY = 1000L;
    public static final long  PARTICLE_END_DELAY   = 3000L;

    // ---- Constantes de Partículas ----
    public static final float  ITEM_SWAY_X          = 0.1F;
    public static final float  ITEM_SWAY_Y          = -0.01F;
    public static final float  PLAYER_PITCH_FACTOR  = 0.0015F;
    public static final float  PARTICLE_SPREAD      = 2.0F;
    public static final float  GALATH_HEIGHT_OFFSET = 1.5F;
    public static final double PARTICLE_LAUNCH_POWER = 0.03D;
    public static final float  RING_COUNT           = 100.0F;
    public static final float  TRAJECTORY_FACTOR    = 0.2F;
    public static final float  LOOK_FORWARD         = 1.5F;

    // ---- Lore (Descripción del objeto limpiada) ----
    public static final String LORE_TEXT =
            "Derrotar a esta poderosa entidad vincula su esencia a esta moneda. "
                    + "Usar la moneda la invoca a tu lado para ofrecerte asistencia mágica. "
                    + "Si usas la moneda directamente sobre ella, regresará a su letargo en el objeto.";

    // ---- Singleton ----
    public static final GalathCoinItem INSTANCE = new GalathCoinItem();

    // ---- GeckoLib ----
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    AnimationController<GalathCoinItem> controller;

    public GalathCoinItem() {
        super(new Properties().stacksTo(1));
    }

    // =========================================================================
    //  Controlador de Animación (GeoItem)
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        controller = new AnimationController<>(this, "controller", 0, this::coinAnimPredicate);
        registrar.add(controller);
    }

    @OnlyIn(Dist.CLIENT)
    private PlayState coinAnimPredicate(AnimationState<GalathCoinItem> state) {
        CompoundTag nbt = Minecraft.getInstance().player.getPersistentData();
        if (nbt.getLong(ACTIVATION_KEY) == 0L && nbt.getLong(DEACTIVATION_KEY) == 0L) {
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }
        state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath_coin.summon"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    // =========================================================================
    //  Uso del Item (Click Derecho)
    // =========================================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        CompoundTag nbt = player.getPersistentData();
        InteractionResultHolder<ItemStack> fail = InteractionResultHolder.fail(player.getItemInHand(hand));

        if (nbt.getLong(DEACTIVATION_KEY) != 0L) return fail;
        if (nbt.getLong(ACTIVATION_KEY)   != 0L) return fail;

        if (!isSummonable(level, player)) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MISC_BEEW[0], SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                ModSounds.MISC_WEOWEO[1], SoundSource.PLAYERS, 1.0F, 1.0F);
        nbt.putLong(ACTIVATION_KEY, System.currentTimeMillis());
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    boolean isSummonable(Level level, Player player) {
        if (!level.isClientSide()) {
            return !GalathOwnershipData.hasGalath(player.getUUID());
        }
        return !GalathOwnershipData.clientHasGalath;
    }

    // =========================================================================
    //  Lógica de Inventario (Tick constante)
    // =========================================================================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player)) return;

        CompoundTag nbt = player.getPersistentData();
        long activation   = nbt.getLong(ACTIVATION_KEY);
        long deactivation = nbt.getLong(DEACTIVATION_KEY);
        long now          = System.currentTimeMillis();

        tickActivation(player, nbt, now, activation);
        tickDeactivation(player, nbt, now, deactivation);

        if (deactivation != 0L && now > deactivation + SUMMON_DURATION) {
            nbt.putLong(DEACTIVATION_KEY, 0L);
            nbt.putBoolean(DE_SUMMON_KEY, false);
        }

        if (level.isClientSide()) {
            tickActivationParticlesClient(player, now, activation);
            tickDeactivationParticlesClient(player, now, deactivation);
        }
    }

    // =========================================================================
    //  Helpers de Servidor (Invocación y Retorno)
    // =========================================================================

    void tickActivation(Player player, CompoundTag nbt, long now, long activation) {
        if (activation == 0L) return;
        if (now - activation <= SUMMON_DURATION) return;

        nbt.putLong(ACTIVATION_KEY, 0L);
        Vec3 eyePos = player.getEyePosition();
        Vec3 spawnPos = eyePos.add(player.getLookAngle().normalize().scale(LOOK_FORWARD));

        Level level = player.level();
        if (level.isClientSide()) {
            onClientGalathSummon(player);
            return;
        }

        GalathEntity galath = new GalathEntity(
                com.trolmastercard.sexmod.registry.ModEntityRegistry.GALATH.get(),
                level, player, spawnPos);
        galath.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        GalathOwnershipData.add(player, galath);
        level.addFreshEntity(galath);
        if (GalathOwnershipData.hasGalath(player.getUUID())) {
            galath.setFromOwnerSummon();
        }
    }

    void tickDeactivation(Player player, CompoundTag nbt, long now, long deactivation) {
        if (deactivation == 0L) return;

        boolean animStarted = nbt.getBoolean(DE_SUMMON_KEY);
        if (!animStarted && now - deactivation > PARTICLE_START_DELAY - (player.level().isClientSide() ? 0L : 150L)) {
            nbt.putBoolean(DE_SUMMON_KEY, true);
            beginDesummon(player);
        }

        if (player.level().isClientSide()) return;
        if (now - deactivation <= PARTICLE_END_DELAY) return;

        UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
        BaseNpcEntity npc = BaseNpcEntity.getNpcByUUID(galathUUID);
        if (npc instanceof GalathEntity galath) {
            GalathOwnershipData.remove(galath);
        }
    }

    void beginDesummon(Player player) {
        if (player.level().isClientSide()) {
            desummonClientSide(player);
        } else {
            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            BaseNpcEntity npc = BaseNpcEntity.getNpcByUUID(galathUUID);
            if (npc instanceof GalathEntity g) {
                startDesummon(g);
            }
        }
    }

    // =========================================================================
    //  Helpers de Cliente (Partículas)
    // =========================================================================

    @OnlyIn(Dist.CLIENT)
    private void tickActivationParticlesClient(Player player, long now, long activation) {
        if (activation == 0L || now <= activation + PARTICLE_START_DELAY || now >= activation + PARTICLE_END_DELAY) return;

        Vec3 eyePos  = player.getEyePosition();
        Vec3 forward = eyePos.add(player.getLookAngle().normalize().scale(LOOK_FORWARD));
        float t = (float)(now - activation - PARTICLE_START_DELAY) / 2000.0F;

        Vec3 itemPos = eyePos.add(
                com.trolmastercard.sexmod.util.VectorMathUtil.offsetInYaw(
                        ITEM_SWAY_X, ITEM_SWAY_Y + player.getXRot() * PLAYER_PITCH_FACTOR,
                        0.0D, player.getYRot()));
        Vec3 p = com.trolmastercard.sexmod.util.MathUtil.lerp(itemPos, forward, t);

        // Omitido: Aquí puedes instanciar tu partícula personalizada.
        // com.trolmastercard.sexmod.client.particle.PhysicsParticle.SIZE = TRAJECTORY_FACTOR;
    }

    @OnlyIn(Dist.CLIENT)
    private void tickDeactivationParticlesClient(Player player, long now, long deactivation) {
        if (deactivation == 0L || now <= deactivation + PARTICLE_START_DELAY || now >= deactivation + PARTICLE_END_DELAY) return;

        GalathEntity galath = null;
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (npc.isRemoved() || !npc.level().isClientSide()) continue;
            if (!(npc instanceof GalathEntity g)) continue;
            if (player.equals(npc.getOwnerPlayer())) { galath = g; break; }
        }
        if (galath == null) return;

        Vec3 galathHead = galath.position().add(0, GALATH_HEIGHT_OFFSET, 0);
        Vec3 eyePos     = player.getEyePosition();
        Vec3 itemPos    = eyePos.add(player.getLookAngle().normalize().scale(LOOK_FORWARD));
        float t         = (float)(now - deactivation - PARTICLE_START_DELAY) / 2000.0F;
        Vec3 p          = com.trolmastercard.sexmod.util.MathUtil.lerp(galathHead, itemPos, t);

        // Omitido: Instanciar partícula personalizada.
    }

    @OnlyIn(Dist.CLIENT)
    void onClientGalathSummon(Player player) {
        if (!Minecraft.getInstance().player.getUUID().equals(player.getUUID())) return;
        GalathOwnershipData.clientHasGalath = true;
    }

    @OnlyIn(Dist.CLIENT)
    void desummonClientSide(Player player) {
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (npc.isRemoved() || !npc.level().isClientSide()) continue;
            if (!(npc instanceof GalathEntity g)) continue;
            if (player.equals(npc.getOwnerPlayer())) {
                startDesummon(g);
                return;
            }
        }
    }

    // =========================================================================
    //  Acciones Estáticas de Galath
    // =========================================================================

    public static void startDesummon(GalathEntity galath) {
        galath.setAnimState(AnimState.GALATH_DE_SUMMON);

        // ¡Importante! Métodos neutralizados para sincronizar con BaseNpcEntity / GalathEntity
        galath.stopInteraction();
        galath.setShouldReturnToCoin(true);
        galath.setTargetPos(galath.position());
        galath.setYRot(galath.getYRot());
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnDesummonParticles(UUID playerUUID, GalathEntity galath) {
        Level world = galath.level();
        Vec3 center = galath.isSitting() ? galath.getSeatPos() : galath.position();
        Vec3 head   = center.add(0, GALATH_HEIGHT_OFFSET, 0);
        Random rng  = galath.getRandom();

        for (int i = 0; i < (int) RING_COUNT; i++) {
            Vec3 offset = new Vec3(
                    (rng.nextFloat() * 2.0F - 1.0F) * PARTICLE_SPREAD,
                    (rng.nextFloat() * 2.0F - 1.0F) * PARTICLE_SPREAD,
                    (rng.nextFloat() * 2.0F - 1.0F) * PARTICLE_SPREAD);
            Vec3 vel = offset.scale(-PARTICLE_LAUNCH_POWER);
            Vec3 pos = head.add(offset);
            world.addParticle(ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z,
                    vel.x, vel.y, vel.z);
        }

        if (Minecraft.getInstance().player.getUUID().equals(playerUUID)) {
            GalathOwnershipData.clientHasGalath = false;
        }
    }

    public static void spawnDesummonParticles(Player player, GalathEntity galath) {
        spawnDesummonParticles(player.getUUID(), galath);
    }

    // =========================================================================
    //  Eventos de Forge
    // =========================================================================

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player    = event.getEntity();
        ItemStack stack  = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof GalathCoinItem)) return;

        Entity target = event.getTarget();
        if (!(target instanceof GalathEntity galath)) return;
        if (!player.getUUID().equals(galath.getOwnerUUID())) return;

        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                ModSounds.MISC_WEOWEO[0], SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getPersistentData().putLong(DEACTIVATION_KEY, System.currentTimeMillis());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
        BaseNpcEntity npc = BaseNpcEntity.getNpcByUUID(galathUUID);
        if (npc == null) return;

        GalathOwnershipData.remove((GalathEntity) npc);
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                new OwnershipSyncPacket(false));
    }
}