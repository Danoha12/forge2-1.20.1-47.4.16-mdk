package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.entity.LunaEntity;
import com.trolmastercard.sexmod.entity.FishingHookEntity;
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
 * LunaRodItem — Caña de pescar personalizada para el NPC Luna.
 * Portado a 1.20.1 y enmascarado (SFW).
 * * Permite a Luna lanzar y recoger su anzuelo especial.
 * La velocidad del lanzamiento depende de la distancia a su "casa".
 */
public class LunaRodItem extends FishingRodItem {

    public LunaRodItem() {
        super(new Properties()
                .stacksTo(1)
                .durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Si el usuario no es nuestra Luna, usamos el comportamiento de vainilla
        if (!(player instanceof LunaEntity luna)) {
            return super.use(level, player, hand);
        }

        // --- Lógica de recuperación (Retrieve) ---
        if (luna.getFishingHook() != null) {
            int rodDmg = luna.getFishingHook().retrieve(stack);

            // Aplicar daño a la caña
            stack.hurtAndBreak(rodDmg, luna, e -> e.broadcastBreakEvent(hand));
            luna.swing(hand);

            level.playSound(null, luna.getX(), luna.getY(), luna.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL,
                    1.0f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        }
        // --- Lógica de lanzamiento (Cast) ---
        else {
            level.playSound(null, luna.getX(), luna.getY(), luna.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                    0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));

            if (!level.isClientSide) {
                // Cálculo de velocidad basado en la distancia al punto de encuentro/casa
                Vec3 home = luna.getHomePos() != null ? luna.getHomePos() : luna.position();
                double dist = luna.position().distanceTo(home);

                // CAST_SPEED_MODIFIER debe ser una constante estática en LunaEntity
                float speed = (float)(dist * LunaEntity.CAST_SPEED_MODIFIER);

                // Configurar el dueño antes de spawnear el anzuelo
                FishingHookEntity.setPendingOwner(luna);
                FishingHookEntity hook = new FishingHookEntity(level, luna, speed);

                // Aplicar encantamientos de la caña al anzuelo
                int lure = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LURE, stack);
                int luck = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, stack);

                if (lure > 0) hook.setLure(lure);
                if (luck > 0) hook.setLuck(luck);

                level.addFreshEntity(hook);
            }
            luna.swing(hand);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}