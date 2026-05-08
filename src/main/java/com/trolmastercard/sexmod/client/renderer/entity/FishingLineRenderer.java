package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.FishingHookEntity;
import com.trolmastercard.sexmod.entity.LunaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * FishingLineRenderer — Portado a 1.20.1 y enmascarado (SFW).
 * * Renderiza la línea de pesca y el anzuelo para el NPC Luna.
 * Incluye lógica para dibujar el ítem sostenido "volando" junto al anzuelo.
 */
@OnlyIn(Dist.CLIENT)
public class FishingLineRenderer extends EntityRenderer<FishingHookEntity> {

    private static final ResourceLocation PARTICLE_TEX = new ResourceLocation("textures/particle/particles.png");

    public FishingLineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FishingHookEntity hook, float entityYaw, float partialTick, PoseStack ps, MultiBufferSource buffer, int packedLight) {
        LunaEntity owner = hook.getOwner(); // Usando el método que definimos en FishingHookEntity
        if (owner == null || owner.isInvisible()) return;

        ps.pushPose();

        // 1. Renderizar el Anzuelo (Bobber)
        renderBobber(hook, ps, buffer, packedLight);

        // 2. Renderizar el Ítem que vuela (si aplica)
        ItemStack heldItem = owner.getMainHandItem();
        if (!heldItem.isEmpty()) {
            renderFlyingItem(owner, heldItem, ps, buffer, packedLight);
        }

        ps.popPose();

        // 3. Renderizar la Cuerda (Line)
        renderFishingLine(hook, owner, partialTick, ps, buffer);

        super.render(hook, entityYaw, partialTick, ps, buffer, packedLight);
    }

    private void renderBobber(FishingHookEntity hook, PoseStack ps, MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.scale(0.5F, 0.5F, 0.5F);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(PARTICLE_TEX));
        Matrix4f matrix = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        // Dibujar el quad del anzuelo (usando el sprite de partículas de Minecraft)
        vertex(consumer, matrix, normal, -0.5F, -0.5F, 0.0625F, 0.1875F, packedLight);
        vertex(consumer, matrix, normal, 0.5F, -0.5F, 0.125F, 0.1875F, packedLight);
        vertex(consumer, matrix, normal, 0.5F, 0.5F, 0.125F, 0.125F, packedLight);
        vertex(consumer, matrix, normal, -0.5F, 0.5F, 0.0625F, 0.125F, packedLight);

        ps.popPose();
    }

    private void renderFlyingItem(LunaEntity owner, ItemStack stack, PoseStack ps, MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.translate(0, 0.25F, 0);
        ps.scale(0.4F, 0.4F, 0.4F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ps,
                buffer,
                owner.level(),
                owner.getId()
        );
        ps.popPose();
    }

    private void renderFishingLine(FishingHookEntity hook, LunaEntity owner, float partialTick, PoseStack ps, MultiBufferSource buffer) {
        // 1. Calcular posición interpolada del dueño (Luna)
        double ownerX = Mth.lerp(partialTick, owner.xo, owner.getX());
        double ownerY = Mth.lerp(partialTick, owner.yo, owner.getY());
        double ownerZ = Mth.lerp(partialTick, owner.zo, owner.getZ());
        Vec3 ownerPos = new Vec3(ownerX, ownerY, ownerZ);

        // 2. Calcular posición de la mano del NPC
        int side = owner.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        float bodyYaw = Mth.lerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;

        double sinYaw = Mth.sin(bodyYaw);
        double cosYaw = Mth.cos(bodyYaw);

        // Offset de la mano relativo al cuerpo
        Vec3 handPos = ownerPos.add(
                -sinYaw * 0.7D + cosYaw * (0.4D * side),
                owner.getEyeHeight() - 0.5D,
                -cosYaw * 0.7D - sinYaw * (0.4D * side)
        );

        // 3. Calcular posición interpolada del anzuelo (Hook)
        double hookX = Mth.lerp(partialTick, hook.xo, hook.getX());
        double hookY = Mth.lerp(partialTick, hook.yo, hook.getY());
        double hookZ = Mth.lerp(partialTick, hook.zo, hook.getZ());
        Vec3 hookPos = new Vec3(hookX, hookY, hookZ);

        // 4. Calcular el vector de distancia entre la mano y el anzuelo
        double dx = handPos.x - hookPos.x;
        double dy = handPos.y - hookPos.y;
        double dz = handPos.z - hookPos.z;

        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
        Matrix4f matrix = ps.last().pose();

        // 5. Dibujar la cuerda
        for (int i = 0; i <= 16; ++i) {
            float f = (float) i / 16.0F;
            float gravity = Mth.sin(f * (float) Math.PI) * 0.2F;
            // En 1.20.1, el método vertex() puede variar ligeramente según el RenderType,
            // pero para lineStrip el formato suele ser POSITION_COLOR.
            lineConsumer.vertex(matrix, (float) (dx * f), (float) (dy * (f * f + f) * 0.5D + 0.2D + gravity), (float) (dz * f))
                    .color(0, 0, 0, 255)
                    .normal(ps.last().normal(), 0, 1, 0) // Añadimos la normal por seguridad
                    .endVertex();
        }
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v, int light) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FishingHookEntity entity) {
        return PARTICLE_TEX;
    }
}