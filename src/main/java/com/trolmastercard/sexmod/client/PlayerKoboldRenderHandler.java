package com.trolmastercard.sexmod.client;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * PlayerKoboldRenderHandler - ported from e_.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Registered on the Forge event bus. When a {@link PlayerKoboldEntity} exists for the
 * rendering player, the player's render is cancelled and the PlayerKobold entity is
 * rendered in their place (at the player's exact position).
 *
 * Before rendering the PlayerKobold we copy the player's transform data onto the entity
 * so it appears where the player is standing.
 *
 * Tracks saved camera state ({@code b}, {@code d}) and the active PlayerKobold ({@code a})
 * for the camera-angle override used in first-person view.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - RenderPlayerEvent.Pre - RenderPlayerEvent.Pre (same in Forge 1.20.1)
 *   - EntityViewRenderEvent - ViewportEvent.ComputeCameraAngles / ViewportEvent.CameraSetup
 *   - RenderWorldLastEvent - RenderLevelStageEvent
 *   - RenderManager - EntityRenderDispatcher
 *   - ei.C() - PlayerKoboldEntity.cleanup()
 *   - ei.d(uuid) - PlayerKoboldEntity.getForPlayer(uuid)
 *   - paramei.c(player) - kobold.mirrorPlayer(player) - copies player transform to kobold
 *   - paramei.E() - kobold.isRenderable()
 *   - paramEntityPlayer.func_98034_c(localPlayer) - player.isLocalPlayer / getGameProfile().equals
 *   - mc.func_175598_ae() - mc.getEntityRenderDispatcher()
 *   - renderManager.func_147936_a(entity, x, y, z, yaw, partialTick) -
 *       dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, bufferSource, light)
 *   - field_70177_z / field_70758_at / etc. - yRot / yRotO / xRot / etc.
 *   - Vec3d - Vec3
 *   - b6.a(from, to, t) - MathUtil.lerpVec3(from, to, t)
 *   - Vector2f - no direct equivalent in 1.20.1; use float[] or Vec2
 *   - c = 1.2345679F (sentinel partial tick used to avoid recursive rendering)
 */
@OnlyIn(Dist.CLIENT)
public class PlayerKoboldRenderHandler {

    /** Magic sentinel partial-tick value that skips this handler (avoids infinite recursion). */
    public static final float SENTINEL_PARTIAL = 1.2345679F;

    Vec3 savedCamPos   = null;
    Vec3 savedCamPrev  = null;
    PlayerKoboldEntity activeKobold = null;
    boolean            active       = false;

    // -- RenderPlayerEvent.Pre --------------------------------------------------

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getPartialTick() == SENTINEL_PARTIAL) return;

        PlayerKoboldEntity.cleanup();
        Player player = event.getEntity();
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player.getUUID());
        if (kobold == null) return;

        event.setCanceled(true);

        // Mirror player transform onto the kobold
        kobold.mirrorPlayer(player);

        // Determine if we are rendering the local player in first-person
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null &&
                mc.player.getGameProfile().equals(((net.minecraft.world.entity.player.Player) player).getGameProfile());

        if (isLocalPlayer && !kobold.isRenderable()) return;

        // Render the kobold using the entity render dispatcher
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.render(
                kobold,
                event.getX(), event.getY(), event.getZ(),
                event.getEntity().getYRot(),
                SENTINEL_PARTIAL,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                dispatcher.getPackedLightCoords(kobold, SENTINEL_PARTIAL));
    }
}
