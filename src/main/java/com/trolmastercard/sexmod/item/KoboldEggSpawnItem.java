package com.trolmastercard.sexmod.item;
import com.trolmastercard.sexmod.ModEntityRegistry;

import com.trolmastercard.sexmod.entity.KoboldEgg;
import com.trolmastercard.sexmod.entity.EyeAndKoboldColor;
import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * KoboldEggSpawnItem - ported from c7.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Item: {@code sexmod:kobold_egg_item}
 *
 * Right-click on a block - spawns a {@link KoboldEgg} entity at the hit position.
 * The egg's body color is determined by the item's damage/meta value via
 * {@link EyeAndKoboldColor#getColorByWoolId(int)}.
 * If the item has a {@code tribeID} NBT tag, the egg is assigned to that tribe.
 *
 * NOTE: This item differs from {@code TribeEggItem} (b9.class) which spawns a
 *       whole tribe structure.  This item spawns a single hatching-egg mob.
 *
 * Field mapping:
 *   b = animCache   (AnimationFactory - AnimatableInstanceCache)
 *   a = static singleton instance
 *
 * In 1.12.2:
 *   IAnimatable / AnimationFactory   - GeoItem / AnimatableInstanceCache
 *   func_77625_d(1)                  - Item.Properties().stacksTo(1)
 *   setRegistryName / func_77655_b   - handled by DeferredRegister
 *   MinecraftForge.EVENT_BUS.register - @Mod.EventBusSubscriber pattern
 *   ModelBakery / TileEntityItemStackRenderer - BlockEntityWithoutLevelRenderer (GeckoLib)
 *   PlayerInteractEvent.RightClickBlock - useOn(UseOnContext)
 *   world.func_72995_K               - level.isClientSide
 *   itemStack.func_77973_b() != a    - item != this (checked via instanceof / registry)
 *   new i(world) - new KoboldEgg(type, level)  (i.class = KoboldEgg entity)
 *   i.func_70107_b(x,y,z) - setPos(x,y,z)
 *   i.func_184212_Q().func_187227_b(i.b, color) - getEntityData().set(KoboldEgg.BODY_COLOR, color)
 *   nbt.func_74779_i("tribeID") - nbt.getString("tribeID")
 *   itemStack.func_190918_g(1) - stack.shrink(1)
 *   KoboldEgg.f = tribeID field - setTribeId(UUID)
 */
public class KoboldEggSpawnItem extends Item implements GeoItem {

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public KoboldEggSpawnItem() {
        super(new Properties().stacksTo(1));
    }

    // =========================================================================
    //  GeoItem  (originally IAnimatable with empty registerControllers)
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // No animations on the item itself
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    // =========================================================================
    //  useOn  (original: PlayerInteractEvent.RightClickBlock handler)
    // =========================================================================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack  = ctx.getItemInHand();
        Vec3 hitPos      = ctx.getClickLocation();

        // Spawn the KoboldEgg entity
        KoboldEgg egg = new KoboldEgg(ModEntityRegistry.KOBOLD_EGG.get(), level);
        egg.setPos(hitPos.x, hitPos.y, hitPos.z);

        // Set body color from item damage/meta via wool color mapping
        String colorName = EyeAndKoboldColor.getColorByWoolId(
            stack.getDamageValue()).toString();
        egg.getEntityData().set(KoboldEgg.BODY_COLOR, colorName);

        // Assign tribe ID from NBT if present
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("tribeID")) {
            egg.setTribeId(UUID.fromString(nbt.getString("tribeID")));
        }

        level.addFreshEntity(egg);
        stack.shrink(1);

        return InteractionResult.CONSUME;
    }
}
