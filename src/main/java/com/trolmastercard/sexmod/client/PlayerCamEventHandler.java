package com.trolmastercard.sexmod.client;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

import java.util.ConcurrentModificationException;
import java.util.UUID;

/**
 * PlayerCamEventHandler - ported from l.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Registered on the Forge event bus. Handles three concerns:
 *
 * 1. {@link #onRenderPlayer}: Suppresses player rendering while they are
 *    involved in a sex animation (so only the NPC model is visible).
 *
 * 2. {@link #onRenderHand}: Suppresses first-person hand rendering for the
 *    same reason, and also when the player is in a PlayerKobold state that
 *    should hide hands.
 *
 * 3. {@link #onRenderTick}: Overrides the client player's camera position
 *    during "boy-cam" mode (the camera is placed at a specific bone of the
 *    NPC rather than at the player's eye position). The player's real position
 *    is saved before the tick and restored after rendering.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - RenderPlayerEvent.Pre still exists in Forge 1.20.1
 *   - RenderHandEvent still exists
 *   - TickEvent.RenderTickEvent still exists
 *   - em.ad() - BaseNpcEntity.getAllNpcs()
 *   - fp.hasPlayer - AnimState.hasPlayer
 *   - fp.useBoyCam - AnimState.useBoyCam
 *   - em.y() - npc.getAnimState()
 *   - em.ae() - npc.getTargetPlayerId()
 *   - ei.d(uuid) - PlayerKoboldEntity.getForPlayer(uuid)
 *   - ei.Q() - playerKobold.isSexModeActive()
 *   - b6.a(Vec3, Vec3, float) - MathUtil.lerpVec3(a, b, t)
 *   - em.b("boyCam") - npc.getBoneWorldPosition("boyCam")
 *   - em.o() - npc.getLastTickPosition()
 *   - minecraft.field_71474_y.field_74320_O != 0 - !firstPerson camera
 *   - field_70142_S/T/U - xOld/yOld/zOld
 *   - field_70165_t/u/v - getX/getY/getZ
 *   - entity.func_70107_b - entity.setPos
 */
public class PlayerCamEventHandler {

    /** Player's real position saved before boy-cam override, restored after render. */
    private Vec3 savedPos      = null;
    private Vec3 savedOldPos   = null;

    // -- 1. Hide player during sex ----------------------------------------------

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
                if (npc.isRemoved()) continue;
                UUID partnerId = npc.getTargetPlayerId();
                if (partnerId == null) continue;

                AnimState state = npc.getAnimState();
                if (state == AnimState.NULL) continue;

                if (state.hasPlayer) {
                    UUID renderingPlayer = event.getPlayer().getUUID();
                    if (partnerId.equals(renderingPlayer) ||
                        partnerId.equals(event.getPlayer().getGameProfile().getId())) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }

    // -- 2. Hide hand during sex ------------------------------------------------

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) return;

        // PlayerKobold check
        try {
            PlayerKoboldEntity pk = PlayerKoboldEntity.getForPlayer(localPlayer);
            if (pk != null && pk.isSexModeActive()) {
                event.setCanceled(true);
                return;
            }
        } catch (ConcurrentModificationException ignored) {}

        // NPC sex-animation check
        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
                UUID partnerId = npc.getTargetPlayerId();
                AnimState state = npc.getAnimState();
                if (npc.isRemoved() || partnerId == null || state == null) continue;

                if (state.hasPlayer) {
                    UUID localId = localPlayer.getGameProfile().getId();
                    if (partnerId.equals(localId) || partnerId.equals(localPlayer.getUUID())) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }

    // -- 3. Boy-cam camera position override ------------------------------------

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Restore saved position at end of render tick
        if (event.phase == TickEvent.Phase.END) {
            if (savedPos != null) {
                mc.player.setPos(savedPos.x, savedPos.y, savedPos.z);
                mc.player.xOld = savedOldPos.x;
                mc.player.yOld = savedOldPos.y;
                mc.player.zOld = savedOldPos.z;
                savedPos    = null;
                savedOldPos = null;
            }
            return;
        }

        // Only applies in first-person view
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return;

        // Find the NPC whose boy-cam should override this player's camera
        BaseNpcEntity targetNpc = BaseNpcEntity.getNpcForPlayer(mc.player.getUUID(), false);
        if (targetNpc == null) return;

        AnimState state = targetNpc.getAnimState();
        if (state == null || !state.useBoyCam) return;

        if (targetNpc.isSexModeActive()) return;

        // Save real position
        savedPos    = mc.player.position();
        savedOldPos = new Vec3(mc.player.xOld, mc.player.yOld, mc.player.zOld);

        // Determine the bone's world position, interpolated if NPC is moving
        Vec3 boneCurrent = targetNpc.getBoneWorldPosition("boyCam");
        Vec3 bonePos;
        if (targetNpc.isSexModeActive()) {
            bonePos = boneCurrent;
        } else {
            Vec3 boneOld = targetNpc.getLastTickBonePosition("boyCam");
            bonePos = MathUtil.lerpVec3(boneOld, boneCurrent, event.renderTickTime);
        }

        // Move the client player to the bone position so the camera follows it
        mc.player.setPos(bonePos.x, bonePos.y - mc.player.getEyeHeight(), bonePos.z);
        mc.player.xOld = bonePos.x;
        mc.player.yOld = bonePos.y - mc.player.getEyeHeight();
        mc.player.zOld = bonePos.z;
    }
}
