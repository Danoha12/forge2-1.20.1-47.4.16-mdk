package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.util.NpcColorData;
import com.trolmastercard.sexmod.util.NpcSkinTexture;
import com.trolmastercard.sexmod.util.ClothingOverlayBones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;

/**
 * GoblinBodyRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador del cuerpo del Goblin.
 * * Maneja el tintado de huesos para simular armaduras y oculta partes
 * del cuerpo cuando es cargado por el jugador (Shoulder-ride).
 */
public class GoblinBodyRenderer extends NpcBodyRenderer<GoblinEntity> {

    static final HashSet<String> TINTABLE_BONES = new HashSet<>(Arrays.asList(
            "kneeL", "kneeR", "shinL", "shinR", "armorHelmet",
            "sockL", "sockR", "braBoobL", "braBoobR",
            "armorNippleR", "armorNippleL", "slip", "turnable", "static"
    ));

    public static final HashSet<String> PHYSICS_ONLY_BONES = new HashSet<>(Arrays.asList(
            "boobs", "booty", "vagina", "fuckhole", "preggy",
            "LegL", "LegR", "cheekR", "cheekL"
    ));

    public GoblinBodyRenderer(EntityRendererProvider.Context ctx, GeoModel<GoblinEntity> model) {
        super(ctx, model);
    }

    // ── Sobrescritura de Textura de Piel ──────────────────────────────────────

    @Nullable
    @Override
    protected NpcSkinTexture getSkinTexture(GoblinEntity entity) {
        try {
            // Asumiendo que FakeWorld fue reemplazado o manejado en 1.20.1
            // if (entity.level() instanceof FakeWorld) return null;
            if (entity.getCarrierUUID() != null) return null; // isCarried
        } catch (RuntimeException ignored) {}

        return NpcColorData.DEFAULT_TEXTURE;
    }

    // ── Huesos Ocultos Base ───────────────────────────────────────────────────

    @Override
    protected HashSet<String> getHiddenBones() {
        HashSet<String> set = new HashSet<>(NpcColorData.getHiddenBones());
        set.addAll(ClothingOverlayBones.ALL);
        return set;
    }

    // ── Render Principal (Lógica Shoulder-Ride) ───────────────────────────────

    @Override
    public void render(GoblinEntity entity, float yaw, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light) {
        try {
            super.render(entity, yaw, partialTick, ps, buf, light);
        } catch (RuntimeException ignored) {}

        // Ocultar si está en primera persona y el jugador local lo está cargando
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.player != null) {
            try {
                var localUUID = mc.player.getGameProfile().getId();
                if (entity instanceof PlayerKoboldEntity pk && localUUID.equals(pk.getOwnerUUID())) {
                    if (!entity.isSexModeActive()) return;
                }
            } catch (RuntimeException ignored) {}
        }

        NpcColorData.applyOutlineTinting(entity, partialTick);
    }

    // ── Intercepción de Huesos (Tintado y Ocultamiento) ───────────────────────

    @Override
    public void renderRecursively(PoseStack ps, GoblinEntity entity, GeoBone bone,
                                  RenderType renderType, MultiBufferSource buf, VertexConsumer vc,
                                  boolean isReRender, float partialTick, int light, int overlay,
                                  float r, float g, float b, float a) {

        String name = bone.getName();

        // 1. Ocultar huesos de físicas si está siendo cargado
        if (entity.getCarrierUUID() != null && PHYSICS_ONLY_BONES.contains(name)) {
            return;
        }

        // 2. Aplicar tinte y UV Offset a huesos de armadura
        if (TINTABLE_BONES.contains(name)) {
            float[] tint = computeBoneTint(entity, name, r, g, b);
            if (tint != null) {
                r = tint[0];
                g = tint[1];
                b = tint[2];
                // tint[3] es el UV offset, tu NpcBodyRenderer debe procesarlo
            }
        }

        super.renderRecursively(ps, entity, bone, renderType, buf, vc,
                isReRender, partialTick, light, overlay, r, g, b, a);
    }

    // ── Cálculo del Tinte de Armadura ─────────────────────────────────────────

    @Nullable
    private float[] computeBoneTint(GoblinEntity entity, String boneName, float r, float g, float b) {
        if ("armorHelmet".equals(boneName)) {
            return super.computeDefaultTint(boneName, r, g, b);
        }

        ItemStack stack = ItemStack.EMPTY;

        // Mapear el hueso al slot de inventario correspondiente
        switch (boneName) {
            case "braBoobL", "braBoobR", "armorNippleR", "armorNippleL" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.CHEST_ITEM);
            case "turnable", "static", "slip" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.LEG_ITEM);
            case "kneeL", "kneeR", "shinL", "shinR", "sockL", "sockR" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.FEET_ITEM);
        }

        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return new float[]{r, g, b, 0.0F};
        }

        // Armadura de Cuero (Tintable)
        if (armor instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(stack);
            float cr = ((color >> 16) & 0xFF) / 255.0F;
            float cg = ((color >> 8) & 0xFF) / 255.0F;
            float cb = (color & 0xFF) / 255.0F;
            return new float[]{r * cr, g * cg, b * cb, -0.09375F};
        }

        // Offset UV para otros materiales de Vanilla
        float uvOffset = -0.1875F; // Default

        if (armor.getMaterial() == ArmorMaterials.IRON) {
            uvOffset = -0.15625F;
        } else if (armor.getMaterial() == ArmorMaterials.GOLD ||
                armor.getMaterial() == ArmorMaterials.DIAMOND ||
                armor.getMaterial() == ArmorMaterials.NETHERITE) {
            uvOffset = -0.125F;
        }

        return new float[]{r, g, b, uvOffset};
    }
}