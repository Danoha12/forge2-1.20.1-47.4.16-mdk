package com.trolmastercard.sexmod.event; // Ajusta a tu paquete de eventos

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.world.damagesource.GalathDamageSource;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NpcDamageHandler — Portado a 1.20.1.
 * * Maneja los eventos de ataque para hacer invulnerables a los NPCs durante
 * * interacciones y proteger al jugador del daño por colisión cercana.
 */
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcDamageHandler {

    // ── Protección de NPCs ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onNpcAttacked(LivingAttackEvent event) {
        // 1.20.1: Usamos .is() con la constante de DamageTypes
        if (event.getSource().is(DamageTypes.OUT_OF_WORLD)) return;

        if (!(event.getEntity() instanceof BaseNpcEntity npc)) return;

        if (npc instanceof PlayerKoboldEntity) {
            // Los Kobolds de la tribu son inmortales
            event.setCanceled(true);
        } else {
            // Cancelar el daño si el NPC tiene un objetivo de interacción activo
            if (npc.getSexTarget() != null) {
                event.setCanceled(true);
            }
        }
    }

    // ── Protección de Proximidad del Jugador ─────────────────────────────────

    @SubscribeEvent
    public static void onPlayerAttackedNearNpc(LivingAttackEvent event) {
        DamageSource source = event.getSource();

        // 1.20.1: Verificación segura del vacío
        if (source.is(DamageTypes.OUT_OF_WORLD)) return;

        // Mantenemos tu clase custom, pero revisa el "Pro-Tip" abajo
        if (source instanceof GalathDamageSource) return;

        if (!(event.getEntity() instanceof Player player)) return;

        // Buscamos al NPC vinculado a este jugador
        BaseNpcEntity npc = BaseNpcEntity.getByPlayerUUID(player.getUUID());
        if (npc == null) return;

        // Si el jugador está a menos de 1 bloque de su NPC, cancelamos el daño
        // (No es necesario castear a Entity, distanceTo lo acepta)
        if (npc.distanceTo(player) < 1.0F) {
            event.setCanceled(true);
        }
    }
}