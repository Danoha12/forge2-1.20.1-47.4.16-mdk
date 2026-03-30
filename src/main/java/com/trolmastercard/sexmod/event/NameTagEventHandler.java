package com.trolmastercard.sexmod.event; // Ajusta el paquete según tu estructura

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NameTagEventHandler — Portado a 1.20.1.
 * * Permite renombrar a los NPCs del mod usando una Etiqueta de Nombre (Name Tag).
 * * Registrado automáticamente en el bus FORGE.
 */
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NameTagEventHandler {

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {

        // Verificamos si hicimos clic en un NPC de nuestro mod
        if (!(event.getTarget() instanceof BaseNpcEntity npc)) return;

        // event.getItemStack() nos da mágicamente el ítem de la mano que disparó este evento
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // Si es una Name Tag y tiene un nombre personalizado escrito en el yunque
        if (stack.is(Items.NAME_TAG) && stack.hasCustomHoverName()) {

            // TODA la lógica de consumo y datos debe ir solo en el Servidor
            if (!event.getLevel().isClientSide()) {

                // Aplicamos el nombre (pasando directamente el Component de la Name Tag)
                npc.setCustomName(stack.getHoverName());

                // Comportamiento Vanilla: Hacerlo visible y evitar que el NPC despawnee
                npc.setCustomNameVisible(true);
                npc.setPersistenceRequired();

                // Consumir la Name Tag si no estamos en creativo
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }

            // Cancelamos el evento para que no se abra ningún menú de UI (como el inventario del NPC)
            // y le decimos a Forge que la acción fue un ÉXITO (para que el brazo del jugador se mueva).
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        }
    }
}