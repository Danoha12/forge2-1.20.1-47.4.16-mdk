package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.client.renderer.item.KoboldEggItemRenderer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.registry.ModEntities;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * KoboldEggSpawnItem — Portado a 1.20.1 / GeckoLib 4.
 * * Al usar este ítem en un bloque, spawnea una entidad KoboldEgg.
 * * El color y la tribu se heredan de los datos del ItemStack (Damage y NBT).
 */
public class KoboldEggSpawnItem extends Item implements GeoItem {

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public KoboldEggSpawnItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // ── Lógica de Spawning ────────────────────────────────────────────────────

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();

        // El spawning solo debe ocurrir en el servidor
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = ctx.getItemInHand();
        Vec3 hitPos = ctx.getClickLocation();
        Player player = ctx.getPlayer();

        // Crear la entidad del huevo
        KoboldEgg egg = new KoboldEgg(ModEntities.KOBOLD_EGG.get(), level);

        // Posicionar el huevo ligeramente arriba del punto de impacto para evitar que se atore
        egg.moveTo(hitPos.x, hitPos.y, hitPos.z, 0.0F, 0.0F);

        // 1. Asignar Color: Convertimos el 'damage' del ítem al nombre del color
        int colorId = stack.getDamageValue();
        String colorName = EyeAndKoboldColor.getColorByWoolId(colorId).toString();
        egg.getEntityData().set(KoboldEgg.BODY_COLOR, colorName);

        // 2. Asignar Tribu: Leemos el UUID del NBT si existe
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("tribeID")) {
            try {
                egg.setTribeId(UUID.fromString(nbt.getString("tribeID")));
            } catch (IllegalArgumentException e) {
                // Si el UUID está corrupto, generamos uno nuevo
                egg.setTribeId(UUID.randomUUID());
            }
        }

        // Aparecer la entidad en el mundo
        if (level.addFreshEntity(egg)) {
            // Consumir el ítem si no se está en creativo
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    // ── GeckoLib 4: Configuración del Renderer ───────────────────────────────

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private KoboldEggItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new KoboldEggItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Los ítems usualmente son estáticos, pero aquí podrías añadir
        // una animación de "latido" si quisieras.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}