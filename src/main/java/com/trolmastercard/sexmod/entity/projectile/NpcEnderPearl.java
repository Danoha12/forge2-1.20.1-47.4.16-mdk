package com.trolmastercard.sexmod.entity.projectile; // Ajusta el paquete a tu estructura

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModEntityRegistry; // Asumiendo que aquí registraste la perla
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NpcEnderPearl — Portado a 1.20.1.
 * * Perla de Ender personalizada usada por los NPCs para teletransportarse.
 */
public class NpcEnderPearl extends ThrownEnderpearl {

    public NpcEnderPearl(Level level) {
        super(ModEntityRegistry.NPC_ENDER_PEARL.get(), level);
    }

    public NpcEnderPearl(Level level, LivingEntity shooter) {
        super(ModEntityRegistry.NPC_ENDER_PEARL.get(), shooter, level);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result); // Importante llamar al super para físicas base de proyectiles

        LivingEntity owner = (LivingEntity) this.getOwner();

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
            BlockEntity be = this.level().getBlockEntity(blockPos);

            if (be instanceof TheEndGatewayBlockEntity gateway) {
                if (owner != null) {
                    if (owner instanceof ServerPlayer sp) {
                        net.minecraft.advancements.CriteriaTriggers.ENTER_BLOCK.trigger(sp, this.level().getBlockState(blockPos));
                    }
                    // En 1.20.1 el método de teletransporte de Gateway cambió a estático,
                    // lo adaptamos a la firma moderna:
                    TheEndGatewayBlockEntity.teleportEntity(this.level(), blockPos, this.level().getBlockState(blockPos), owner, gateway);
                    this.discard();
                    return;
                }
                TheEndGatewayBlockEntity.teleportEntity(this.level(), blockPos, this.level().getBlockState(blockPos), this, gateway);
                return;
            }
        }

        // Spawn portal particles (Solo Cliente)
        if (this.level().isClientSide()) {
            for (int i = 0; i < 32; i++) {
                this.level().addParticle(ParticleTypes.PORTAL,
                        this.getX(), this.getY() + this.random.nextDouble() * 2.0D, this.getZ(),
                        this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
            }
        }

        // Lógica de Teletransporte (Solo Servidor)
        if (!this.level().isClientSide()) {
            if (owner instanceof BaseNpcEntity npc) {
                // Asumiendo que homePosition es ahora accesible (ej. getHomePosition())
                // Si tienes la variable pública, puedes dejar npc.homePosition
                if (npc.homePosition != null && Math.sqrt(npc.homePosition.distSqr(this.blockPosition())) < 5.0D) {

                    // ¡El evento moderno de Forge 1.20.1!
                    EntityTeleportEvent.EnderPearl event = new EntityTeleportEvent.EnderPearl(npc, this.getX(), this.getY(), this.getZ(), this, 5.0F);

                    if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
                        if (owner.isSleeping()) owner.stopSleeping();
                        owner.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
                        owner.resetFallDistance(); // 1.20.1: fallDistance = 0 ya no se recomienda directamente
                    }
                }
            }
            // SIEMPRE descartar el proyectil al chocar en el servidor
            this.discard();
        }
    }

    // ── Event handler inner class (Auto-Registrado) ─────────────────────────

    @Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventHandler {

        @SubscribeEvent
        public static void onEnderTeleport(EntityTeleportEvent.EnderPearl event) {
            // Este evento intercepta TANTO las perlas de los jugadores como las de los NPCs.
            // Si es un NPC de nuestro mod, reseteamos su estado.
            if (event.getEntity() instanceof BaseNpcEntity npc) {
                npc.sexPartnerUUID = null; // O npc.setSexPartner(null)
                npc.setAnimState(AnimState.NULL);

                // Actualizar el EntityData tracker para que el cliente se entere
                npc.getEntityData().set(npc.isSexActive, false); // Asegúrate de que isSexActive sea el EntityDataAccessor correcto
                npc.resetSexState();
            }
        }
    }
}