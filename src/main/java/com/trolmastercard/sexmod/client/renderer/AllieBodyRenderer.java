package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.Collection;

/**
 * AllieBodyRenderer (dv) - Ported from 1.12.2 to 1.20.1.
 *
 * {@link NpcArmRenderer} for Allie-type NPCs with tail-sway and body-bob
 * physics driven by the NPC's owner player movement.
 *
 * Physics fields:
 *   prevX/Z    - owner position last tick          (A/D - C/z)
 *   curX/Z     - owner position this tick           (C/z)
 *   swayX/Z    - computed sway angles               (G/I)
 *   smoothSwayX/Z - exponentially smoothed sway     (F/B)
 *   bodyBob    - current body bob factor            (L)
 *   prevBob    - body bob previous tick             (H)
 *
 * Constants:
 *   SWAY_SCALE   = 8.0      (E)
 *   MAX_SWAY     = 1.68     (K)
 *   BOB_SCALE    = 5.0      (M)
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code TickEvent.ClientTickEvent} subscription unchanged (client tick)
 *   - {@code Vec2f.field_189983_j/i} - {@code Vec2.x/y}
 *   - All {@code GlStateManager} - {@code PoseStack}
 *   - {@code be.b(val, min, max)} - {@code MathUtil.clamp(val, min, max)}
 *   - {@code b6.a(from, to, t)} - {@code MathUtil.lerp(from, to, t)}
 *   - {@code e5} - {@code AllieEntity}
 *   - {@code fp.BOW/ATTACK} - {@code AnimState.BOW/ATTACK}
 *   - {@code this.y} (partialTick) - stored each frame
 */
public class AllieBodyRenderer<T extends AllieEntity> extends NpcArmRenderer<T> {

    // -- Constants -------------------------------------------------------------

    private static final float SWAY_SCALE = 8.0f;
    private static final float MAX_SWAY   = 1.68f;
    private static final float BOB_SCALE  = 5.0f;

    // -- Shared instance registry (for tick updates) ---------------------------

    static final Collection<AllieBodyRenderer<?>> ALL_INSTANCES = new ArrayList<>();

    // -- Physics state ---------------------------------------------------------

    double curX  = 0, prevX = 0;
    double curZ  = 0, prevZ = 0;
    float  swayX = 0, prevSwayX = 0;
    float  swayZ = 0, prevSwayZ = 0;
    double bodyBob = 0, prevBob = 0;

    /** Partials tick stored by the render call. */
    private float partialTick = 0.0f;

    public AllieBodyRenderer(GeoModel<T> model) {
        super(model);
        ALL_INSTANCES.add(this);
    }

    // -- World-space transform -------------------------------------------------

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.1, 0);
        poseStack.scale(0.7f, 0.7f, 0.7f);
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        UseAnim anim = stack.getItem().getUseAnimation(stack);
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        if (!isRightHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0f));
        }
        poseStack.translate(0, 0.05, 0);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyEmptyHandTransform(poseStack, isRightHand);
        poseStack.translate(isRightHand ? 0.15 : -0.05, 0, 0);
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0f));
        } else if (!isRightHand && !isOffHand) {
            poseStack.translate(-0.05, -0.125, 0.125);
            poseStack.mulPose(Axis.XP.rotationDegrees(50.0f));
        }
    }

    // -- Physics per-bone -----------------------------------------------------

    @Override
    protected void onBoneProcess(String name, GeoBone bone) {
        if (entityRef == null) return;
        if (entityRef.isFrozen()) return;

        if ("tail".equals(name)) {
            applySwayToBone(bone, 0.0f, 0.0f, 1.0f);
        } else if ("body".equals(name)) {
            applyBodyBobToBone(bone);
        } else if ("armL".equals(name) && entityRef.getAnimState() != AnimState.BOW) {
            applySwayToBone(bone, 0.0f, -0.34906584f, 0.15f);
        } else if ("armR".equals(name) && entityRef.getAnimState() != AnimState.BOW
                                       && entityRef.getAnimState() != AnimState.ATTACK) {
            applySwayToBone(bone, 0.0f, 0.34906584f, 0.15f);
        }
    }

    /**
     * Compute the sway angles from owner movement delta, apply to bone.
     * Corresponds to {@code void a(GeoBone, float, float, float)} in 1.12.2.
     */
    private void applySwayToBone(GeoBone bone,
                                  float baseRotX, float baseRotZ, float influence) {
        double dx = curX - prevX;
        double dz = curZ - prevZ;
        double yawRad = Math.toRadians(entityRef.getYRot());

        // Rotate delta into entity-local space
        Vec2 local = new Vec2(
                (float)(dx * Math.cos(yawRad) + dz * Math.sin(yawRad)),
                (float)(-dx * Math.sin(yawRad) + dz * Math.cos(yawRad))
        );

        float rawX = local.x * -SWAY_SCALE;
        float rawZ = local.y *  SWAY_SCALE;
        rawX = MathUtil.clamp(rawX, -MAX_SWAY, MAX_SWAY);
        rawZ = MathUtil.clamp(rawZ, -MAX_SWAY, MAX_SWAY);

        // Smooth
        swayX = (float) MathUtil.lerp(prevSwayX, rawX, partialTick);
        swayZ = (float) MathUtil.lerp(prevSwayZ, rawZ, partialTick);

        bone.setRotX(baseRotX + swayX * influence);
        bone.setRotZ(baseRotZ + swayZ * influence);
    }

    /**
     * Apply body-bob displacement; also updates {@code AllieEntity.bobScale}.
     * Corresponds to {@code void a(GeoBone)} in 1.12.2.
     */
    private void applyBodyBobToBone(GeoBone bone) {
        double dx = curX - prevX;
        double dz = curZ - prevZ;
        double bobTarget = Math.min(1.0, (Math.abs(dx) + Math.abs(dz)) * BOB_SCALE);
        float smoothedBob = (float) MathUtil.lerp(prevBob, bobTarget, partialTick);
        bone.setPosY((float) MathUtil.lerp(5.0, 0.0, smoothedBob));

        entityRef.bobScale = (float) MathUtil.lerp(0.3, 0.0, smoothedBob);
    }

    // -- Tick update (owner position tracking) --------------------------------

    /**
     * Called every client tick for all registered instances to advance the
     * physics simulation by one step.
     */
    void tickPhysics() {
        if (entityRef == null) return;

        // Save smoothed - previous
        prevSwayX = swayX;
        prevSwayZ = swayZ;
        prevBob   = bodyBob;

        // Track owner player
        if (entityRef.getOwnerUUID() == null) return;
        var player = entityRef.level().getPlayerByUUID(entityRef.getOwnerUUID());
        if (player == null) return;

        prevX = curX;
        prevZ = curZ;
        curX  = player.getX();
        curZ  = player.getZ();
    }

    // -- Static tick event subscriber -----------------------------------------

    public static class TickHandler {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            for (AllieBodyRenderer<?> renderer : AllieBodyRenderer.ALL_INSTANCES) {
                renderer.tickPhysics();
            }
        }
    }
}
