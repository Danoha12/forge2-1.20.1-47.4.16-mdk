package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ModEntityAttributes — Portado a 1.20.1.
 * * Define la vida, daño y velocidad de todas las entidades.
 * * Sin este registro, el juego crashea al intentar spawnear mobs personalizados.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Todas las chicas suelen compartir la misma base de atributos
        var npcAttributes = BaseNpcEntity.createAttributes();

        event.put(ModEntityRegistry.JENNY.get(), npcAttributes.build());
        event.put(ModEntityRegistry.ELLIE.get(), npcAttributes.build());
        event.put(ModEntityRegistry.ALLIE.get(), npcAttributes.build());
        event.put(ModEntityRegistry.BIA.get(), npcAttributes.build());
        event.put(ModEntityRegistry.BEE.get(), npcAttributes.build());
        event.put(ModEntityRegistry.LUNA.get(), npcAttributes.build());
        event.put(ModEntityRegistry.KOBOLD.get(), npcAttributes.build());
        event.put(ModEntityRegistry.GOBLIN.get(), npcAttributes.build());
        event.put(ModEntityRegistry.GALATH.get(), npcAttributes.build());
        event.put(ModEntityRegistry.MANGLELIE.get(), npcAttributes.build());
        event.put(ModEntityRegistry.SLIME_NPC.get(), npcAttributes.build());

        // Avatares y enemigos
        event.put(ModEntityRegistry.WANDERING_ENEMY.get(), npcAttributes.build());

        // Entidades técnicas que heredan de LivingEntity
        event.put(ModEntityRegistry.MANGLELIE_SEX.get(), npcAttributes.build());
    }
}