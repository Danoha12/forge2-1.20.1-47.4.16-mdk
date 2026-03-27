package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathSexEntity; // Mantenemos el nombre para que conecte con JennyEntity
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * StartGalathSexPacket - Paquete de Red de Interacción.
 * Portado a 1.20.1.
 * * Enviado CLIENTE -> SERVIDOR.
 * * Instruye a todas las instancias NPC del lado del servidor vinculadas al jugador
 * a iniciar su secuencia de interacción (ej. ir a la cama), si implementan la interfaz.
 */
public class StartGalathSexPacket {

    private final UUID    masterUUID;
    private final boolean valid;

    // =========================================================================
    //  Constructores
    // =========================================================================

    public StartGalathSexPacket(UUID masterUUID) {
        this.masterUUID = masterUUID;
        this.valid      = true;
    }

    // =========================================================================
    //  Codificador / Decodificador (Codec)
    // =========================================================================

    public static StartGalathSexPacket decode(FriendlyByteBuf buf) {
        UUID uuid = UUID.fromString(buf.readUtf());
        return new StartGalathSexPacket(uuid);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
    }

    // =========================================================================
    //  Manejador (Handler)
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                // Limpiamos el log de la consola para que sea profesional
                System.out.println("[Tribu/Network] Mensaje de red inválido @StartInteraction :(");
                return;
            }

            // Ejecutamos la lógica en el hilo principal del servidor
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                // Buscamos a todas las NPCs que le pertenecen a este jugador
                ArrayList<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(masterUUID);

                for (BaseNpcEntity npc : npcs) {
                    if (npc.level().isClientSide()) continue;

                    // Si el NPC implementa nuestra interfaz de interacción (Como JennyEntity)
                    if (npc instanceof GalathSexEntity interactionNpc) {
                        // Disparamos el evento (que en Jenny pone atBed = true)
                        interactionNpc.onGalathSexStart();
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}