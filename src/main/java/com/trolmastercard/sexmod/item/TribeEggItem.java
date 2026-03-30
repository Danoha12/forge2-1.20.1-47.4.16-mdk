package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TribeEggItem — Portado a 1.20.1.
 * * Ítem de spawn especial: sexmod:tribe_egg
 * * Haz clic derecho para trazar un rayo de 5 bloques y spawnear una tribu
 * en la posición de impacto.
 */
public class TribeEggItem extends Item {

    // Instancia Singleton (Si usas DeferredRegister, esto se manejará en tu clase ModItems)
    public static final TribeEggItem INSTANCE = new TribeEggItem();

    public TribeEggItem() {
        // Configuramos el tamaño máximo del stack a 64
        super(new Properties().stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1. Cálculos de Raytrace (Mirada del jugador a 5 bloques de distancia)
        Vec3 eye = player.getEyePosition(1.0F); // 1.0F = partialTick (tiempo actual)
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.x * 5.0, look.y * 5.0, look.z * 5.0);

        // Trazamos el rayo considerando colisiones con bloques (OUTLINE) y omitiendo líquidos (NONE)
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));

        // 2. Verificación de Impacto
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack); // 'pass' permite que otros eventos sigan ocurriendo
        }

        // 3. Lógica de Consumo (Solo si no está en modo Creativo)
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // 4. Spawn de la Tribu (Solo en el lado del Servidor)
        if (!level.isClientSide()) {
            // hit.getLocation() devuelve el vector x,y,z exacto donde pegó el rayo
            TribeManager.spawnTribeAt(level, hit.getLocation());
        }

        // Devolvemos sidedSuccess para que el cliente haga la animación de mover el brazo
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}