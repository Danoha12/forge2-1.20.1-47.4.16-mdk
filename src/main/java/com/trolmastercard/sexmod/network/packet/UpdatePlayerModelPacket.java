package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Constructor;
import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * UpdatePlayerModelPacket - ported from b_.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER. Requests that the server:
 *  1. Removes any existing {@link PlayerKoboldEntity} bound to the sending player.
 *  2. If a non-null {@link NpcType} is provided, spawns a new
 *     {@link PlayerKoboldEntity} subclass (determined by {@code NpcType.playerClass})
 *     69 blocks above the player (off-screen staging area).
 *
 * Passing {@code null} for {@code npcType} is the "player" case - removes
 * the bound NPC without spawning a replacement.
 *
 * The spawned entity has:
 *   - persistence required
 *   - {@code noPhysics = true} (field_145_X)
 *   - velocity zeroed
 *   - positioned at player.x, player.y + 69, player.z
 *   - immediately calls {@code B()} (- {@code initDefaultState()}) to set
 *     up default sex-animation pose
 *
 * In 1.12.2:
 *   - {@code ByteBufUtils.readUTF8String/writeUTF8String} - {@code buf.readUtf()/writeUtf()}
 *   - {@code IMessage/IMessageHandler} - FriendlyByteBuf + handle()
 *   - {@code FMLCommonHandler...getMinecraftServerInstance()} - {@code ServerLifecycleHooks.getCurrentServer()}
 *   - {@code Guava Optional.absent()} - {@code java.util.Optional.empty()}
 *   - {@code world.func_72838_d(entity)} - {@code level.addFreshEntity(entity)}
 *   - {@code world.func_72900_e(entity)} - {@code entity.discard()}
 */
public class UpdatePlayerModelPacket {

    /** Null means "player" mode - remove bound NPC without replacing. */
    private final NpcType npcType;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public UpdatePlayerModelPacket(NpcType npcType) {
        this.npcType = npcType;
        this.valid   = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static UpdatePlayerModelPacket decode(FriendlyByteBuf buf) {
        String str = buf.readUtf();
        NpcType type = "player".equals(str) ? null : NpcType.valueOf(str);
        return new UpdatePlayerModelPacket(type);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcType == null ? "player" : npcType.toString());
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @UpdatePlayerModel :(");
                return;
            }

            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid message @UpdatePlayerModel :(");
                return;
            }

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                var level = sender.serverLevel();
                UUID playerUUID = sender.getUUID();

                // 1. Remove existing PlayerKoboldEntity bound to this player
                PlayerKoboldEntity existing = PlayerKoboldEntity.getByPlayerUUID(playerUUID);
                if (existing != null) {
                    // Also remove any ClothingOverlay entities sharing the same UUID group
                    try {
                        for (var npc : com.trolmastercard.sexmod.entity.BaseNpcEntity.getAllActive()) {
                            if (npc.level().isClientSide()) continue;
                            if (npc.getMasterUUID().equals(existing.getMasterUUID())) {
                                npc.discard();
                            }
                        }
                    } catch (ConcurrentModificationException ignored) {}

                    existing.dropAllContents();
                    PlayerKoboldEntity.removeFromRegistry(playerUUID);
                    com.trolmastercard.sexmod.entity.BaseNpcEntity.getAllActive().remove(existing);
                    existing.clearMasterUUID();
                }

                // 2. Optionally spawn a new PlayerKoboldEntity
                if (npcType == null) return;

                try {
                    Constructor<? extends PlayerKoboldEntity> ctor =
                        npcType.playerClass.getConstructor(
                            net.minecraft.world.level.Level.class,
                            UUID.class);

                    PlayerKoboldEntity newEntity = ctor.newInstance(level, playerUUID);
                    newEntity.setPersistenceRequired();
                    newEntity.noPhysics = true;
                    newEntity.setDeltaMovement(0, 0, 0);
                    newEntity.setPos(sender.getX(), sender.getY() + 69.0, sender.getZ());
                    level.addFreshEntity(newEntity);
                    newEntity.initDefaultState();     // B()  initDefaultState()

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
