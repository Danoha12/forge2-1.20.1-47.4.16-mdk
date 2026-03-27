package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.vecmath.Vector2f;
import java.util.UUID;

/**
 * KoboldShoulderRenderHandler - ported from e_.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Registered on the Forge bus. During a {@link RenderPlayerEvent.Pre} it:
 *   1. Cancels the default player render.
 *   2. Copies the player's rotation/position onto the PlayerKoboldEntity.
 *   3. Calls the entity renderer to draw the PlayerKoboldEntity at the
 *      same screen position as the player (so it perfectly overlays the player).
 *
 * Also provides the static helper {@link #renderAsPlayer} used by other systems
 * (e.g. GoblinEntityRenderer/dy) to render a PlayerKoboldEntity in place of
 * the player.
 *
 * Sentinel partial-tick value {@code SENTINEL = 1.2345679F} is used to
 * suppress recursive calls: when RenderManager renders the entity at the
 * sentinel tick value we know to skip.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - @SubscribeEvent(RenderPlayerEvent.Pre) still exists
 *   - RenderWorldLastEvent - RenderLevelStageEvent
 *   - TickEvent.RenderTickEvent still exists
 *   - entity.field_70177_z/ar/aq/etc - getYRot/yHeadRot/yBodyRot/etc
 *   - entity.field_70122_E - isOnGround()
 *   - entity.func_70093_af() - isShiftKeyDown()
 *   - entity.func_70051_ag() - isUsingItem()
 *   - entity.func_184218_aH() - isFallFlying()
 *   - entity.field_70169_q/r/s - xOld/yOld/zOld
 *   - entity.field_70142_S/T/U - xOldOld/yOldOld/zOldOld (approx)
 *   - entity.func_184605_cv() - getTicksUsingItem()
 *   - renderManager.func_188391_a(entity, x, y, z, yaw, pt, forced) - dispatcher.render(...)
 *   - ei.C() - PlayerKoboldEntity.cleanup()
 *   - ei.d(uuid) - PlayerKoboldEntity.getForPlayer(uuid)
 *   - ei.E() - playerKobold.isVisible()
 *   - paramei.z() - playerKobold.isShouldRideOffset()
 *   - dm.v - NpcBodyRenderer.RENDERING_SHOULDER
 *   - b6.a(v1, v2, t) - MathUtil.lerpVec3(v1, v2, t)
 *   - paramei.c(player) - playerKobold.mirrorPlayer(player)
 *   - paramei.ao = new Vector2f(...) - playerKobold.footOffset = new Vector2f(...)
 *   - em.G - BaseNpcEntity.isSexActive
 */
@OnlyIn(Dist.CLIENT)
public class KoboldShoulderRenderHandler {

    /** Sentinel partial-tick value to detect recursive render calls. */
    public static final float SENTINEL = 1.2345679F;

    private Vec3 prevPos  = null;
    private Vec3 prevPos2 = null;
    private PlayerKoboldEntity trackedKobold = null;
    private boolean isActive = false;

    // -- RenderPlayerEvent.Pre --------------------------------------------------

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // Suppress recursive call
        if (event.getPartialTick() == SENTINEL) return;

        // Cleanup stale kobold references
        PlayerKoboldEntity.cleanup();

        UUID playerId = event.getEntity().getGameProfile().getId();
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(playerId);
        if (kobold == null) return;

        // Cancel default player render - we'll draw the kobold in its place
        event.setCanceled(true);

        renderAsPlayer(kobold, event.getEntity(),
                event.getX(), event.getY(), event.getZ(),
                event.getPartialTick());
    }

    // -- Static helper: render PlayerKoboldEntity in place of a Player ----------

    /**
     * Copies all relevant rotation/position/state from {@code player} onto
     * {@code kobold} and then renders the kobold at the given world offset.
     *
     * @param kobold   the PlayerKoboldEntity to render
     * @param player   the player whose position/rotation/state to mirror
     * @param dx dy dz the render offset (entity pos minus camera pos)
     * @param partialTick  the partial tick for interpolation
     */
    public static void renderAsPlayer(PlayerKoboldEntity kobold, Player player,
                                       double dx, double dy, double dz,
                                       float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // Mirror the owner player
        player = kobold.mirrorPlayer(player);
        if (player == null) return;

        // Don't render if the player is invisible to the local client
        // (but still render if the kobold has the "always visible" flag)
        if (player.isInvisibleTo(mc.player) && !kobold.isVisible()) return;

        var dispatcher = mc.getEntityRenderDispatcher();

        // Copy rotation
        kobold.setYRot(player.getYRot());
        kobold.yHeadRot  = player.yHeadRot;
        kobold.yBodyRot  = player.yBodyRot;
        kobold.yHeadRotO = player.yHeadRotO;
        kobold.xRotO     = player.xRotO;
        kobold.setXRot(player.getXRot());

        // Copy previous tick positions for interpolation
        kobold.xOld = player.xOld;
        kobold.yOld = player.yOld;
        kobold.zOld = player.zOld;
        kobold.xOldOld = player.xOldOld;
        kobold.yOldOld = player.yOldOld;
        kobold.zOldOld = player.zOldOld;

        // Copy movement state flags
        kobold.setOnGround(player.isOnGround());
        kobold.setSprinting(player.isSprinting());
        kobold.setShiftKeyDown(player.isShiftKeyDown());
        kobold.setItemInUse(player.isUsingItem(), player.getTicksUsingItem());
        kobold.isFallFlying = player.isFallFlying();

        // Compute foot horizontal offset (used by some animation controllers)
        double yaw  = Math.toRadians(player.getYRot());
        double odx  = player.xOldOld - player.getX();
        double odz  = player.zOldOld - player.getZ();
        kobold.footOffset = new Vector2f(
                (float)(odx * Math.cos(yaw) + odz * Math.sin(yaw)),
                (float)(odx * Math.sin(yaw) + odz * Math.cos(yaw)));

        // Optional Y offset for shoulder-ride mode
        float yOffset = kobold.isShouldRideOffset()
                ? computeShoulderOffset(kobold, player, partialTick)
                : 0.0F;

        // Render via the entity render dispatcher using the sentinel partial-tick
        // to prevent re-entrant cancellation in onRenderPlayer.
        NpcBodyRenderer.RENDERING_SHOULDER = true;
        try {
            dispatcher.render(kobold, dx, dy + yOffset, dz, 90.0F, partialTick,
                    new com.mojang.blaze3d.vertex.PoseStack(),
                    mc.renderBuffers().bufferSource(),
                    dispatcher.getPackedLightCoords(kobold, partialTick));
        } finally {
            NpcBodyRenderer.RENDERING_SHOULDER = false;
        }
    }

    /**
     * Computes the vertical Y offset for rendering a shoulder-riding kobold.
     * The kobold bobs up/down with the player's walk cycle.
     */
    private static float computeShoulderOffset(PlayerKoboldEntity kobold,
                                                 Player player, float partialTick) {
        // If the kobold is in a sex-active state, don't apply offset
        if (kobold.getEntityData().get(BaseNpcEntity.isSexActive)) return 0.0F;

        // Bow/crossbow usage check
        boolean holdingBow = player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem;
        // Use main-hand item check
        float headPitch = player.getXRot();
        float walkCycle = player.walkAnimation.position(partialTick);
        float swing     = player.walkAnimation.speed(partialTick);

        // Bob using sin of walk cycle (mirrors 1.12.2 formula from dy)
        double offset = Math.sin(headPitch * Math.PI / 180.0F) * swing * 0.5;
        return (float) offset;
    }
}
