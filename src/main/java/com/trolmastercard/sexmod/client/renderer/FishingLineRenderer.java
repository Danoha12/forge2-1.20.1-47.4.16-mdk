package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Renders the fishing rod line + hook entity (gi) for NPC entities.
 * The hook travels from the NPC's hand toward its target and draws
 * the held item above the hook while in-flight.
 *
 * Obfuscated name: fi
 */
@OnlyIn(Dist.CLIENT)
public class FishingLineRenderer extends EntityRenderer<FishingHookEntity> {

    // Magic constants from original: string length offsets
    static final double LINE_SIDE_OFFSET = 0.1896224320030116D;
    static final double LINE_VERT_OFFSET = -0.5D;
    static final double LINE_DIAG_OFFSET = 0.08742380916962415D;

    private static final ResourceLocation PARTICLE_TEX =
            new ResourceLocation("textures/particle/particles.png");

    public FishingLineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FishingHookEntity hook, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        BaseNpcEntity owner = hook.getOwnerNpc();
        if (owner == null) return;
        if (owner.isInvisible()) return;
        if (Mth.abs(1.0F - owner.getScale()) < 0.001F) return; // scale == 1.0  skip

        // Advance visual approach factor
        hook.approachFactor += (60.0F / Minecraft.getInstance().getDeltaFrameTime())
                * 0.01666F * 2.0F;
        hook.approachFactor = Math.min(1.0F, hook.approachFactor);

        ItemStack heldItem = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        if (heldItem.getItem().equals(Items.AIR)) {
            hook.approachFactor = 0.0F;
            return;
        }

        // Interpolate hook position toward target
        Vec3 ownerEye = lerpVec(owner, partialTick).add(0, 0.875D, 0);
        Vec3 hookPos  = lerpVec(hook, partialTick);

        Vec3 hookDir  = hookPos.subtract(ownerEye);
        Vec3 targetPos = lerpVec(hook, partialTick); // base position
        Vec3 finalPos = ownerEye.add(hookDir.scale(hook.approachFactor));

        double rx = finalPos.x;
        double ry = finalPos.y;
        double rz = finalPos.z;

        poseStack.pushPose();
        poseStack.translate(rx, ry, rz);

        // Rotate to face camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.5F, 0.5F, 0.5F);

        // Render held item above hook
        if (!heldItem.getItem().equals(Items.AIR)) {
            poseStack.scale(2.0F, 2.0F, 2.0F);
            poseStack.translate(0, -0.2F, 0);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    (net.minecraft.world.entity.LivingEntity) owner,
                    heldItem,
                    net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    buffer,
                    owner.level(),
                    packedLight,
                    net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(
                            buffer, RenderType.armorCutoutNoCull(PARTICLE_TEX), false, false),
                    owner.getId());
            poseStack.translate(0, 0.2F, 0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }

        // Draw hook quad (particle sprite)
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(PARTICLE_TEX));
        consumer.vertex(poseStack.last().pose(), -0.5F, -0.5F, 0)
                .uv(0.0625F, 0.1875F).normal(0, 1, 0).endVertex();
        consumer.vertex(poseStack.last().pose(), 0.5F, -0.5F, 0)
                .uv(0.125F, 0.1875F).normal(0, 1, 0).endVertex();
        consumer.vertex(poseStack.last().pose(), 0.5F, 0.5F, 0)
                .uv(0.125F, 0.125F).normal(0, 1, 0).endVertex();
        consumer.vertex(poseStack.last().pose(), -0.5F, 0.5F, 0)
                .uv(0.0625F, 0.125F).normal(0, 1, 0).endVertex();

        poseStack.popPose();

        // Draw fishing line string if item is air (rod logic)
        if (heldItem.getItem().equals(Items.AIR)) {
            // Compute hand position offsets (mirrors EntityRenderer logic for fishing rod)
            int handSign = (owner.getMainArm() == HumanoidArm.RIGHT) ? 1 : -1;
            ItemStack offHandItem = owner.getOffhandItem();
            if (!(offHandItem.getItem() instanceof FishingRodItem)) handSign = -handSign;

            float yRad   = owner.yBodyRot * ((float) Math.PI / 180F);
            double sinY  = Mth.sin(yRad);
            double cosY  = Mth.cos(yRad);
            double hDist = handSign * 0.35D;

            Vec3 ownerInterp = lerpVec(owner, partialTick);
            double hx = ownerInterp.x + cosY * 0.8D + sinY * hDist;
            double hy = ownerInterp.y + owner.getEyeHeight() - 0.45D;
            double hz = ownerInterp.z + sinY * 0.8D - cosY * hDist;

            // Hook position offsets from original constants
            double hookAngle = (owner.yBodyRot + 90F) * (Math.PI / 180D);
            double hx2 = hook.getX() + partialTick * (hook.getX() - hook.xo)
                    - Math.sin(hookAngle) * LINE_SIDE_OFFSET
                    - Math.sin(owner.yBodyRot * (Math.PI / 180D)) * LINE_DIAG_OFFSET;
            double hy2 = hook.getY() + partialTick * (hook.getY() - hook.yo) + 0.25D + LINE_VERT_OFFSET;
            double hz2 = hook.getZ() + partialTick * (hook.getZ() - hook.zo)
                    + Math.cos(hookAngle) * LINE_SIDE_OFFSET
                    + Math.cos(owner.yBodyRot * (Math.PI / 180D)) * LINE_DIAG_OFFSET;

            double dx = (float)(hx - hx2);
            double dy = (float)(hy - hy2);
            double dz = (float)(hz - hz2);

            poseStack.pushPose();
            VertexConsumer line = buffer.getBuffer(RenderType.lineStrip());
            for (int i = 0; i <= 16; i++) {
                float t = i / 16.0F;
                line.vertex(poseStack.last().pose(),
                        (float)(rx + dx * t),
                        (float)(ry + dy * (t * t + t) * 0.5D + 0.25D),
                        (float)(rz + dz * t))
                        .color(0, 0, 0, 255)
                        .endVertex();
            }
            poseStack.popPose();
        }

        super.render(hook, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(FishingHookEntity entity) {
        return PARTICLE_TEX;
    }

    // -- Helper ----------------------------------------------------------------

    private static Vec3 lerpVec(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }
}
