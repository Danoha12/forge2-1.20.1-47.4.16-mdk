package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModEntityRegistry;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NpcEnderPearl - ported from ho.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A custom ThrownEnderpearl used by NPCs. On impact it teleports the owning
 * NPC (BaseNpcEntity) rather than a player. Fires EnderTeleportEvent so
 * other systems can react.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityEnderPearl - ThrownEnderpearl
 *   - func_70184_a(RayTraceResult) - onHit(HitResult)
 *   - func_85052_h() - getOwner()
 *   - RayTraceResult.Type.BLOCK - HitResult.Type.BLOCK
 *   - TileEntityEndGateway - TheEndGatewayBlockEntity
 *   - CriteriaTriggers.field_192124_d - CriteriaTriggers.ENTER_BLOCK
 *   - tileEntity.func_184306_a(entity) - gateway.teleportEntity(entity)
 *   - EnumParticleTypes.PORTAL - ParticleTypes.PORTAL
 *   - func_175688_a - level.addParticle(...)
 *   - entityLivingBase.func_70634_a - entity.teleportTo(x,y,z)
 *   - func_184218_aH() - isSleeping()
 *   - func_184210_p() - stopSleeping()
 *   - em - BaseNpcEntity; fp.NULL - AnimState.NULL
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
        LivingEntity owner = (LivingEntity) getOwner();

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
            BlockEntity be = this.level.getBlockEntity(blockPos);
            if (be instanceof TheEndGatewayBlockEntity gateway) {
                if (owner != null) {
                    if (owner instanceof ServerPlayer sp) {
                        CriteriaTriggers.ENTER_BLOCK.trigger(sp, this.level.getBlockState(blockPos));
                    }
                    gateway.teleportEntity(owner);
                    discard();
                    return;
                }
                gateway.teleportEntity(this);
                return;
            }
        }

        // Spawn portal particles
        for (int i = 0; i < 32; i++) {
            this.level.addParticle(ParticleTypes.PORTAL,
                    getX(), getY() + random.nextDouble() * 2.0D, getZ(),
                    random.nextGaussian(), 0.0D, random.nextGaussian());
        }

        if (!this.level.isClientSide() && owner != null) {
            BaseNpcEntity npc = (BaseNpcEntity) owner;
            if (npc.homePosition.distanceTo(position()) < 5.0D) {
                EnderTeleportEvent event = new EnderTeleportEvent(owner, getX(), getY(), getZ(), 5.0F);
                if (!MinecraftForge.EVENT_BUS.post(event)) {
                    if (owner.isSleeping()) owner.stopSleeping();
                    owner.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
                    owner.fallDistance = 0.0F;
                }
            }
            discard();
        }
    }

    // -- Event handler inner class -----------------------------------------------

    public static class EventHandler {
        @SubscribeEvent
        public void onEnderTeleport(EnderTeleportEvent event) {
            if (event.getEntityLiving() instanceof BaseNpcEntity npc) {
                npc.sexPartnerUUID = null;
                npc.setAnimState(AnimState.NULL);
                npc.getEntityData().set(npc.isSexActive, false);
                npc.resetSexState();
            }
        }
    }
}
