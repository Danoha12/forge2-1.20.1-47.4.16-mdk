package com.trolmastercard.sexmod.inventory; // Ajusta el paquete según tu estructura

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.LunaEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * GuiHandler — Portado a 1.20.1.
 * * En la 1.20.1, IGuiHandler ya no existe.
 * * Esta clase ahora actúa como una utilidad estática para abrir los inventarios
 * * usando el sistema moderno de NetworkHooks y MenuProvider.
 */
public class GuiHandler {

    public static final int GUI_NPC_CHEST = 0;
    public static final int GUI_NPC_EQUIPMENT = 1;

    /**
     * Abre el inventario del NPC de forma segura.
     * DEBE llamarse SÓLO en el lado del Servidor (ServerSide).
     *
     * @param player El jugador que hace clic derecho al NPC.
     * @param npc    El NPC exacto con el que se está interactuando.
     * @param guiId  El tipo de inventario a abrir.
     */
    public static void openNpcContainer(ServerPlayer player, BaseNpcEntity npc, int guiId) {

        // Creamos un UUID único para esta sesión de inventario (como lo hacía tu código original)
        UUID sessionUuid = UUID.randomUUID();

        // Creamos el proveedor del menú
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return npc.getName(); // El título de la interfaz
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player playerEntity) {
                if (guiId == GUI_NPC_CHEST) {
                    if (npc instanceof LunaEntity luna) {
                        return new NpcChestContainer(windowId, luna, playerInv, sessionUuid);
                    }
                    return new NpcEquipmentContainer(windowId, npc, playerInv, sessionUuid);
                }

                if (guiId == GUI_NPC_EQUIPMENT && npc instanceof NpcInventoryEntity) {
                    return new NpcEquipmentContainer(windowId, npc, playerInv, sessionUuid);
                }

                return null;
            }
        };

        // NetworkHooks se encarga de todo: abre el contenedor en el servidor,
        // envía un paquete al cliente, y el cliente abre la Screen automáticamente.
        // Usamos el buffer extra para enviar el ID de la entidad y el UUID al cliente.
        NetworkHooks.openScreen(player, menuProvider, buf -> {
            buf.writeInt(npc.getId());
            buf.writeUUID(sessionUuid);
        });
    }
}