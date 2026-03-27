package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.AnimState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

import static com.trolmastercard.sexmod.network.ModNetwork.CHANNEL;

/**
 * ResetNpcPacket - ported from s.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER by the player to reset all NPCs with a given master UUID.
 *
 * {@code onlyResetPlayer} flag:
 *   false - fully reset each matching NPC (clear sex state, teleport above ground)
 *   true  - only reset the player's own state, leave NPCs untouched
 *
 * Also contains two static utility helpers used from other packet handlers:
 *   {@link #resetNpc(BaseNpcEntity)}     - full NPC state reset
 *   {@link #resetPlayer(ServerPlayer)}   - player state reset (un-spectate, un-freeze)
 */
public class ResetNpcPacket {

    private final UUID    masterUUID;
    private final boolean onlyResetPlayer;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public ResetNpcPacket(UUID masterUUID) {
        this(masterUUID, false);
    }

    public ResetNpcPacket(UUID masterUUID, boolean onlyResetPlayer) {
        this.masterUUID       = masterUUID;
        this.onlyResetPlayer  = onlyResetPlayer;
        this.valid            = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static ResetNpcPacket decode(FriendlyByteBuf buf) {
        UUID    uuid  = UUID.fromString(buf.readUtf());
        boolean flag  = buf.readBoolean();
        return new ResetNpcPacket(uuid, flag);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(masterUUID.toString());
        buf.writeBoolean(onlyResetPlayer);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @ResetNpc :(");
                return;
            }

            // Collect all NPCs whose master UUID matches
            ArrayList<BaseNpcEntity> matches = BaseNpcEntity.getAllWithMaster(masterUUID);

            for (BaseNpcEntity npc : matches) {
                // Skip client-side worlds
                if (npc.level().isClientSide()) continue;

                // Reset the sex partner's player state if applicable
                if (npc.getSexTarget() != null) {
                    ServerPlayer partner = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(npc.getSexTarget());
                    resetPlayer(partner);
                }

                // If onlyResetPlayer, skip the NPC reset
                if (onlyResetPlayer) continue;

                resetNpc(npc);
            }
        });
        ctx.setPacketHandled(true);
    }

    // =========================================================================
    //  Static utilities (used by other handlers and the logout event)
    // =========================================================================

    /**
     * Fully resets an NPC after a sex sequence:
     *  - Clears sex partner, active flag, animation state
     *  - Un-freezes physics
     *  - Teleports upward until the NPC is not inside a solid block
     *
     * Equivalent to the original static {@code a(em)} method.
     */
    public static void resetNpc(BaseNpcEntity npc) {
        // If it's a PlayerKoboldEntity, also reset the bound player
        if (npc instanceof PlayerKoboldEntity pk) {
            UUID playerUUID = pk.getPlayerUUID();
            if (playerUUID != null) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    player.getAbilities().flying    = false;
                    player.setNoGravity(false);
                    player.setSpectator(false);
                    player.onUpdateAbilities();

                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new IsGirlPacket(true));

                    npc.entityData.set(BaseNpcEntity.MODEL_INDEX, 1);
                }
            }
        }

        npc.setActive(false);
        npc.setAnimState(AnimState.NULL);
        npc.setSexTarget(null);
        npc.bedPos = null;
        npc.setNoGravity(false);
        npc.setInvulnerable(false);

        // Move upward until above solid ground
        Level level = npc.level();
        Vec3 pos = npc.position();
        while (level.getBlockState(BlockPos.containing(pos)).getBlock() != Blocks.AIR) {
            pos = pos.add(0, 1, 0);
        }
        npc.teleportTo(pos.x, pos.y, pos.z);
    }

    /**
     * Resets a player's game-mode and physics flags after a sex sequence.
     * Equivalent to the original static {@code a(EntityPlayerMP)} method.
     */
    public static void resetPlayer(ServerPlayer player) {
        if (player == null) return;

        Level level = player.level();
        Vec3 pos = player.position();
        while (level.getBlockState(BlockPos.containing(pos)).getBlock() != Blocks.AIR) {
            pos = pos.add(0, 1, 0);
        }
        player.teleportTo(pos.x, pos.y, pos.z);

        player.setGodMode(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new IsGirlPacket(true));
    }
}
