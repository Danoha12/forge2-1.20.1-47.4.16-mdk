package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TribeEggItem - ported from b9.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * sexmod:tribe_egg - right-click to raytrace 5 blocks and spawn a tribe at the hit pos.
 *
 * In 1.12.2:
 *   World.func_147447_a - level.clip(ClipContext)
 *   RayTraceResult.Type.MISS - HitResult.Type.MISS
 *   rayTraceResult.field_72307_f (hitVec) - hitResult.getLocation()
 *   player.field_71075_bZ.field_75098_d (creative) - player.getAbilities().instabuild
 *   itemStack.func_190918_g(1) - stack.shrink(1)
 *   ax.a(world, hitPos) - TribeManager.spawnTribeAt(level, pos)
 */
public class TribeEggItem extends Item {

    public static final TribeEggItem INSTANCE = new TribeEggItem();

    public TribeEggItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 eye  = player.getEyePosition(0.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end  = eye.add(look.x * 5, look.y * 5, look.z * 5);

        HitResult hit = level.clip(new ClipContext(
            eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit == null || hit.getType() == HitResult.Type.MISS)
            return InteractionResultHolder.fail(stack);

        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (!level.isClientSide()) TribeManager.spawnTribeAt(level, hit.getLocation());

        return InteractionResultHolder.success(stack);
    }
}
