package com.trolmastercard.sexmod.handler;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * World event handler for bed-break protection and push-out-of-blocks cancellation.
 * Ported from ey.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Register on the FORGE event bus.
 */
public class BlockEventHandler {

    /** Search radius around a destroyed bed block. */
    static final int BED_CHECK_RADIUS = 3;

    // -- Bed break protection ------------------------------------------------

    /**
     * Cancels breaking a bed if any nearby NPC is currently using it (sex
     * session active), and sends the player a polite message.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof BedBlock)) return;

        BlockPos pos = event.getPos();
        var level = event.getLevel();

        double r = BED_CHECK_RADIUS;
        var aabb = new net.minecraft.world.phys.AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r, pos.getY() + r, pos.getZ() + r
        );

        if (!(level instanceof net.minecraft.world.level.Level worldLevel)) return;

        List<BaseNpcEntity> nearby = worldLevel.getEntitiesOfClass(BaseNpcEntity.class, aabb);
        boolean inUse = false;
        for (BaseNpcEntity npc : nearby) {
            if (!npc.isRemoved() && npc.entityData.get(BaseNpcEntity.FROZEN)) {
                inUse = true;
                break;
            }
        }

        if (!inUse) return;

        event.getPlayer().displayClientMessage(
                Component.literal("this bed is currently used by a girl.. pls don't disturb okay? ... you are kinda mean rn"),
                true
        );
        event.setCanceled(true);
    }

    // -- Push-out-of-blocks cancel (client only) ------------------------------

    /**
     * Prevents the vanilla push-out-of-blocks mechanic when the local player
     * has an NPC partner (prevents jitter during sex poses).
     *
     * In 1.20.1 there is no direct equivalent of
     * {@code PlayerSPPushOutOfBlocksEvent}; we use
     * {@link net.minecraftforge.client.event.ClientPlayerNetworkEvent} or
     * override movement processing.  As a safe approximation we hook
     * {@code LivingEntityUseItemEvent} - the actual push-cancel may need to be
     * done by overriding {@code EntityPlayerSP#pushOutOfBlocks} via Mixin.
     *
     * TODO: implement via Mixin on LocalPlayer#pushOutOfBlocks if needed.
     */
    @OnlyIn(Dist.CLIENT)
    public void onPushOutOfBlocks() {
        // See TODO above. Requires Mixin or reflection on LocalPlayer.
    }
}
