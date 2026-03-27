package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathOwnershipData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RequestRidingPacket - ported from bk.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER. Makes the sending player start riding the NPC that is
 * bound to them via {@link GalathOwnershipData}.
 *
 * Sequence:
 *  1. Look up the player's bound NPC UUID via {@code GalathOwnershipData.getOwnerOf(playerUUID)}.
 *  2. Find that NPC in the active NPC list.
 *  3. Call {@code player.startRiding(npc, true)}.
 *  4. Set the NPC's animation state to {@code CONTROLLED_FLIGHT}.
 *  5. Set NPC's target player.
 *  6. Give the NPC an upward impulse (+0.25 Y).
 *  7. Mark the chunk for re-broadcast.
 *
 * In 1.12.2:
 *   - {@code v.b(player)} - {@link GalathOwnershipData#getOwnerOf(java.util.UUID)}
 *   - {@code em.a(uuid)} - {@link BaseNpcEntity#getByMasterUUID(UUID)}
 *   - {@code entityPlayerMP.func_184205_a(entity, true)} - {@code player.startRiding(npc, true)}
 *   - {@code em.b(fp.CONTROLLED_FLIGHT)} - {@code npc.setAnimState(AnimState.CONTROLLED_FLIGHT)}
 *   - {@code em.a(player)} - {@code npc.setPlayerTarget(player)}
 *   - {@code em.field_70181_x = 0.25} - {@code npc.setDeltaMovement(x, 0.25, z)}
 *   - {@code world.func_175726_f(blockPos).func_76622_b(entity)} -
 *     {@code level.getChunkAt(npc.blockPosition()).setUnsaved(true)}
 */
public class RequestRidingPacket {

    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public RequestRidingPacket() {
        this.valid = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static RequestRidingPacket decode(FriendlyByteBuf buf) {
        return new RequestRidingPacket();
    }

    public void encode(FriendlyByteBuf buf) {
        // no fields
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @RequestRiding :(");
                return;
            }

            ServerPlayer player = ctx.getSender();
            if (player == null) {
                System.out.println("received an invalid message @RequestRiding :(");
                return;
            }

            UUID npcUUID = GalathOwnershipData.getOwnerOf(player.getUUID());
            if (npcUUID == null) return;

            BaseNpcEntity npc = BaseNpcEntity.getByMasterUUID(npcUUID);
            if (npc == null) return;

            // Start riding
            player.startRiding((Entity) npc, true);

            // Set flight animation
            npc.setAnimState(AnimState.CONTROLLED_FLIGHT);

            // Set target player
            npc.setPlayerTarget(player);

            // Launch upward
            npc.setDeltaMovement(npc.getDeltaMovement().x, 0.25, npc.getDeltaMovement().z);

            // Mark chunk dirty
            player.level().getChunkAt(npc.blockPosition()).setUnsaved(true);
        });
        ctx.setPacketHandled(true);
    }
}
