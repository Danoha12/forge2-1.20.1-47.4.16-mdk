package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * SummonAlliePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Invoca a AllieEntity 2 bloques frente al jugador que envía el paquete.
 * * Selecciona la animación de aparición dependiendo de si pisa arena u otro bloque.
 */
public class SummonAlliePacket {

    // Paquete vacío (Ping)
    public SummonAlliePacket() {}

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(SummonAlliePacket msg, FriendlyByteBuf buf) {}

    public static SummonAlliePacket decode(FriendlyByteBuf buf) {
        return new SummonAlliePacket();
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(SummonAlliePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();

            // Calcular posición 2 bloques frente al jugador (Matemática de Yaw de Minecraft)
            float yaw = player.getYRot();
            double sinYaw = -Math.sin(Math.toRadians(yaw));
            double cosYaw = Math.cos(Math.toRadians(yaw));
            Vec3 spawnPos = player.position().add(sinYaw * 2.0D, 0.0D, cosYaw * 2.0D);

            // 🚨 Nota: Asegúrate de que AllieEntity reciba correctamente el Level en su constructor
            AllieEntity allie = new AllieEntity(level, player.getMainHandItem());
            allie.setMasterUUID(player.getUUID());
            allie.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            // Rotar a la entidad para que mire directamente al jugador (desfase de 180 grados)
            allie.setYRot(yaw + 180.0F);
            allie.setYHeadRot(yaw + 180.0F); // Setter oficial para la cabeza

            allie.noPhysics = true; // Desactiva colisiones momentáneamente durante la animación
            allie.setPersistenceRequired(); // Evita que despawnee mágicamente

            level.addFreshEntity(allie);

            // Verificar el bloque debajo para elegir la animación
            BlockPos below = allie.blockPosition().below();

            // 1.20.1: is(Block) es la forma moderna recomendada sobre ==
            if (level.getBlockState(below).is(Blocks.SAND)) {
                allie.setAnimState(AnimState.SUMMON_SAND);
            } else {
                allie.setAnimState(allie.isFuta() ? AnimState.SUMMON : AnimState.SUMMON_NORMAL);
            }
        });

        ctx.setPacketHandled(true);
    }
}