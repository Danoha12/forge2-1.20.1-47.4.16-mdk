package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * AlliesLampItem — Portado a 1.20.1 / GeckoLib 4.
 * * Lámpara mágica para invocar al NPC Allie. Tiene 3 usos.
 */
public class AlliesLampItem extends Item implements GeoItem {

    public static final String KEY_IN_USE = "modAllieInUse";
    public static final String KEY_IN_USE_TICKS = "modAllieInUseTicks";
    public static final String KEY_USES = "modUses";

    static final int SUMMON_TICKS = 95;
    static final int START_PARTICLE = 50;
    public static final int MAX_USES = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AlliesLampItem() {
        super(new Properties().stacksTo(1));
    }

    // ── GECKOLIB 4: Renderizado de Ítems (VITAL PARA 1.20.1) ─────────────────

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private software.bernie.geckolib.renderer.GeoItemRenderer<AlliesLampItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    // Tendrás que crear esta clase renderizadora después
                    // this.renderer = new AlliesLampRenderer();
                }
                return this.renderer;
            }
        });
    }

    // ── Lógica de Uso ────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag itemTag = stack.getOrCreateTag();

        if (itemTag.getInt(KEY_USES) >= MAX_USES) {
            return InteractionResultHolder.pass(stack);
        }

        CompoundTag entityData = player.getPersistentData();
        if (entityData.getBoolean(KEY_IN_USE)) {
            return InteractionResultHolder.pass(stack);
        }

        // Iniciar frotado
        entityData.putBoolean(KEY_IN_USE, true);
        entityData.putInt(KEY_IN_USE_TICKS, 0);

        // Disparamos la animación globalmente usando el sistema de GeckoLib
        if (!level.isClientSide()) {
            // Requiere registrar la sincronización en la clase del Renderizador
            // triggerAnim(player, GeoItem.getOrAssignId(stack, (net.minecraft.server.level.ServerLevel) level), "controller", "rub");
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player)) return;
        if (stack != player.getMainHandItem() && stack != player.getOffhandItem()) return;

        CompoundTag entityData = player.getPersistentData();
        if (!entityData.getBoolean(KEY_IN_USE)) return;

        int ticks = entityData.getInt(KEY_IN_USE_TICKS) + 1;
        entityData.putInt(KEY_IN_USE_TICKS, ticks);

        // TODO: Lógica de partículas (BedUtil) cuando el cliente lo alcance

        if (ticks < SUMMON_TICKS) return;

        // Limpieza tras invocación
        entityData.putBoolean(KEY_IN_USE, false);
        entityData.putInt(KEY_IN_USE_TICKS, 0);

        if (!level.isClientSide()) {
            CompoundTag itemTag = stack.getOrCreateTag();
            itemTag.putInt(KEY_USES, itemTag.getInt(KEY_USES) + 1);
            stack.setTag(itemTag);

            // TODO: Invocar a AllieEntity aquí
        }
    }

    // ── Animación y Tooltip ──────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 2, state -> {
            // Como fallback, la lámpara se anima en primera persona si el NBT está activo
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getPersistentData().getBoolean(KEY_IN_USE)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.lamp.rub"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int remaining = MAX_USES - (stack.hasTag() ? stack.getTag().getInt(KEY_USES) : 0);
        tooltip.add(Component.literal(remaining + " wishes left"));
    }

    // ── Eventos Estáticos ────────────────────────────────────────────────────

    @Mod.EventBusSubscriber
    public static class Events {
        @SubscribeEvent
        public static void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
            event.getEntity().getPersistentData().putBoolean(KEY_IN_USE, false);
        }

        @SubscribeEvent
        public static void onLootTableLoad(LootTableLoadEvent event) {
            // Implementación básica mantenida. Para Mods grandes de 1.20+, usa GlobalLootModifiers.
            Set<ResourceLocation> targets = Set.of(
                    BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.STRONGHOLD_LIBRARY,
                    BuiltInLootTables.ABANDONED_MINESHAFT, BuiltInLootTables.WOODLAND_MANSION
            );
            if (targets.contains(event.getName())) {
                LootPool pool = event.getTable().getPool("main");
                if (pool != null) {
                    // pool.addEntry(LootItem.lootTableItem(INSTANCE).setWeight(5).build());
                }
            }
        }
    }
}