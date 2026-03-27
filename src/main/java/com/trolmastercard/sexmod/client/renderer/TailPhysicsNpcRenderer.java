package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Ported from dv.java (1.12.2 - 1.20.1)
 * NpcHandRenderer subclass that adds inertial physics to the "tail" and "body" bones,
 * and optionally to "armL" / "armR".
 *
 * Physics constants (translated from original):
 *  MAX_SWAY   = 8.0 (lateral speed multiplier)
 *  MAX_LEAN   = 1.68 (max rotation radians for tail)
 *  BODY_SCALE = 5.0 (body bob multiplier)
 *
 * A static ticker (inner class {@link Ticker}) must be registered on the Forge event bus
 * to update all active instances every client tick.
 *
 * Original: {@code class dv extends dm}
 */
public class TailPhysicsNpcRenderer extends NpcHandRenderer {

    // -- Physics constants -----------------------------------------------------
    private static final float MAX_SWAY   = 8.0F;
    private static final float MAX_LEAN   = 1.68F;
    private static final float BODY_SCALE = 5.0F;

    /** All live instances so the static ticker can update them all. */
    static final Collection<TailPhysicsNpcRenderer> ALL = new ArrayList<>();

    // -- Per-instance physics state --------------------------------------------
    /** Current owner position (updated by ticker). */
    private double curX = 0.0, curZ = 0.0;
    /** Previous owner position. */
    private double prevX = 0.0, prevZ = 0.0;

    /** Current angular velocities for tail bones (X, Z). */
    private float tailRotX = 0.0F, tailRotZ = 0.0F;
    /** Previous frame's velocities (for lerp). */
    private float prevTailRotX = 0.0F, prevTailRotZ = 0.0F;

    /** Current body-bob amount [0, 1]. */
    private double bodyBob = 0.0, prevBodyBob = 0.0;

    public TailPhysicsNpcRenderer(EntityRendererProvider.Context context,
                                   GeoModel<BaseNpcEntity> model,
                                   double shadowRadius) {
        super(context, model, shadowRadius);
        ALL.add(this);
    }

    // -- Base transform --------------------------------------------------------

    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        poseStack.translate(0.0F, -1.1F, 0.0F);
        poseStack.scale(0.7F, 0.7F, 0.7F);
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack,
                                          boolean isMainHand,
                                          ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);
        if (isActionItem(itemStack)) return;
        if (!isMainHand) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(20.0F));
        }
        poseStack.translate(0.0, 0.05, 0.0);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isMainHand) {
        super.applyEmptyHandTransform(poseStack, isMainHand);
        if (isMainHand) {
            poseStack.translate(0.15, 0.0, 0.0);
        } else {
            poseStack.translate(-0.05, 0.0, 0.0);
        }
    }

    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack,
                                            boolean isMainHand,
                                            boolean isOffHand) {
        super.applyTwoHandedTransform(poseStack, isMainHand, isOffHand);
        if (isMainHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, -0.1);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(10.0F));
        } else if (!isMainHand && !isOffHand) {
            poseStack.translate(-0.05, -0.125, 0.125);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(50.0F));
        }
    }

    // -- Bone physics ----------------------------------------------------------

    @Override
    protected void onBoneProcess(String boneName, CoreGeoBone bone) {
        if (entity == null) return;
        boolean isFrozen = entity.getEntityData().get(BaseNpcEntity.FROZEN);
        if (isFrozen) return;

        float partialTick = net.minecraft.client.Minecraft.getInstance().getPartialTick();

        switch (boneName) {
            case "tail" -> applyTailPhysics(bone, 0.0F, 0.0F, 1.0F, partialTick);
            case "body" -> applyBodyPhysics(bone, partialTick);
            case "armL" -> {
                if (entity.getAnimState() != AnimState.BOW) {
                    applyTailPhysics(bone, 0.0F, -0.34906584F, 0.15F, partialTick);
                }
            }
            case "armR" -> {
                if (entity.getAnimState() != AnimState.BOW
                        && entity.getAnimState() != AnimState.ATTACK) {
                    applyTailPhysics(bone, 0.0F, 0.34906584F, 0.15F, partialTick);
                }
            }
        }
    }

    /**
     * Applies inertial swing to a bone.
     * @param baseX  base rotation X offset (rest pose)
     * @param baseZ  base rotation Z offset (rest pose)
     * @param scale  how strongly velocity affects this bone
     */
    private void applyTailPhysics(CoreGeoBone bone,
                                   float baseX, float baseZ, float scale,
                                   float partialTick) {
        double yaw = Math.toRadians(entity.getYRot());
        // Velocity in local space
        double dx = curX - prevX;
        double dz = curZ - prevZ;
        float localX = (float) (dx * Math.cos(yaw) + dz * Math.sin(yaw));
        float localZ = (float) (-dx * Math.sin(yaw) + dz * Math.cos(yaw));

        float targetX = localX * -MAX_SWAY;
        float targetZ = localZ * MAX_SWAY;
        targetX = MathUtil.clamp(targetX, -MAX_LEAN, MAX_LEAN);
        targetZ = MathUtil.clamp(targetZ, -MAX_LEAN, MAX_LEAN);
        tailRotX = (float) MathUtil.lerp(prevTailRotX, targetX, partialTick);
        tailRotZ = (float) MathUtil.lerp(prevTailRotZ, targetZ, partialTick);

        bone.setRotX(baseX + tailRotX * scale);
        bone.setRotZ(baseZ + tailRotZ * scale);
    }

    /**
     * Applies bob physics to the body bone.
     * Also forwards the bob value to AllieEntity.extraBob if applicable.
     */
    private void applyBodyPhysics(CoreGeoBone bone, float partialTick) {
        double dx = curX - prevX;
        double dz = curZ - prevZ;
        double speed = (Math.abs(dx) + Math.abs(dz)) * BODY_SCALE;
        speed = MathUtil.clamp((float) speed, 0.0F, 1.0F);

        double smoothBob = MathUtil.lerp(prevBodyBob, speed, partialTick);
        bone.setPosY((float) MathUtil.lerp(BODY_SCALE, 0.0, smoothBob));

        // AllieEntity-specific extra bob
        if (entity instanceof AllieEntity allie) {
            allie.extraBobAmount = (float) MathUtil.lerp(0.3, 0.0, smoothBob);
        }
    }

    // -- Ticker (called from static inner class) -------------------------------

    void tick() {
        if (entity == null) return;
        prevTailRotX = tailRotX;
        prevTailRotZ = tailRotZ;
        prevBodyBob  = bodyBob;

        if (entity.getOwnerUUID() == null) return;

        net.minecraft.world.entity.player.Player player =
                entity.level().getPlayerByUUID(entity.getOwnerUUID());
        if (player == null) return;

        prevX = curX;
        prevZ = curZ;
        curX  = player.getX();
        curZ  = player.getZ();
    }

    // -- Helpers ---------------------------------------------------------------

    private boolean isActionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var action = stack.getUseAnimation();
        return action == UseAnim.BOW || action == UseAnim.CROSSBOW;
    }

    // -- Static ticker ---------------------------------------------------------

    /**
     * Register an instance of this on the Forge event bus to tick all renderers.
     * {@code MinecraftForge.EVENT_BUS.register(new TailPhysicsNpcRenderer.Ticker());}
     */
    public static class Ticker {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            for (TailPhysicsNpcRenderer renderer : TailPhysicsNpcRenderer.ALL) {
                renderer.tick();
            }
        }
    }
}
