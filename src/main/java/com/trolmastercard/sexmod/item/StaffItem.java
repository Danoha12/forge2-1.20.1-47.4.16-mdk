package com.trolmastercard.sexmod.item;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.TribeUIValuesPacket;
import com.trolmastercard.sexmod.client.screen.StaffCommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * StaffItem — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * El "Command Staff". Al hacer clic derecho, abre el menú radial de comandos de la tribu.
 * Bloquea la interacción con camas y cofres mientras se sostiene para evitar aperturas accidentales.
 */
@Mod.EventBusSubscriber(modid = "mod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StaffItem extends Item implements GeoItem {

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public StaffItem() {
        super(new Properties()
                .stacksTo(1)); // El registro y la pestaña se manejan en ModItems
    }

    // ── Lógica de Uso del Ítem ────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // Solo abrir si hay miembros de la tribu (Kobolds) cerca o en el mundo
            if (!KoboldEntity.getAllKobolds().isEmpty()) {
                openStaffScreen();
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openStaffScreen() {
        Minecraft.getInstance().setScreen(new StaffCommandScreen());
        ModNetwork.CHANNEL.sendToServer(new TribeUIValuesPacket());
    }

    // ── GeckoLib 4 ────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Sin animaciones por ahora
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    // ── Eventos de Bloqueo de Interacción ─────────────────────────────────────

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();

        // Verificar si sostiene el bastón (asumiendo que StaffItem está registrado correctamente)
        boolean holdingStaff = mainHand.getItem() instanceof StaffItem || offHand.getItem() instanceof StaffItem;
        if (!holdingStaff) return;

        var block = event.getLevel().getBlockState(event.getPos()).getBlock();

        // Cancelar interacciones con Camas y Cofres para que el bastón sea la prioridad
        if (block instanceof BedBlock || block instanceof ChestBlock) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setResult(Event.Result.DENY);
            event.setCanceled(true);
        }
    }
}