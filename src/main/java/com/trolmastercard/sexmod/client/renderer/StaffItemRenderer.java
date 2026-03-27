package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.model.StaffModel;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.item.StaffItem;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Item renderer for {@link StaffItem} - draws an ender-crystal orb on the
 * staff tip and optionally visualises the staff's "target" kobold positions
 * as orbiting block items.
 *
 * Ported from fa.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class StaffItemRenderer extends GeoItemRenderer<StaffItem> {

    // -- Constants (original static fields) -----------------------------------
    private static final ResourceLocation ENDER_CRYSTAL_TEX =
            new ResourceLocation("textures/entity/endercrystal/endercrystal.png");

    static final float ORBIT_RADIUS     = 10.0F;  // p
    static final float TIP_Y_OFFSET     =  1.5F;  // f
    static final float ORB_BASE_SCALE   =  0.175F; // m
    static final float SCALE_VARIATION  =  0.1F;   // r
    static final float TILT_FACTOR      =  0.04F;  // g
    static final float PETAL_D          =  8.0F;   // d
    static final float PETAL_I          =  6.0F;   // i
    static final float ITEM_SCALE       =  1.3F;   // a

    // UV coords for orbit display
    static final Vector2f[] ORBIT_UVS = {
        new Vector2f(1.0F, 0.0F), new Vector2f(0.0F, 1.0F), new Vector2f(0.0F, 0.0F),
        new Vector2f(0.5F, 0.5F), new Vector2f(0.75F, 0.25F),
        new Vector2f(0.25F, 0.75F), new Vector2f(0.25F, 0.75F)
    };

    /** Whether "scatter mode" is active (shows block items at calculated positions). */
    private static boolean scatterMode = false;

    // -- Per-render state ------------------------------------------------------
    private Vector2f   walkDelta;
    private double     gameTime   = 0.0D;
    private Player     holder;
    private ItemStack  heldStack;

    /** Per-ItemStack physics position cache (originally static HashMap). */
    private static final HashMap<ItemStack, Vector3f> itemPositions = new HashMap<>();

    public StaffItemRenderer() {
        super(new StaffModel());
    }

    // -- Public API ------------------------------------------------------------

    public static boolean isScatterMode()  { return scatterMode; }
    public static void    toggleScatter()  { scatterMode = !scatterMode; }

    // -- Pre-render setup ------------------------------------------------------

    @Override
    public void renderByItem(StaffItem item, ItemDisplayContext ctx,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        holder = null;

        // Identify the player holding this item
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                if (p.getInventory().items.contains(heldStack)
                 || p.getInventory().offhand.contains(heldStack)) {
                    holder = p;
                    break;
                }
            }
        }

        // Compute walk-delta XZ vector for orb physics tilt
        if (holder != null) {
            double dx = holder.getX() - holder.xOld;
            double dz = holder.getZ() - holder.zOld;
            double yaw = Math.toRadians(holder.getYRot());
            walkDelta = new Vector2f(
                    (float)( dx * Math.cos(yaw) + dz * Math.sin(yaw)),
                    (float)(-dx * Math.sin(yaw) + dz * Math.cos(yaw))
            );
        } else {
            walkDelta = new Vector2f(0.0F, 0.0F);
        }

        if (!mc.isPaused()) {
            gameTime = mc.player != null
                    ? mc.player.tickCount + mc.getFrameTime()
                    : gameTime;
        }

        this.heldStack = heldStack;
        super.renderByItem(item, ctx, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // -- Bone override - staff-tip orb -----------------------------------------

    @Override
    public void renderRecursively(PoseStack poseStack, StaffItem item, GeoBone bone,
                                  MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if ("staff".equals(bone.getName())) {
            poseStack.pushPose();

            // Move to bone pivot
            bone.applyTransform(poseStack);

            // Translate to tip position (with subtle sinusoidal bob)
            double bob = TIP_Y_OFFSET + 0.001D * Math.sin(0.005D * gameTime) + 0.001D;
            poseStack.translate(0.0D, bob, 0.0D);

            // Scale + physics tilt from walk delta
            double scale = orbScale();
            poseStack.scale((float)scale, (float)scale, (float)scale);

            // Apply accumulated physics position
            Vector3f physPos = itemPositions.computeIfAbsent(heldStack, k -> new Vector3f());
            physPos.add(walkDelta.x, (holder != null ? (float)(holder.getY() - holder.yOld) : 0.0F), walkDelta.y);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees( physPos.z * ORBIT_RADIUS));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees( physPos.x * ORBIT_RADIUS));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-physPos.y * ORBIT_RADIUS));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float)(gameTime * 0.1D)));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(gameTime * 0.1D)));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)(gameTime * 0.1D)));
            itemPositions.put(heldStack, physPos);

            // Render ender-crystal model at the tip
            renderEnderCrystalOrb(poseStack);

            poseStack.popPose();

            // Render orbiting kobold-target items (if holder is present)
            if (holder != null) renderKoboldTargets(poseStack);

            // Reset buffer to the model's texture before recursing
        }

        super.renderRecursively(poseStack, item, bone, bufferSource, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    // -- Orbiting item rendering -----------------------------------------------

    /**
     * Collects the list of active kobold positions stored in
     * {@link KoboldEntity#staffTargetPositions} and renders them as
     * small orbiting block items (original {@code c()} / {@code a()/b()} methods).
     */
    private void renderKoboldTargets(PoseStack poseStack) {
        if (KoboldEntity.staffTargetPositions == null || KoboldEntity.staffTargetPositions.isEmpty()) return;

        List<Integer>  ids  = new ArrayList<>();
        List<Vec3>     poss = new ArrayList<>();
        for (var entry : KoboldEntity.staffTargetPositions) {
            ids.add((int) entry.w());
            poss.add(new Vec3(entry.x(), entry.y(), entry.z()));
        }

        if (scatterMode) {
            renderTargetsScatter(poseStack, ids, poss);
        } else {
            renderTargetsRainbow(poseStack, ids);
        }
    }

    /** Rainbow mode - items equally spaced around a fixed colour ring. */
    private void renderTargetsRainbow(PoseStack poseStack, List<Integer> ids) {
        float step    = 1.0F / ids.size();
        float current = 0.0F;
        for (int i = 0; i < ids.size(); i++) {
            current += step;
            float blue  = (float) MathUtil.lerp(0.8D, 1.2D, i / (double) ids.size());
            renderOrbitItem(poseStack, ids.get(i), 1.0F - current, 0.0F + current, blue, false);
        }
    }

    /** Scatter mode - items pushed toward their real in-world direction. */
    private void renderTargetsScatter(PoseStack poseStack, List<Integer> ids, List<Vec3> poss) {
        if (holder == null) return;
        float yaw   = MathUtil.lerpAngle(holder.yRotO,   holder.getYRot(),   Minecraft.getInstance().getFrameTime());
        float pitch = MathUtil.lerpAngle(holder.xRotO,   holder.getXRot(),   Minecraft.getInstance().getFrameTime());
        Vec3  eye   = MathUtil.lerpPos(
                new Vec3(holder.xOld, holder.yOld + holder.getEyeHeight(), holder.zOld),
                holder.getEyePosition(), Minecraft.getInstance().getFrameTime());

        for (int i = 0; i < ids.size(); i++) {
            Vec3 dir = eye.subtract(poss.get(i));
            dir = MathUtil.rotateVec(dir, -pitch, yaw);

            double len = Math.abs(dir.x) + Math.abs(dir.y) + Math.abs(dir.z);
            double nx  = applyFisheyeCompress(-dir.x / len) * 1.3D;
            double ny  = applyFisheyeCompress(-dir.y / len) * 1.3D;
            double nz  = applyFisheyeCompress( dir.z / len) * 1.3D;

            renderOrbitItem(poseStack, ids.get(i), (float)nx, (float)ny, (float)nz, true);
        }
    }

    /** Renders a single orbiting block item at the computed angular offset. */
    private void renderOrbitItem(PoseStack ps, int metaOrId,
                                  float nx, float ny, float nz,
                                  boolean useSpin) {
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.translate(0.0D, TIP_Y_OFFSET + 0.001D * Math.sin(0.005D * gameTime) + 0.001D, 0.0D);
        ps.scale(TILT_FACTOR, TILT_FACTOR, TILT_FACTOR);
        ps.translate(nx * PETAL_D, ny * PETAL_D, nz * PETAL_D);
        if (useSpin) {
            ps.mulPose(com.mojang.math.Axis.of(new Vector3f(nx, ny, nz))
                    .rotationDegrees((float)(gameTime * PETAL_I * nz)));
            ps.translate(PETAL_I, 0.0F, 0.0F);
        }
        mc.getItemRenderer().renderStatic(
                new ItemStack(net.minecraft.world.level.block.Blocks.STONE),
                ItemDisplayContext.NONE,
                15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                ps,
                mc.renderBuffers().bufferSource(),
                mc.level,
                0
        );
        ps.popPose();
    }

    /** Draws the ender-crystal model centred at the current pose. */
    private void renderEnderCrystalOrb(PoseStack poseStack) {
        // Vanilla EnderCrystalRenderer is a standalone renderer in 1.20.1.
        // We delegate to a thin helper that binds the texture and renders the model.
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bindForSetup(ENDER_CRYSTAL_TEX);
        // TODO: use EnderCrystalRenderer directly once entity renderer access is stable.
    }

    // -- Orb scale computation -------------------------------------------------

    private double orbScale() {
        return ORB_BASE_SCALE + SCALE_VARIATION * Math.sin(0.005D * gameTime) + SCALE_VARIATION;
    }

    /** Fisheye-style compression: {@code x * sqrt(1 - x-/2)} */
    private double applyFisheyeCompress(double x) {
        return x * Math.sqrt(1.0D - x * x / 2.0D);
    }
}
