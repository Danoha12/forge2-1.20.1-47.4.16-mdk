package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * ColoredNpcHandRenderer — Portado a 1.20.1 / GeckoLib 4.
 * REPARADO: Ahora hereda de GeoEntityRenderer y usa la lógica de GL4.
 */
// 🚨 REPARACIÓN: Agregamos el genérico <T> para que acepte cualquier NPC
public abstract class ColoredNpcHandRenderer<T extends BaseNpcEntity> extends GeoEntityRenderer<T> {

    protected final HashMap<String, Vec3i> colorCache = new HashMap<>();
    protected static final Vec3i WHITE = new Vec3i(255, 255, 255);

    protected ColoredNpcHandRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
        this.shadowRadius = 0.5F;
    }

    // ── API de Color ─────────────────────────────────────────────────────────

    protected Vec3i getBoneColor(T entity, GeoBone bone) {
        if (entity == null) return WHITE;
        return colorCache.computeIfAbsent(bone.getName(), this::getBoneColorByName);
    }

    protected abstract Vec3i getBoneColorByName(String boneName);

    // ── Renderizado con Colores (GeckoLib 4 Style) ──────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, net.minecraft.client.renderer.RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        // 🚨 AQUÍ ES DONDE SUCEDE LA MAGIA DEL COLOR:
        Vec3i color = getBoneColor(animatable, bone);

        // Multiplicamos el color del hueso por el color que ya trae (daño, luz, etc.)
        float r = red * (color.getX() / 255.0f);
        float g = green * (color.getY() / 255.0f);
        float b = blue * (color.getZ() / 255.0f);

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, alpha);
    }

    // ── Lógica de Huesos (Reemplazo de onBoneProcess) ────────────────────────

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // Aquí puedes aplicar rotaciones globales o escalas
        if (animatable.isPassenger()) {
            // En GL4 es mejor mover los huesos en el MODELO, pero si quieres hacerlo aquí:
            model.getBone("legL").ifPresent(bone -> bone.setPosZ(bone.getPosZ() + 1.0F));
            model.getBone("legR").ifPresent(bone -> bone.setPosZ(bone.getPosZ() + 1.0F));
        }

        // Limpiamos el caché cada frame para que cambie la ropa en tiempo real
        colorCache.clear();
    }

    // ── Helpers Visuales ─────────────────────────────────────────────────────

    protected boolean isHoldingBow(T entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof BowItem;
    }

    protected boolean isHoldingShield(T entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof ShieldItem;
    }
}