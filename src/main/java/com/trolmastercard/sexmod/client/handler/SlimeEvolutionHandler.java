package com.trolmastercard.sexmod.client.handler;

import com.trolmastercard.sexmod.entity.SlimeEntity;
import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sexmod")
public class SlimeEvolutionHandler {

    // 24000 ticks = 20 minutos (un día de Minecraft)
    private static final int GROWTH_TIME = 24000;

    @SubscribeEvent
    public static void onSlimeTick(LivingEvent.LivingTickEvent event) {
        // 1. Solo trabajamos en el Servidor y con Slimes de vainilla
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Slime vanillaSlime)) {
            return;
        }

        // 2. ¿Tiene nuestra etiqueta de "evolución"?
        if (vanillaSlime.getTags().contains("future_chicaslime")) {

            // 3. Usamos NBT para llevar la cuenta del crecimiento
            int growth = vanillaSlime.getPersistentData().getInt("growth_timer");
            growth++;

            if (growth >= GROWTH_TIME) {
                evolveToChicaSlime(vanillaSlime);
            } else {
                vanillaSlime.getPersistentData().putInt("growth_timer", growth);
            }
        }
    }

    private static void evolveToChicaSlime(Slime oldSlime) {
        if (oldSlime.level() instanceof ServerLevel serverLevel) {
            // 4. Creamos a la nueva Chica Slime
            SlimeEntity chicaSlime = ModEntityRegistry.SLIME_NPC.get().create(serverLevel);

            if (chicaSlime != null) {
                // Copiamos la posición y rotación exacta
                chicaSlime.moveTo(oldSlime.getX(), oldSlime.getY(), oldSlime.getZ(), oldSlime.getYRot(), oldSlime.getXRot());

                // 5. El gran cambio: Borramos al moco, aparece la waifu
                serverLevel.addFreshEntity(chicaSlime);
                oldSlime.discard();

                // (Opcional) Partículas de explosión de moco
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        chicaSlime.getX(), chicaSlime.getY() + 1, chicaSlime.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}