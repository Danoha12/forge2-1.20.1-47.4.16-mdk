package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.entity.AnimState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

/**
 * SummonAlliePacket - ported from bg.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT-SERVER. Spawns AllieEntity 2 blocks in front of the sending player.
 * Picks SUMMON_SAND, SUMMON, or SUMMON_NORMAL animation based on ground block.
 *
 * In 1.12.2:
 *   player.func_174791_d() - player.position()
 *   player.field_70759_as (yaw) - player.getYRot()
 *   Blocks.field_150354_m (sand) - Blocks.SAND
 *   ev.b(fp.X) - allie.setAnimState(AnimState.X)
 *   ev.f() - allie.isFuta()
 *   ev.field_70145_X = true - allie.noPhysics = true
 */
public class SummonAlliePacket {

    private boolean valid;

    public SummonAlliePacket() { valid = true; }

    public static SummonAlliePacket decode(FriendlyByteBuf buf) { return new SummonAlliePacket(); }
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (!valid || player == null) {
                System.out.println("received an invalid message @SummonAllie :(");
                return;
            }
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                float yaw = player.getYRot();
                double sinYaw = -Math.sin(Math.toRadians(yaw));
                double cosYaw =  Math.cos(Math.toRadians(yaw));
                Vec3 spawnPos = player.position().add(sinYaw * 2.0, 0.0, cosYaw * 2.0);

                AllieEntity allie = new AllieEntity(player.level(), player.getMainHandItem());
                allie.setMasterUUID(player.getUUID());
                allie.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                allie.setYRot(yaw + 180.0F);
                allie.yHeadRot = yaw + 180.0F;
                allie.noPhysics = true;
                allie.setPersistenceRequired();
                player.level().addFreshEntity(allie);

                BlockPos below = allie.blockPosition().below();
                if (player.level().getBlockState(below).getBlock() == Blocks.SAND) {
                    allie.setAnimState(AnimState.SUMMON_SAND);
                } else {
                    allie.setAnimState(allie.isFuta() ? AnimState.SUMMON : AnimState.SUMMON_NORMAL);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
