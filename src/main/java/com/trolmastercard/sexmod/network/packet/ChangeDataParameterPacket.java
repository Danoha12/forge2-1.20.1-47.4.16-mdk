package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ChangeDataParameterPacket - ported from n.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER to update a named data parameter on a specific NPC.
 *
 * Supported parameter names (replaces the original string-switch):
 *   "pregnant"            - fn.U (pregnancy counter)
 *   "currentModel"        - em.D (model index)
 *   "currentAction"       - em.h (animation follow-up string)
 *   "animationFollowUp"   - em.h (animation follow-up string)
 *   "playerSheHasSexWith" - UUID of sex partner
 *   "targetPos"           - Vec3 encoded as "xfyfzf"
 *   "master"              - em.v (master UUID string)
 *   "walk speed"          - em.a (walk speed string)
 *   "shouldbeattargetpos" - em.G (boolean)
 */
public class ChangeDataParameterPacket {

    private final UUID  npcUUID;
    private final String paramName;
    private final String value;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public ChangeDataParameterPacket(UUID npcUUID, String paramName, String value) {
        this.npcUUID   = npcUUID;
        this.paramName = paramName;
        this.value     = value;
        this.valid     = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static ChangeDataParameterPacket decode(FriendlyByteBuf buf) {
        UUID   uuid  = UUID.fromString(buf.readUtf());
        String param = buf.readUtf();
        String val   = buf.readUtf();
        return new ChangeDataParameterPacket(uuid, param, val);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcUUID.toString());
        buf.writeUtf(paramName);
        buf.writeUtf(value == null ? "null" : value);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @ChangeDataParameter :(");
                return;
            }

            BaseNpcEntity npc = BaseNpcEntity.getById(npcUUID);
            if (npc == null) return;

            switch (paramName) {
                case "pregnant" ->
                    npc.entityData.set(PlayerKoboldEntity.PREGNANT, Integer.parseInt(value));

                case "currentModel" ->
                    npc.entityData.set(BaseNpcEntity.MODEL_INDEX, Integer.parseInt(value));

                case "currentAction" -> {
                    AnimState requested = AnimState.valueOf(value);
                    // Guard: don't override an active non-NULL state with ATTACK
                    if (requested == AnimState.ATTACK && npc.getAnimState() != AnimState.NULL) break;
                    npc.setAnimState(requested);
                }

                case "animationFollowUp" ->
                    npc.entityData.set(BaseNpcEntity.ANIMATION_FOLLOW_UP, value);

                case "playerSheHasSexWith" -> {
                    if ("null".equals(value)) npc.setSexTarget(null);
                    else                      npc.setSexTarget(UUID.fromString(value));
                }

                case "targetPos" -> {
                    String[] parts = value.split("f");
                    Vec3 pos = new Vec3(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]));
                    npc.setTargetPos(pos);
                }

                case "master" ->
                    npc.entityData.set(BaseNpcEntity.MASTER_UUID, value);

                case "walk speed" ->
                    npc.entityData.set(BaseNpcEntity.WALK_SPEED, value);

                case "shouldbeattargetpos" ->
                    npc.entityData.set(BaseNpcEntity.SHOULD_BE_AT_TARGET_POS,
                        Boolean.parseBoolean(value));
            }
        });
        ctx.setPacketHandled(true);
    }
}
