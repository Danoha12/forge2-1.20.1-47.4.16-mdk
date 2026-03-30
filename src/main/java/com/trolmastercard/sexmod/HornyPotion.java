package com.trolmastercard.sexmod;

import com.trolmastercard.sexmod.entity.ai.NpcBreedGoal;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ModelListPacket;
import com.trolmastercard.sexmod.registry.ModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * HornyPotion — Portado a 1.20.1.
 * * Efecto que activa la IA de apareamiento en animales/aldeanos
 * * y abre el menú de selección de modelos en jugadores.
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public class HornyPotion extends MobEffect {

    public HornyPotion() {
        // Categoría Beneficiosa (Azul/Rosa), Color: 16736968 (Rosa fuerte)
        super(MobEffectCategory.BENEFICIAL, 16736968);
    }

    // ── Lógica para Jugadores ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        // Obtenemos el efecto desde nuestro registro
        MobEffectInstance effect = player.getEffect(ModMobEffects.HORNY_POTION.get());

        if (effect != null) {
            // Si le quedan menos de 3500 ticks (es decir, han pasado unos 5 segundos desde que se tomó)
            if (effect.getDuration() < 3500) {
                player.removeEffect(ModMobEffects.HORNY_POTION.get());

                // Abrimos el menú enviando el paquete al cliente
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                        new ModelListPacket(player));
            }
        }
    }

    // ── Lógica para Entidades (Aldeanos y Animales) ─────────────────────────

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        MobEffect horny = ModMobEffects.HORNY_POTION.get();

        // 1. Aldeanos: Añadir meta de apareamiento personalizada
        if (entity instanceof Villager villager && villager.hasEffect(horny)) {
            // Evitamos añadir el mismo Goal varias veces
            villager.goalSelector.addGoal(2, new NpcBreedGoal(villager));
            villager.removeEffect(horny);
        }

        // 2. Animales: Forzar modo amor
        if (entity instanceof Animal animal && animal.hasEffect(horny)) {
            if (animal.getAge() >= 0) {
                animal.setAge(0);
                animal.setInLove(null); // null significa que no hay un "jugador" que lo causó

                // Buscar objetivo cercano (jugador)
                Player nearest = entity.level().getNearestPlayer(entity, 30.0D);
                if (nearest != null) {
                    animal.setTarget(nearest);
                }
            }
            animal.removeEffect(horny);
        }
    }
}