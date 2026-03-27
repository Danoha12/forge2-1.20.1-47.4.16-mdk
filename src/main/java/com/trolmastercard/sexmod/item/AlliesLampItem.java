package com.trolmastercard.sexmod.item;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.client.screen.AlliesLampScreen;
import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.BedUtil;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * AlliesLampItem - ported from ap.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * A magic lamp that the player rubs to summon an Allie NPC.
 * Has 3 uses (stored in NBT as {@code sexmodUses}).
 * On right-click, plays a 95-tick rub animation and then spawns an {@link AllieEntity}
 * 2 blocks in front of the player.
 *
 * NBT keys (unchanged from original):
 *   "sexmodAllieInUse"     - boolean, true while rub animation is playing
 *   "sexmodAllieInUseTicks"- int, tick counter during rub
 *   "sexmodUses"           - int, number of times used (max 3)
 *   "sexmodAllieID"        - string UUID, the UUID of the summoned Allie
 *
 * Constants:
 *   c = 95   - SUMMON_TICKS   (total ticks for the rub animation)
 *   k = 50   - START_PARTICLE (tick at which particles begin)
 *   a = 150  - PARTICLE_COUNT (CRIT_MAGIC particles emitted at summon)
 *   f = 0.75 - PARTICLE_SPEED
 *
 * In 1.12.2:
 *   - IAnimatable (GeckoLib 3) - GeoItem (GeckoLib 4)
 *   - AnimationFactory - AnimatableInstanceCache
 *   - new AnimationBuilder().addAnimation(...) - RawAnimation.begin().thenPlay(...)
 *   - ModelLoader.setCustomModelResourceLocation - ItemRendererRegistry / renderer event
 *   - TileEntityItemStackRenderer - GeoItemRenderer registered in client setup
 *   - LootTableList constants - ResourceLocations under loot_tables/
 *   - EnumParticleTypes.CRIT_MAGIC - ParticleTypes.ENCHANTED_HIT
 *   - cj.a(world, type, pos, count, spread, speed) - BedUtil.spawnParticles(...)
 *   - d3.a/b - AlliesLampScreen.setEnabled/isEnabled
 */
@Mod.EventBusSubscriber
public class AlliesLampItem extends Item implements GeoItem {

    // NBT keys
    public static final String KEY_IN_USE       = "sexmodAllieInUse";
    public static final String KEY_IN_USE_TICKS = "sexmodAllieInUseTicks";
    public static final String KEY_USES          = "sexmodUses";
    public static final String KEY_ALLIE_ID      = "sexmodAllieID";

    // Constants
    static final int   SUMMON_TICKS    = 95;
    static final int   START_PARTICLE  = 50;
    public static final int   PARTICLE_COUNT = 150;
    public static final float PARTICLE_SPEED = 0.75F;
    public static final int   MAX_USES       = 3;

    /** Singleton - registered via DeferredRegister in ModItems. */
    public static final AlliesLampItem INSTANCE = new AlliesLampItem();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AnimationController<AlliesLampItem> controller;

    public AlliesLampItem() {
        super(new Item.Properties()
            .stacksTo(1)
            .tab(CreativeModeTab.TAB_MISC));
    }

    // =========================================================================
    //  Per-tick update
    //  Original: ap.func_77663_a (inventoryTick equivalent)
    // =========================================================================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player)) return;

        // Only tick the item that is currently in hand
        if (!stack.equals(player.getMainHandItem()) &&
            !stack.equals(player.getOffhandItem())) return;

        CompoundTag entityData = player.getPersistentData();
        boolean inUse = entityData.getBoolean(KEY_IN_USE);
        int ticks     = entityData.getInt(KEY_IN_USE_TICKS);

        if (!inUse) return;

        entityData.putInt(KEY_IN_USE_TICKS, ticks + 1);

        // Spawn building-up particles
        if (ticks > START_PARTICLE && ticks < SUMMON_TICKS) {
            double t = (double)(ticks - START_PARTICLE) / (SUMMON_TICKS - START_PARTICLE);
            t = com.trolmastercard.sexmod.util.MathUtil.easeInOutSine(t);
            Vec3 spawnPos = getSpawnPos(player).add(0, player.getEyeHeight() * (1.0 - t), 0);
            BedUtil.spawnParticles(level, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                spawnPos, (int)(t * PARTICLE_COUNT), t * PARTICLE_SPEED, t);
        }

        if (ticks < SUMMON_TICKS) return;

        // Summon complete
        BedUtil.spawnParticles(level, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
            getSpawnPos(player), PARTICLE_COUNT, PARTICLE_SPEED, 2.0);

        entityData.putBoolean(KEY_IN_USE, false);
        entityData.putInt(KEY_IN_USE_TICKS, 0);

        if (level.isClientSide()) {
            AlliesLampScreen.setEnabled(false);
            return;
        }

        // Server side: increment use count and spawn Allie
        CompoundTag itemTag = stack.getOrCreateTag();
        itemTag.putInt(KEY_USES, itemTag.getInt(KEY_USES) + 1);

        AllieEntity allie = new AllieEntity(level, stack);
        allie.setMasterUUID(player.getUUID());

        Vec3 spawnPos = getSpawnPos(player);
        allie.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        allie.setYRot(player.getYRot() + 180.0F);
        allie.yHeadRot = player.getYRot() + 180.0F;
        allie.setPersistenceRequired();
        allie.setNoAi(false);

        // Determine summon animation based on ground block
        BlockPos below = allie.blockPosition().below();
        if (level.getBlockState(below).getBlock() == net.minecraft.world.level.block.Blocks.SAND) {
            allie.setAnimState(AnimState.SUMMON_SAND);
        } else {
            allie.setAnimState(allie.isFuta() ? AnimState.SUMMON : AnimState.SUMMON_NORMAL);
        }

        level.addFreshEntity(allie);
        stack.setTag(itemTag);
    }

    /** Returns the spawn position 2 blocks in front of the player. */
    Vec3 getSpawnPos(Player player) {
        return player.position().add(
            VectorMathUtil.rotateYaw(new Vec3(0, 0, 2), player.getYRot()));
    }

    // =========================================================================
    //  Tooltip
    // =========================================================================

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        int wishesUsed = tag.getInt(KEY_USES);
        int remaining  = MAX_USES - wishesUsed;
        switch (remaining) {
            case 2  -> tooltip.add(Component.literal("2 wishes left"));
            case 1  -> tooltip.add(Component.literal("1 wish left"));
            case 0  -> tooltip.add(Component.literal("no wishes left"));
        }
    }

    // =========================================================================
    //  GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 2, state -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return PlayState.STOP;

            boolean inUse = mc.player.getPersistentData().getBoolean(KEY_IN_USE);
            if (!inUse) {
                state.getController().forceAnimationReset();
                return PlayState.STOP;
            }

            state.setAnimation(RawAnimation.begin()
                .thenPlay("animation.lamp.rub")
                .thenLoop("animation.lamp.rub"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================================
    //  Loot table injection
    // =========================================================================

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        // Inject into dungeons, strongholds, mineshafts, woodland mansions
        var targets = java.util.Set.of(
            new net.minecraft.resources.ResourceLocation("minecraft", "chests/simple_dungeon"),
            new net.minecraft.resources.ResourceLocation("minecraft", "chests/stronghold_library"),
            new net.minecraft.resources.ResourceLocation("minecraft", "chests/abandoned_mineshaft"),
            new net.minecraft.resources.ResourceLocation("minecraft", "chests/woodland_mansion")
        );

        if (!targets.contains(event.getName())) return;

        LootPool pool = event.getTable().getPool("pool3");
        if (pool == null) pool = event.getTable().getPool("pool2");
        if (pool == null) return;

        pool.addEntry(net.minecraft.world.level.storage.loot.entries.LootItem
            .lootTableItem(INSTANCE)
            .setWeight(5)
            .build());
    }

    // =========================================================================
    //  Right-click handler (inner class a - static nested InteractHandler)
    // =========================================================================

    @Mod.EventBusSubscriber
    public static class InteractHandler {

        @SubscribeEvent
        public static void onPlayerLogOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
            event.getEntity().getPersistentData().putBoolean(KEY_IN_USE, false);
        }

        @SubscribeEvent
        public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
            Player player    = event.getEntity();
            ItemStack stack  = player.getItemInHand(event.getHand());

            // Skip if player is already in a sex sequence
            try {
                if (PlayerKoboldEntity.isPlayerBound(player)) return;
            } catch (ConcurrentModificationException ignored) { return; }

            // Client: check AlliesLampScreen gate
            if (player.level().isClientSide()) {
                if (!AlliesLampScreen.isEnabled()) return;
            }

            // Server: ensure no existing Allie is already holding this exact lamp
            if (!player.level().isClientSide()) {
                try {
                    for (var npc : com.trolmastercard.sexmod.entity.BaseNpcEntity.getAllActive()) {
                        if (npc.isRemoved()) continue;
                        if (!(npc instanceof AllieEntity allie)) continue;
                        if (stack.equals(allie.getLampStack())) return;
                    }
                } catch (ConcurrentModificationException ignored) {}
            }

            if (stack.getItem() != INSTANCE) return;

            // Check uses remaining
            CompoundTag itemTag = stack.getTag();
            if (itemTag != null && itemTag.getInt(KEY_USES) >= MAX_USES) return;

            // Check not already rubbing
            CompoundTag entityData = player.getPersistentData();
            if (entityData.getBoolean(KEY_IN_USE)) return;

            entityData.putBoolean(KEY_IN_USE, true);
            entityData.putInt(KEY_IN_USE_TICKS, 0);
        }
    }
}
