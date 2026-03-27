package com.trolmastercard.sexmod.potion;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ModelListPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * HornyPotion - Portado a 1.20.1.
 * * Efecto que altera el comportamiento de entidades.
 * * Jugadores: Abre una interfaz (ModelListPacket) tras unos segundos.
 * * Aldeanos/Animales: Fuerza el modo de interacción social o cría.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HornyPotion extends MobEffect {

    // Instancia Singleton original mantenida intacta para no romper imports
    public static final HornyPotion HORNY_EFFECT = new HornyPotion();

    public HornyPotion() {
        // Color Rosa/Magenta original (16736968 en Decimal)
        super(MobEffectCategory.BENEFICIAL, 16736968);
    }

    // =========================================================================
    //  Manejador de Eventos del Jugador
    // =========================================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;

        MobEffectInstance instance = player.getEffect(HORNY_EFFECT);
        if (instance == null) return;

        // Si el contador baja de 3500 (Han pasado 5 segundos reales)
        if (instance.getDuration() > 3500) return;

        // Quitamos efecto y mandamos el paquete original
        player.removeEffect(HORNY_EFFECT);

        // Asumiendo que ModelListPacket acepta el Player o está vacío según tu versión
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new ModelListPacket(player)
        );
    }

    // =========================================================================
    //  Manejador de Eventos de Entidades
    // =========================================================================

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) { // Actualizado a LivingTickEvent para 1.20.1
        LivingEntity entity = event.getEntity();

        // 1. Aldeanos
        if (entity instanceof Villager villager) {
            if (villager.hasEffect(HORNY_EFFECT)) {
                // Asume que NpcBreedGoal es tu meta original
                // villager.goalSelector.addGoal(2, new com.trolmastercard.sexmod.entity.ai.NpcBreedGoal(villager));
                villager.removeEffect(HORNY_EFFECT);
            }
        }

        // 2. Animales Vanilla
        if (entity instanceof Animal animal) {
            if (animal.hasEffect(HORNY_EFFECT)) {
                if (animal.getAge() >= 0) {
                    animal.setAge(0);
                    animal.setInLove(null);

                    Entity nearest = animal.level().getNearestPlayer(animal, 30.0D);
                    if (nearest instanceof LivingEntity le) {
                        animal.setTarget(le);
                    }
                }
                animal.removeEffect(HORNY_EFFECT);
            }
        }
    }
}