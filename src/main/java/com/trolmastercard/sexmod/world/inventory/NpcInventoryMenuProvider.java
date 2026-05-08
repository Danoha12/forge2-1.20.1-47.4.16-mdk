package com.trolmastercard.sexmod.world.inventory; // 🚨 Ruta corregida para tu Packet

import com.trolmastercard.sexmod.entity.BaseNpcEntity; // 🚨 Import corregido
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * NpcInventoryMenuProvider - Abre la pantalla de inventario del NPC para un jugador.
 * (Diseño de Claude, afinado para el chasis de Trolmastercard)
 */
public class NpcInventoryMenuProvider implements MenuProvider {

    private final BaseNpcEntity npc;

    public NpcInventoryMenuProvider(BaseNpcEntity npc) {
        this.npc = npc;
    }

    @Override
    public Component getDisplayName() {
        // Usa el nombre real que tenga la entidad en el juego
        return Component.literal(this.npc.getDisplayName().getString() + " Inventory");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // 🚨 OJO AQUÍ: Tu clase NpcInventoryMenu probablemente necesite la entidad (this.npc)
        // para saber el inventario de quién está abriendo.
        // Si te marca error aquí, es porque no has creado la clase NpcInventoryMenu todavía.
        return new NpcInventoryMenu(id, inv, this.npc.getUUID());
    }
}