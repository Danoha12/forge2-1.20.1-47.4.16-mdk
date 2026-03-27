package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.entity.CatEntity;
import com.trolmastercard.sexmod.entity.FishingHookEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * LunaRodItem - custom fishing rod used by the cat NPC.
 * Ported from gp.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original obfuscation:
 *   gp         - LunaRodItem
 *   gi         - FishingHookEntity
 *   eb         - CatEntity
 *   eb.av      - cat.fishingHook  (FishingHookEntity field)
 *   eb.ap      - CatEntity.CAST_SPEED_MODIFIER (float)
 *   gi.b       - FishingHookEntity.pendingOwner (static NPC reference set before new hook)
 *
 * Migration notes:
 *   ItemFishingRod - FishingRodItem
 *   ActionResult<ItemStack> a(World,eb,EnumHand) - InteractionResultHolder<ItemStack> use(Level,Player,InteractionHand)
 *   EnchantmentHelper.func_191528_c - EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LURE, stack)
 *   EnchantmentHelper.func_191529_b - EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, stack)
 *   IItemPropertyGetter / "cast" property - FoV model predicate via item properties
 *   func_77656_e / func_77625_d - maxStackSize / maxDamage on Item.Properties
 *   ModelLoader.setCustomModelResourceLocation - RegisterItemModelEvent / provider
 */
public class LunaRodItem extends FishingRodItem {

    public static final LunaRodItem INSTANCE = new LunaRodItem();

    private LunaRodItem() {
        super(new Properties()
                .stacksTo(1)
                .durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Only NPC cats use this rod - but allow a player to retrieve or cast
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof CatEntity cat)) {
            // Fallback: vanilla fishing behaviour for real players
            return super.use(level, player, hand);
        }

        if (cat.getFishingHook() != null) {
            // Retrieve
            int rodDmg = cat.getFishingHook().retrieve(stack);
            stack.hurtAndBreak(rodDmg, cat, e -> e.broadcastBreakEvent(hand));
            cat.swing(hand);
            level.playSound(null, cat.getX(), cat.getY(), cat.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL,
                    1.0f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        } else {
            // Cast
            level.playSound(null, cat.getX(), cat.getY(), cat.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                    0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));

            if (!level.isClientSide) {
                // Compute cast speed using distance to home
                Vec3 home = new Vec3(cat.getHomePos().x, cat.getHomePos().y, cat.getHomePos().z);
                double dist = cat.position().distanceTo(home);
                float speed = (float)(dist * CatEntity.CAST_SPEED_MODIFIER);

                FishingHookEntity.setPendingOwner(cat);
                FishingHookEntity hook = new FishingHookEntity(level, cat, speed);

                int lure    = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LURE,          stack);
                int luck    = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, stack);
                if (lure > 0)  hook.setLure(lure);
                if (luck > 0)  hook.setLuck(luck);

                level.addFreshEntity(hook);
            }
            cat.swing(hand);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
