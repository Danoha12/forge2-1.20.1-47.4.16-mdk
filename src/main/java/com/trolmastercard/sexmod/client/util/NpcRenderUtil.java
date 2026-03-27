package com.trolmastercard.sexmod.client.util;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.trolmastercard.sexmod.util.MathUtil.lerpPosition;

/**
 * NpcRenderUtil - ported from ak.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Static rendering helpers used throughout the mod.
 *
 * Method mapping:
 *   b(Entity, EntityPlayer, float) - getOffsetFromPlayer(Entity, Player, float)
 *     Returns (NPC eye position) - (player position), both lerped by partialTick.
 *     Used to position render effects relative to the camera.
 *
 *   a(Entity, EntityPlayer, float) - getEntityOffset(Entity, Player, float)
 *     Returns (entity lerped pos) - (player lerped pos).
 *     Falls back to {@link #getLerpedPos(Entity, float)} for non-NPC entities.
 *
 *   a(Entity, float)               - getLerpedPos(Entity, float)
 *     For NPC entities that have a custom smooth-interpolation override (em.Q()
 *     - {@link BaseNpcEntity#hasSmoothPos()}), returns {@link BaseNpcEntity#getSmoothPos()}.
 *     Otherwise returns standard lerp of prevPos - pos.
 *
 *   b(Entity, float)               - standardLerpPos(Entity, float)  (package-private)
 *     Standard lerp of prevPos - currentPos.
 *
 *   a()                            - setFullBright()
 *     Sets lightmap texture coords to (240, 240) - full bright.
 *     In 1.20.1 use {@code LightTexture.FULL_BRIGHT} as the packed light value
 *     when calling vertex consumers; this method handles the RenderSystem side.
 */
@OnlyIn(Dist.CLIENT)
public final class NpcRenderUtil {

    private NpcRenderUtil() {}

    // =========================================================================
    //  b(Entity, EntityPlayer, float) - getOffsetFromPlayer
    // =========================================================================

    /**
     * Returns the NPC's eye-level position minus the player's position,
     * both lerped to {@code partialTick}.
     *
     * Original: {@code ak.b(Entity, EntityPlayer, float)}
     *   vec1 = lerp(prevPos, pos + (0, eyeHeight, 0), t)
     *   vec2 = lerp(prevPos, pos, t)          - player
     *   return vec1 - vec2
     */
    public static Vec3 getOffsetFromPlayer(Entity entity, Player player, float partialTick) {
        Vec3 prevNpc = new Vec3(entity.xo, entity.yo + player.getEyeHeight(), entity.zo);
        Vec3 curNpc  = entity.position().add(0, player.getEyeHeight(), 0);
        Vec3 npcEye  = lerpPosition(prevNpc, curNpc, partialTick);

        Vec3 prevPl  = new Vec3(player.xo, player.yo, player.zo);
        Vec3 plPos   = lerpPosition(prevPl, player.position(), partialTick);

        return npcEye.subtract(plPos);
    }

    // =========================================================================
    //  a(Entity, EntityPlayer, float) - getEntityOffset
    // =========================================================================

    /**
     * Returns the entity's lerped position minus the player's lerped position.
     * If {@code player} is null, returns just the entity's lerped position.
     *
     * Original: {@code ak.a(Entity, EntityPlayer, float)}
     */
    public static Vec3 getEntityOffset(Entity entity, Player player, float partialTick) {
        Vec3 entityPos = getLerpedPos(entity, partialTick);
        if (player == null) return entityPos;
        Vec3 playerPos = getLerpedPos(player, partialTick);
        return entityPos.subtract(playerPos);
    }

    // =========================================================================
    //  a(Entity, float) - getLerpedPos
    // =========================================================================

    /**
     * Returns a lerped position for {@code entity} at {@code partialTick}.
     *
     * If the entity is a {@link BaseNpcEntity} and has a custom smooth-position
     * override ({@link BaseNpcEntity#hasSmoothPos()}), that value is returned
     * directly.  Otherwise falls through to {@link #standardLerpPos}.
     *
     * Original: {@code ak.a(Entity, float)}
     */
    public static Vec3 getLerpedPos(Entity entity, float partialTick) {
        if (entity instanceof BaseNpcEntity npc) {
            if (npc.hasSmoothPos()) {
                return npc.getSmoothPos();     // em.o()
            }
        }
        return standardLerpPos(entity, partialTick);
    }

    // =========================================================================
    //  b(Entity, float) - standardLerpPos  (package-private)
    // =========================================================================

    /**
     * Standard lerp of prevPos - pos.
     *
     * Original: {@code ak.b(Entity, float)} (package-private)
     */
    static Vec3 standardLerpPos(Entity entity, float partialTick) {
        Vec3 prev = new Vec3(entity.xo, entity.yo, entity.zo);
        return lerpPosition(prev, entity.position(), partialTick);
    }

    // =========================================================================
    //  a() - setFullBright
    // =========================================================================

    /**
     * Sets the lightmap to maximum brightness (240, 240).
     *
     * Original: {@code ak.a()} - called {@code OpenGlHelper.func_77475_a(field_77476_b, 240, 240)}.
     *
     * In 1.20.1 most vertex consumers accept a packed light int; pass
     * {@code LightTexture.FULL_BRIGHT} instead of calling this.  This method
     * is kept for legacy call-sites that manipulate the lightmap directly.
     */
    @OnlyIn(Dist.CLIENT)
    public static void setFullBright() {
        // In 1.20.1 the lightmap texture unit is index 1.
        // For Immediate-mode renderers, set the shader uniform instead.
        // For modern PoseStack+VertexConsumer renderers use LightTexture.FULL_BRIGHT.
        RenderSystem.setShaderTexture(1, 0xF000F0); // packed 240|240
    }
}
