package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client - Server packet that sets a custom model code string on an NPC
 * (or the sending player's PlayerKoboldEntity) plus optional girl-specific data.
 *
 * Obfuscated name: fw
 */
public class CustomizeNpcPacket {

    private final String     modelCode;
    private final UUID       npcUUID;
    private final List<Integer> specificData;

    public CustomizeNpcPacket(String modelCode, UUID npcUUID) {
        this.modelCode    = modelCode;
        this.npcUUID      = npcUUID;
        this.specificData = new ArrayList<>();
    }

    public CustomizeNpcPacket(String modelCode, UUID npcUUID, List<Integer> specificData) {
        this.modelCode    = modelCode;
        this.npcUUID      = npcUUID;
        this.specificData = specificData;
    }

    // -- Codec -----------------------------------------------------------------

    public static CustomizeNpcPacket decode(FriendlyByteBuf buf) {
        String code = buf.readUtf();
        UUID   uuid = UUID.fromString(buf.readUtf());
        int    cnt  = buf.readInt();
        List<Integer> data = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i++) data.add(buf.readInt());
        return new CustomizeNpcPacket(code, uuid, data);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.modelCode);
        buf.writeUtf(this.npcUUID.toString());
        buf.writeInt(this.specificData.size());
        for (int v : this.specificData) buf.writeInt(v);
    }

    // -- Handler ---------------------------------------------------------------

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("received an invalid message @UploadModelString :(");
                return;
            }

            BaseNpcEntity npc = BaseNpcEntity.getByUUID(this.npcUUID);
            if (npc == null) {
                System.out.println("received an invalid message @UploadModelString :(");
                return;
            }

            boolean hasSpecific = !this.specificData.isEmpty();
            boolean specificValid = false;

            if (hasSpecific) {
                specificValid = validateSpecificData(npc, this.specificData);
                if (specificValid) npc.applySpecificData(this.specificData);
            }

            // PlayerKoboldEntity: persist to player NBT
            if (npc instanceof PlayerKoboldEntity pke) {
                CompoundTag playerData = sender.getPersistentData();
                NpcType type = NpcType.fromEntity(npc);
                if (type == null) return;

                playerData.putString("sexmod:CustomModel" + type, this.modelCode);
                if (hasSpecific && specificValid) {
                    playerData.putString("sexmod:GirlSpecific" + type,
                            npc.encodeSpecificData(this.specificData));
                }
            } else {
                npc.setCustomModelCode(this.modelCode);
            }
        });
        ctx.setPacketHandled(true);
    }

    /** Validates that the proposed specific data does not exceed the NPC's maxima. */
    private static boolean validateSpecificData(BaseNpcEntity npc, List<Integer> proposed) {
        List<Integer> maxima = npc.getSpecificDataMaxima();
        for (int i = 0; i < maxima.size(); i++) {
            if (i >= proposed.size()) return false;
            if (maxima.get(i) <= proposed.get(i)) return false;
        }
        return true;
    }
}
