package com.trolmastercard.sexmod.handler;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles name-tag renaming of NPC entities.
 * Ported from f4.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * When a player right-clicks an NPC while holding a named name tag the NPC
 * is given that name; the name tag is consumed if the player is not in
 * creative mode.
 *
 * Register on the FORGE event bus.
 */
public class NameTagEventHandler {

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Entity entity = event.getTarget();
        if (!(entity instanceof BaseNpcEntity npc)) return;

        Player player = event.getEntity();

        // Prefer main hand, fall back to off-hand
        ItemStack stack;
        if (player.getMainHandItem().getItem() == Items.NAME_TAG) {
            stack = player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() == Items.NAME_TAG) {
            stack = player.getOffhandItem();
        } else {
            return;
        }

        // Name tags without a custom name do nothing
        String name = stack.hasCustomHoverName() ? stack.getHoverName().getString() : "";
        if (name.isEmpty()) return;

        // Apply the name to the NPC
        npc.setCustomName(net.minecraft.network.chat.Component.literal(name));

        // Consume the tag unless in creative
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        event.setCanceled(true);
        event.setResult(Event.Result.DENY);
    }
}
