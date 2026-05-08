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

public class GalathCoinItem extends Item implements GeoItem {

    public static final String ACTIVATION_KEY = "sexmod:galath_coin_activation_time";
    public static final String DEACTIVATION_KEY = "sexmod:galath_coin_deactivation_time";
    public static final String DE_SUMMON_KEY = "sexmod:galath_coin_de_summoning_animation_time";

    public static final long SUMMON_DURATION = 4000L;
    public static final long PARTICLE_START_DELAY = 1000L;
    public static final long PARTICLE_END_DELAY = 3000L;
    public static final float LOOK_FORWARD = 1.5F;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GalathCoinItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 0, state -> ClientLogic.handleCoinAnimation(state)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        CompoundTag nbt = player.getPersistentData();
        ItemStack stack = player.getItemInHand(hand);

        if (nbt.getLong(DEACTIVATION_KEY) != 0L || nbt.getLong(ACTIVATION_KEY) != 0L) {
            return InteractionResultHolder.fail(stack);
        }

        if (!isSummonable(level, player)) {
            // REPARACIÓN: Se añade .get() al sonido
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MISC_BEEW[0].get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.success(stack);
        }

        // REPARACIÓN: Se añade .get() al sonido
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MISC_WEOWEO[1].get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        nbt.putLong(ACTIVATION_KEY, System.currentTimeMillis());
        return InteractionResultHolder.success(stack);
    }

    boolean isSummonable(Level level, Player player) {
        if (!level.isClientSide()) {
            return !GalathOwnershipData.hasGalath(player.getUUID());
        }
        return !GalathOwnershipData.clientHasGalath;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player)) return;

        CompoundTag nbt = player.getPersistentData();
        long activation = nbt.getLong(ACTIVATION_KEY);
        long deactivation = nbt.getLong(DEACTIVATION_KEY);
        long now = System.currentTimeMillis();

        if (!level.isClientSide()) {
            tickActivationServer(player, nbt, now, activation, level);
            tickDeactivationServer(player, nbt, now, deactivation);
        } else {
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

    private void tickActivationServer(Player player, CompoundTag nbt, long now, long activation, Level level) {
        if (activation == 0L || now - activation <= SUMMON_DURATION) return;

        nbt.putLong(ACTIVATION_KEY, 0L);
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(LOOK_FORWARD));

        // 🚨 REPARACIÓN 1: Pon el nombre exacto que usaste en ModEntityRegistry
        GalathEntity galath = new GalathEntity(ModEntityRegistry.GALATH.get(), level);

        galath.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        galath.setMaster(player.getUUID().toString());

        // 🚨 REPARACIÓN 2: Cambiamos 'add' por 'setOwnership'
        GalathOwnershipData.setOwnership(player, galath);

        level.addFreshEntity(galath);
    }
    private void tickDeactivationServer(Player player, CompoundTag nbt, long now, long deactivation) {
        if (deactivation == 0L) return;

        // 🚨 REPARACIÓN 1: Verificamos que estamos en el servidor para poder buscar entidades por UUID de forma nativa
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        boolean animStarted = nbt.getBoolean(DE_SUMMON_KEY);
        if (!animStarted && now - deactivation > 850L) {
            nbt.putBoolean(DE_SUMMON_KEY, true);

            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            // 🚨 REPARACIÓN 2: Usamos serverLevel.getEntity(uuid) que es el estándar de Forge/Minecraft 1.20.1
            if (galathUUID != null && serverLevel.getEntity(galathUUID) instanceof GalathEntity g) {
                startDesummon(g);
            }
        }

        if (now - deactivation > 3000L) {
            UUID galathUUID = GalathOwnershipData.getGalathUUID(player);
            if (galathUUID != null && serverLevel.getEntity(galathUUID) instanceof GalathEntity galath) {
                // 🚨 REPARACIÓN 3: Llamamos al método correcto que definimos en la clase de datos
                GalathOwnershipData.removeOwnership(galath);
            }
        }
    }

    public static void startDesummon(GalathEntity galath) {
        galath.setAnimStateFiltered(AnimState.GALATH_DE_SUMMON); // O el estado que desees
        galath.setSexPartnerUUID(null);
    }

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CoinEvents {

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            Player player = event.getEntity();
            ItemStack stack = player.getItemInHand(event.getHand());
            if (!(stack.getItem() instanceof GalathCoinItem)) return;

            Entity target = event.getTarget();
            if (!(target instanceof GalathEntity galath)) return;
            if (!player.getUUID().equals(galath.getSexPartnerUUID())) return;

            // REPARACIÓN: Se añade .get() al sonido
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MISC_WEOWEO[0].get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            player.getPersistentData().putLong(DEACTIVATION_KEY, System.currentTimeMillis());
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientLogic {
        // ... (Tu lógica de partículas se mantiene igual, asegúrate de que use .r(), .g(), .b() si usas RgbColor)
        public static PlayState handleCoinAnimation(AnimationState<GalathCoinItem> state) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return PlayState.STOP;
            CompoundTag nbt = player.getPersistentData();
            if (nbt.getLong(ACTIVATION_KEY) == 0L && nbt.getLong(DEACTIVATION_KEY) == 0L) {
                return PlayState.STOP;
            }
            return state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath_coin.summon"));
        }

        public static void onClientGalathSummon(Player player) {
            GalathOwnershipData.clientHasGalath = true;
        }

        public static void tickActivationParticlesClient(Player player, long now, long activation) {}
        public static void tickDeactivationParticlesClient(Player player, long now, long deactivation) {}
    }
}