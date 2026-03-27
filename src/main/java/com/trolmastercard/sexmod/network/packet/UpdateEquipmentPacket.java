package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client - Server packet that updates the equipment ItemStackHandler of all
 * NpcInventoryEntity instances owned by the given UUID.
 * Obfuscated name: ft
 */
public class UpdateEquipmentPacket {

    private final UUID ownerUUID;
    private final CompoundTag equipmentTag;

    public UpdateEquipmentPacket(UUID ownerUUID, CompoundTag equipmentTag) {
        this.ownerUUID    = ownerUUID;
        this.equipmentTag = equipmentTag;
    }

    // -- Codec -----------------------------------------------------------------

    public static UpdateEquipmentPacket decode(FriendlyByteBuf buf) {
        UUID owner = UUID.fromString(buf.readUtf());
        CompoundTag tag = buf.readNbt();
        return new UpdateEquipmentPacket(owner, tag);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.ownerUUID.toString());
        buf.writeNbt(this.equipmentTag);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ArrayList<BaseNpcEntity> owned = BaseNpcEntity.getAllByOwnerUUID(this.ownerUUID);
            for (BaseNpcEntity npc : owned) {
                if (npc instanceof NpcInventoryEntity inv) {
                    inv.equipmentHandler.deserializeNBT(this.equipmentTag);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
