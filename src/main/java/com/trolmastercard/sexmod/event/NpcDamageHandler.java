package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathDamageSource;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NpcDamageHandler - ported from ah.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Handles {@link LivingAttackEvent} on the FORGE event bus to:
 *
 *  1. {@link #onNpcAttacked}: Cancel incoming damage on NPC entities that are
 *     currently in an active sex sequence, or cancel all damage on
 *     {@link PlayerKoboldEntity} instances (they are always invulnerable).
 *
 *  2. {@link #onPlayerAttackedNearNpc}: Cancel damage on players who are
 *     standing too close to their own bound NPC (within 1 block). This
 *     prevents the NPC accidentally hurting the player during sex animations.
 *     Also passes through {@link GalathDamageSource} without cancellation.
 *
 * Original method mapping:
 *   {@code b(LivingAttackEvent)} - {@link #onNpcAttacked(LivingAttackEvent)}
 *   {@code a(LivingAttackEvent)} - {@link #onPlayerAttackedNearNpc(LivingAttackEvent)}
 */
public class NpcDamageHandler {

    // =========================================================================
    //  NPC attack guard
    // =========================================================================

    /**
     * Cancels attacks on NPCs that are:
     *  - {@link PlayerKoboldEntity}: always invulnerable
     *  - Any {@link BaseNpcEntity} with an active sex partner
     *
     * Void damage ({@code DamageSource.OUT_OF_WORLD}) is never cancelled.
     *
     * Equivalent to: {@code ah.b(LivingAttackEvent)}
     */
    @SubscribeEvent
    public void onNpcAttacked(LivingAttackEvent event) {
        if (event.getSource() == event.getEntity().level().damageSources().outOfWorld()) return;
        if (!(event.getEntity() instanceof BaseNpcEntity npc)) return;

        if (npc instanceof PlayerKoboldEntity) {
            // PlayerKoboldEntity is always invulnerable
            event.setCanceled(true);
        } else {
            // Cancel if NPC is currently in a sex sequence
            event.setCanceled(npc.getSexTarget() != null);
        }
    }

    // =========================================================================
    //  Player proximity guard
    // =========================================================================

    /**
     * Cancels damage on players standing within 1 block of their bound NPC.
     * This prevents the NPC's hitbox from damaging the player during animations.
     *
     * {@link GalathDamageSource} is explicitly allowed through (cum drain
     * should still apply).
     *
     * Equivalent to: {@code ah.a(LivingAttackEvent)}
     */
    @SubscribeEvent
    public void onPlayerAttackedNearNpc(LivingAttackEvent event) {
        DamageSource source = event.getSource();

        // Allow void damage and galath cum-drain damage
        if (source == event.getEntity().level().damageSources().outOfWorld()) return;
        if (source instanceof GalathDamageSource) return;

        if (!(event.getEntity() instanceof Player player)) return;

        // Look up the NPC bound to this player
        BaseNpcEntity npc = BaseNpcEntity.getByPlayerUUID(player.getUUID());
        if (npc == null) return;

        // Cancel if player is within 1 block of their NPC
        if (npc.distanceTo((Entity) player) < 1.0F) {
            event.setCanceled(true);
        }
    }
}
