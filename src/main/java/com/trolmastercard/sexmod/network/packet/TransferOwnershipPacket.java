package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * TransferOwnershipPacket (dc) - CLIENT-SERVER.
 *
 * For every BaseNpcEntity matching {@code npcUuid}:
 *  1. Removes KoboldFollowOwnerGoal and WaterAvoidingRandomStrollGoal
 *  2. Stops navigation and zeros velocity
 *  3. Sets new owner to {@code newOwnerUuid} (or re-uses existing if already set)
 *  4. Optionally teleports home to current position (setHome=true)
 *  5. Optionally fires TickableCallback.onOwnerSet() (triggerCallback=true)
 */
public class TransferOwnershipPacket {

    private final UUID npcUuid;
    private final UUID newOwnerUuid;   // may be null  keep existing
    private final boolean setHome;
    private final boolean triggerCallback;

    public TransferOwnershipPacket(UUID npcUuid, UUID newOwnerUuid,
                                    boolean setHome, boolean triggerCallback) {
        this.npcUuid        = npcUuid;
        this.newOwnerUuid   = newOwnerUuid;
        this.setHome        = setHome;
        this.triggerCallback = triggerCallback;
    }

    // -- Codec -----------------------------------------------------------------

    public static TransferOwnershipPacket decode(FriendlyByteBuf buf) {
        UUID npc   = UUID.fromString(buf.readUtf());
        boolean sh = buf.readBoolean();
        boolean tc = buf.readBoolean();
        String ownerStr = buf.readUtf();
        UUID owner = "null".equals(ownerStr) ? null : UUID.fromString(ownerStr);
        return new TransferOwnershipPacket(npc, owner, sh, tc);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcUuid.toString());
        buf.writeBoolean(setHome);
        buf.writeBoolean(triggerCallback);
        buf.writeUtf(newOwnerUuid == null ? "null" : newOwnerUuid.toString());
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> applyToEntities(npcUuid, newOwnerUuid, setHome, triggerCallback));
        context.setPacketHandled(true);
    }

    // -- Static application logic ----------------------------------------------

    public static void applyToEntities(UUID npcUuid, UUID newOwnerUuid,
                                        boolean setHome, boolean triggerCallback) {
        ArrayList<BaseNpcEntity> targets;
        try {
            targets = BaseNpcEntity.getAllByMasterUUID(npcUuid);
        } catch (ConcurrentModificationException e) {
            return;
        }

        for (BaseNpcEntity npc : targets) {
            try {
                if (npc.level().isClientSide()) continue;

                // Remove follow/wander goals if it's a proper NPC type
                if (npc instanceof PlayerKoboldEntity || npc instanceof NpcInventoryEntity
                        || npc instanceof BaseNpcEntity) {
                    npc.goalSelector.removeGoal(npc.followOwnerGoal);
                    npc.goalSelector.removeGoal(npc.wanderGoal);
                }

                // Stop navigation, zero velocity
                npc.getNavigation().stop();
                npc.setDeltaMovement(0, 0, 0);

                // Set owner: if no existing owner, use provided uuid
                if (npc.getMasterUUID() == null) {
                    npc.setMasterUUID(newOwnerUuid);
                }

                // Optionally update home position to current pos
                if (setHome) {
                    npc.setHomePos(npc.getHomePos());
                }

                // Re-bind owner field
                npc.setMasterUUID(npc.getMasterUUID());

                if (!triggerCallback) continue;

                // Fire TickableCallback if implemented
                if (npc instanceof TickableCallback cb) {
                    cb.onOwnerSet();
                }
            } catch (ConcurrentModificationException e) {
                // skip
            } catch (NullPointerException e) {
                // skip
            }
        }
    }
}
