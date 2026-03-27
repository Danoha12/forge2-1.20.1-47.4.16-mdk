package com.trolmastercard.sexmod.item;
import com.trolmastercard.sexmod.KoboldEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * StaffItem - ported from hy.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Registry name: sexmod:dragon_staff
 *
 * The "dragon staff" item. When right-clicked while kobolds are present in
 * the world, it opens the {@link StaffCommandScreen} (tribe command radial menu).
 * Also blocks Bed and Chest interactions while held so the player doesn't
 * accidentally open them during tribe management.
 *
 * Two inner event classes (originally nested {@code a}):
 *   - {@link RightClickItemListener}  - opens StaffCommandScreen / sends TribeUIValuesPacket
 *   - {@link RightClickBlockListener} - cancels Bed/Chest right-click while staff is held
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - IAnimatable - GeoItem; AnimationFactory - AnimatableInstanceCache
 *   - AnimationData - AnimatableManager.ControllerRegistrar (empty here - no animations)
 *   - func_77659_a(world, player, hand) - use(level, player, hand)
 *   - EnumActionResult.FAIL - InteractionResultHolder.fail(stack)
 *   - ModelLoader.setCustomModelResourceLocation - item model JSON in assets
 *   - TileEntityItemStackRenderer - BlockEntityWithoutLevelRenderer (handled by GeoItem)
 *   - b.setTileEntityItemStackRenderer(new fa()) - GeoItem provides its own BEWLR
 *   - PlayerInteractEvent.RightClickItem - PlayerInteractEvent.RightClickItem (same)
 *   - world.field_72995_K - level.isClientSide()
 *   - entityPlayer.func_184586_b(EnumHand.MAIN_HAND/OFF_HAND) - getItemInHand(hand)
 *   - ff.aY.isEmpty() - KoboldEntity.getAllKobolds().isEmpty()
 *   - Minecraft.func_71410_x().func_147108_a(new j()) - mc.setScreen(new StaffCommandScreen())
 *   - ge.b.sendToServer(new b3()) - ModNetwork.CHANNEL.sendToServer(new TribeUIValuesPacket())
 *   - block instanceof BlockBed / BlockChest - block instanceof BedBlock / ChestBlock
 *   - param1RightClickBlock.setCancellationResult(FAIL) + setResult(DENY) + setCanceled(true)
 *     - same API in 1.20.1 Forge
 */
public class StaffItem extends Item implements GeoItem {

    public static final StaffItem INSTANCE = new StaffItem();

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public StaffItem() {
        super(new Properties()
                .tab(CreativeModeTab.TAB_MISC)
                .stacksTo(1));
    }

    // -- Item use ---------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Prevent right-click from doing anything server-side
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    // -- Registration -----------------------------------------------------------

    public static void register() {
        INSTANCE.setRegistryName("sexmod", "dragon_staff");
        MinecraftForge.EVENT_BUS.register(RightClickItemListener.class);
    }

    // -- GeckoLib --------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // No animations for the staff item itself
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    // -- Inner event listener ---------------------------------------------------

    public static class RightClickItemListener {

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            Level level = event.getLevel();
            if (!level.isClientSide()) return;

            Player player = event.getEntity();
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand  = player.getItemInHand(InteractionHand.OFF_HAND);

            boolean holdingStaff = mainHand.getItem() == StaffItem.INSTANCE
                    || offHand.getItem() == StaffItem.INSTANCE;
            if (!holdingStaff) return;

            // Only open if there are kobolds in the world
            if (KoboldEntity.getAllKobolds().isEmpty()) return;

            openStaffScreen();
        }

        @OnlyIn(Dist.CLIENT)
        private static void openStaffScreen() {
            Minecraft.getInstance().setScreen(new StaffCommandScreen());
            ModNetwork.CHANNEL.sendToServer(new TribeUIValuesPacket());
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand  = player.getItemInHand(InteractionHand.OFF_HAND);

            boolean holdingStaff = mainHand.getItem() == StaffItem.INSTANCE
                    || offHand.getItem() == StaffItem.INSTANCE;
            if (!holdingStaff) return;

            var block = event.getLevel()
                    .getBlockState(event.getPos())
                    .getBlock();

            // Cancel Bed and Chest interactions while holding the staff
            if (block instanceof BedBlock || block instanceof ChestBlock) {
                event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                event.setResult(Event.Result.DENY);
                event.setCanceled(true);
            }
        }
    }
}
