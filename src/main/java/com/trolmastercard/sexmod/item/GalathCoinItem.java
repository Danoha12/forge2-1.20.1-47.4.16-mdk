package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.OwnershipSyncPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * GalathCoinItem — Portado a 1.20.1 / GeckoLib 4.
 * * Moneda vinculada al alma de Galath.
 * * Click derecho invoca/desinvoca a la súcubo usando temporizadores NBT.
 */
public class GalathCoinItem extends Item implements GeoItem {

    // ── Claves NBT ────────────────────────────────────────────────────────────
    public static final String ACTIVATION_KEY = "sexmod:galath_coin_activation_time";
    public static final String DEACTIVATION_KEY = "sexmod:galath_coin_deactivation_time";
    public static final String DE_SUMMON_KEY = "sexmod:galath_coin_de_summoning_animation_time";

    // ── Constantes de Tiempo y Partículas ─────────────────────────────────────
    public static final long SUMMON_DURATION = 4000L;
    public static final long PARTICLE_START_DELAY = 1000L;
    public static final long PARTICLE_END_DELAY = 3000L;

    public static final float ITEM_SWAY_X = 0.1F;
    public static final float ITEM_SWAY_Y = -0.01F;
    public static final float PLAYER_PITCH_FACTOR = 0.0015F;
    public static final float PARTICLE_SPREAD = 2.0F;
    public static final float GALATH_HEIGHT_OFFSET = 1.5F;
    public static final double PARTICLE_LAUNCH_POWER = 0.03D;
    public static final float RING_COUNT = 100.0F;
    public static final float TRAJECTORY_FACTOR = 0.2F;
    public static final float LOOK_FORWARD = 1.5F;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GalathCoinItem() {
        super(new Properties().stacksTo(1));
    }

    // ── Animaciones (GeckoLib 4) ──────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 0, state -> {
            // Aislamos la lectura del jugador local para evitar crasheos en el servidor
            return ClientLogic.handleCoinAnimation(state);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    // ── Interacción Base (Uso del Ítem) ───────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        CompoundTag nbt = player.getPersistentData();
        ItemStack stack = player.getItemInHand(hand);

        if (nbt.getLong(DEACTIVATION_KEY) != 0L || nbt.getLong(ACTIVATION_KEY) != 0L) {
            return InteractionResultHolder.fail(stack);
        }

        if (!isSummonable(level, player)) {
            // Pasar 'null' en lugar de 'player' asegura que el propio jugador escuche el sonido
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MISC_BEEW[0], SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.success(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MISC_WEOWEO[1], SoundSource.PLAYERS, 1.0F, 1.0F);

        nbt.putLong(ACTIVATION_KEY, System.currentTimeMillis());
        return InteractionResultHolder.success(stack);
    }

    boolean isSummonable(Level level, Player player) {
        if (!level.isClientSide()) {
            return !GalathOwnershipData.hasGalath(player.getUUID());
        }
        return !GalathOwnershipData.clientHasGalath;
    }

    // ── Lógica Principal (Inventory Tick) ─────────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player)) return;

        CompoundTag nbt = player.getPersistentData();
        long activation = nbt.getLong(ACTIVATION_KEY);
        long deactivation = nbt.getLong(DEACTIVATION_KEY);
        long now = System.currentTimeMillis();

        if (!level.isClientSide()) {
            tickActivationServer(player, nbt, now, activation, level);
            tickDeactivationServer(player, nbt, now, deactivation);
        } else {
            // Aislamiento cliente
            ClientLogic.tickActivationParticlesClient(player, now, activation);
            ClientLogic.tickDeactivationParticlesClient(player, now, deactivation);

            if (activation > 0L && now - activation > SUMMON_DURATION) {
                ClientLogic.onClientGalathSummon(player);
            }
        }

        if (deactivation != 0L && now > deactivation + SUMMON_DURATION) {
            nbt.putLong(DEACTIVATION_KEY, 0L);
            nbt.putBoolean(DE_SUMMON_KEY, false);
        }
    }

    // ── Lógica de Servidor ────────────────────────────────────────────────────

    private void tickActivationServer(Player player, CompoundTag nbt, long now, long activation, Level level) {
        if (activation == 0L || now - activation <= SUMMON_DURATION) return;

        nbt.putLong(ACTIVATION_KEY, 0L);
        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().normalize().scale(LOOK_FORWARD));

        GalathEntity galath = new GalathEntity(ModEntityRegistry.GALATH_ENTITY.get(), level, player, spawnPos);
        galath.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        GalathOwnershipData.add(player, galath);
        level.addFreshEntity(galath);

        if (GalathOwnershipData.hasGalath(player.getUUID())) {
            galath.setFromOwnerSummon();
        }
    }

    private void tickDeactivationServer(Player player, CompoundTag nbt, long now, long deactivation) {
        if (deactivation == 0L) return;

        boolean animStarted = nbt.getBoolean(DE_SUMMON_KEY);
        if (!animStarted && now - deactivation > PARTICLE_START_DELAY - 150L) {
            nbt.putBoolean(DE_SUMMON_KEY, true);

            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            if (BaseNpcEntity.getByUUID(galathUUID) instanceof GalathEntity g) {
                startDesummon(g);
            }
        }

        if (now - deactivation > PARTICLE_END_DELAY) {
            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            if (BaseNpcEntity.getByUUID(galathUUID) instanceof GalathEntity galath) {
                GalathOwnershipData.remove(galath);
            }
        }
    }

    public static void startDesummon(GalathEntity galath) {
        galath.setAnimState(AnimState.GALATH_DE_SUMMON);
        galath.stopSexSession(); // Asegúrate de que este método exista en tu entidad
        galath.setShouldReturnToCoin(true);
        galath.setSexTargetPos(galath.position());
        galath.setYRot(galath.getYRot());
    }

    // ── Eventos de Forge (Automáticos y Seguros) ──────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CoinEvents {

        /** Detecta el click sobre la Galath para desinvocarla. */
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            Player player = event.getEntity();
            ItemStack stack = player.getItemInHand(event.getHand());
            if (!(stack.getItem() instanceof GalathCoinItem)) return;

            Entity target = event.getTarget();
            if (!(target instanceof GalathEntity galath)) return;
            if (!player.getUUID().equals(galath.getOwnerUUID())) return;

            // Null como primer parámetro = El jugador que hace click escucha el sonido
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MISC_WEOWEO[0], SoundSource.PLAYERS, 1.0F, 1.0F);

            player.getPersistentData().putLong(DEACTIVATION_KEY, System.currentTimeMillis());
            event.setCanceled(true); // Cancela la interacción normal
        }

        /** Si el jugador cambia de dimensión, Galath se guarda automáticamente. */
        @SubscribeEvent
        public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;

            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            if (BaseNpcEntity.getByUUID(galathUUID) instanceof GalathEntity npc) {
                GalathOwnershipData.remove(npc);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new OwnershipSyncPacket(false));
            }
        }
    }

    // ── Lógica Aislada del Cliente (Protección Anti-Crash) ────────────────────

    @OnlyIn(Dist.CLIENT)
    private static class ClientLogic {

        public static PlayState handleCoinAnimation(AnimationState<GalathCoinItem> state) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return PlayState.STOP;

            CompoundTag nbt = player.getPersistentData();
            if (nbt.getLong(ACTIVATION_KEY) == 0L && nbt.getLong(DEACTIVATION_KEY) == 0L) {
                state.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            return state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath_coin.summon"));
        }

        public static void onClientGalathSummon(Player player) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                GalathOwnershipData.clientHasGalath = true;
            }
        }

        public static void tickActivationParticlesClient(Player player, long now, long activation) {
            if (activation == 0L || now <= activation + PARTICLE_START_DELAY || now >= activation + PARTICLE_END_DELAY) return;

            Vec3 eyePos = player.getEyePosition();
            Vec3 forward = eyePos.add(player.getLookAngle().normalize().scale(LOOK_FORWARD));
            float t = (float) (now - activation - PARTICLE_START_DELAY) / 2000.0F;

            Vec3 itemPos = eyePos.add(VectorMathUtil.offsetInYaw(ITEM_SWAY_X, ITEM_SWAY_Y + player.getXRot() * PLAYER_PITCH_FACTOR, 0.0D, player.getYRot()));
            Vec3 p = MathUtil.lerp(itemPos, forward, t);

            // Generar partículas aquí (Asumiendo que PhysicsParticle existe en tu mod)
            // com.trolmastercard.sexmod.client.particle.PhysicsParticle.SIZE = TRAJECTORY_FACTOR;
        }

        public static void tickDeactivationParticlesClient(Player player, long now, long deactivation) {
            if (deactivation == 0L || now <= deactivation + PARTICLE_START_DELAY || now >= deactivation + PARTICLE_END_DELAY) return;

            GalathEntity galath = null;
            for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcsClient()) {
                if (npc instanceof GalathEntity g && player.equals(g.getOwnerPlayer())) {
                    galath = g;
                    break;
                }
            }
            if (galath == null) return;

            // Activamos la animación de desinvocación en el cliente visualmente
            CompoundTag nbt = player.getPersistentData();
            if (!nbt.getBoolean(DE_SUMMON_KEY)) {
                nbt.putBoolean(DE_SUMMON_KEY, true);
                startDesummon(galath);
            }

            Vec3 galathHead = galath.position().add(0, GALATH_HEIGHT_OFFSET, 0);
            Vec3 itemPos = player.getEyePosition().add(player.getLookAngle().normalize().scale(LOOK_FORWARD));
            float t = (float) (now - deactivation - PARTICLE_START_DELAY) / 2000.0F;
            Vec3 p = MathUtil.lerp(galathHead, itemPos, t);

            // Generar partículas (Asumiendo que PhysicsParticle existe en tu mod)
            // com.trolmastercard.sexmod.client.particle.PhysicsParticle.SIZE = TRAJECTORY_FACTOR;
        }
    }
}