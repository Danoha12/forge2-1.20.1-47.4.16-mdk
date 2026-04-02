package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.util.NpcSkinLoader;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;
import java.util.UUID;

/**
 * BaseNpcRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja texturas dinámicas, huesos ocultos y visibilidad.
 */
@OnlyIn(Dist.CLIENT)
public abstract class BaseNpcRenderer<T extends BaseNpcEntity> extends GeoEntityRenderer<T> {

    protected static final Minecraft mc = Minecraft.getInstance();
    protected final HashSet<String> hiddenBoneSet = new HashSet<>();
    protected T currentEntity;

    protected BaseNpcRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    // ── Gestión de Texturas ──────────────────────────────────────────────────

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        UUID ownerUUID = entity.getMasterUUID();

        // Si tiene un dueño, usamos nuestro cargador asíncrono
        if (ownerUUID != null) {
            return NpcSkinLoader.getOrCreateSkin(ownerUUID);
        }

        // Si no, recurrimos a la textura por defecto del modelo
        return super.getTextureLocation(entity);
    }

    // ── Pipeline de Renderizado GeckoLib 4 ──────────────────────────────────

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.currentEntity = entity;

        // Comprobación de visibilidad (Line of Sight)
        if (mc.player != null && !entity.isNoAi()) {
            if (!isVisible(entity, mc.player)) return;
        }

        // Actualizamos los huesos ocultos antes de renderizar
        this.hiddenBoneSet.clear();
        this.hiddenBoneSet.addAll(buildHiddenBoneSet(entity));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Actualizar posiciones de huesos para el sistema de partículas/físicas
        updateBoneMatrices(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // Aplicar ocultamiento de huesos
        if (this.hiddenBoneSet.contains(name)) {
            bone.setHidden(true);
            return; // No renderizamos este hueso ni sus hijos
        } else {
            bone.setHidden(false);
        }

        // Aplicar color personalizado por hueso (Blanco por defecto)
        net.minecraft.core.Vec3i color = getBoneColor(name);
        float r = (color.getX() / 255.0f) * red;
        float g = (color.getY() / 255.0f) * green;
        float b = (color.getZ() / 255.0f) * blue;

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, alpha);
    }

    // ── Lógica de Visibilidad y Huesos ───────────────────────────────────────

    protected HashSet<String> buildHiddenBoneSet(T entity) {
        // Aquí puedes añadir lógica para ocultar ropa o partes según el estado
        return new HashSet<>();
    }

    protected net.minecraft.core.Vec3i getBoneColor(String boneName) {
        return new net.minecraft.core.Vec3i(255, 255, 255);
    }

    protected boolean isVisible(T npc, Player player) {
        // Si el jugador está en tercera persona o es un Kobold, siempre es visible
        if (mc.options.getCameraType().isFirstPerson()) {
            Vec3 npcPos = npc.position().add(0, npc.getBbHeight() / 2, 0);
            Vec3 camPos = player.getEyePosition(1.0f);

            BlockHitResult result = npc.level().clip(new ClipContext(
                    camPos, npcPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player));

            return result.getType() == HitResult.Type.MISS || result.getType() == HitResult.Type.ENTITY;
        }
        return true;
    }

    protected void updateBoneMatrices(T entity) {
        // Sincroniza las posiciones de los huesos de GeckoLib con la entidad lógica
        this.model.getBone("root").ifPresent(bone -> {
            // Lógica para actualizar colisiones o partes de la entidad si es necesario
        });
    }
}