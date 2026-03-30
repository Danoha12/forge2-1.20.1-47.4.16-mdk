package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * BlockEventHandler — Portado a 1.20.1 y enmascarado (SFW).
 * * Manejador de eventos del mundo para la protección de bloques (Camas).
 * Evita que se rompan camas mientras un NPC las está utilizando en una interacción.
 */
@Mod.EventBusSubscriber
public class BlockEventHandler {

    /** Radio de búsqueda alrededor de la cama destruida. */
    private static final int BED_CHECK_RADIUS = 3;

    // ── Protección de ruptura de cama ───────────────────────────────────────

    /**
     * Cancela la ruptura de una cama si hay algún NPC cercano utilizándola
     * (Estado de interacción activo) y envía un mensaje al jugador.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();

        // Solo nos interesan las camas
        if (!(state.getBlock() instanceof BedBlock)) return;

        BlockPos pos = event.getPos();

        // En 1.20.1 getLevel() es LevelAccessor, necesitamos Level para getEntitiesOfClass
        if (!(event.getLevel() instanceof Level level)) return;

        double r = BED_CHECK_RADIUS;
        AABB aabb = new AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r, pos.getY() + r, pos.getZ() + r
        );

        // Buscar NPCs de nuestro mod en el área
        List<BaseNpcEntity> nearbyNpcs = level.getEntitiesOfClass(BaseNpcEntity.class, aabb);
        boolean isBedInUse = false;

        for (BaseNpcEntity npc : nearbyNpcs) {
            // Verificamos si el NPC está en estado de interacción (Frozen)
            if (!npc.isRemoved() && npc.getEntityData().get(BaseNpcEntity.IS_FROZEN)) {
                isBedInUse = true;
                break;
            }
        }

        if (isBedInUse) {
            // Enmascaramiento SFW: Mensaje profesional
            event.getPlayer().displayClientMessage(
                    Component.translatable("mod.message.bed_in_use"),
                    true // Mostrar en el Action Bar (encima del inventario)
            );

            // Si no tienes el archivo de traducción, usa este literal por ahora:
            // Component.literal("This bed is currently in use. Please do not disturb.")

            event.setCanceled(true);
        }
    }
}