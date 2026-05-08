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
    protected NpcSkinTexture getSkinTexture(GoblinEntity entity) {
        try {
            // 1. Si no hay nivel, es que estamos en un menú (el reemplazo de FakeWorld)
            if (entity.level() == null) return NpcColorData.DEFAULT_TEXTURE;

            // 2. Si la están cargando en el hombro, a veces conviene no procesar skin
            if (entity.getCarrierUUID() != null) return null;

            // 3. ¡LA MAGIA! Intentamos sacar el UUID del dueño o de quien la spawneó
            java.util.UUID skinUUID = entity.getOwnerUUID();

            // Si no hay dueño, usamos el del jugador local como prueba
            if (skinUUID == null && Minecraft.getInstance().player != null) {
                skinUUID = Minecraft.getInstance().player.getGameProfile().getId();
            }

            // 4. Si de plano no hay UUID, ahí sí mandamos la default
            if (skinUUID == null) return NpcColorData.DEFAULT_TEXTURE;

            // 5. Buscamos en el archivero (Caché) que creamos hace rato
            NpcSkinTexture cached = NpcSkinTexture.getCache().get(skinUUID);
            if (cached != null) return cached;

            // 6. Si no estaba en el archivero, la mandamos a fabricar
            return NpcColorData.loadSkinTexture(skinUUID, entity.level());

        } catch (Exception e) {
            // Si algo truena (como que no haya internet para bajar la skin),
            // devolvemos la default para que no crashee el juego
            return NpcColorData.DEFAULT_TEXTURE;
        }
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
                if (localUUID.equals(entity.getOwnerUUID())) {
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

        // Ocultamiento base
        if (NpcColorData.getHiddenBones().contains(name) || ClothingOverlayBones.ALL.contains(name)) return;
        if (entity.getCarrierUUID() != null && PHYSICS_ONLY_BONES.contains(name)) return;

        // 🚨 CONFIGURACIÓN DEL ADAPTADOR
        float uvOffset = 0.0F;
        VertexConsumer activeConsumer = vc; // Por defecto usamos el normal

        // 2. Aplicar tinte y UV Offset a huesos de armadura
        if (TINTABLE_BONES.contains(name)) {
            float[] tint = computeBoneTint(entity, name, r, g, b);
            if (tint != null) {
                r = tint[0];
                g = tint[1];
                b = tint[2];
                uvOffset = tint[3]; // 👈 ¡Ahora sí guardamos el offset!

                // Si el offset no es cero, activamos el "Adaptador"
                if (uvOffset != 0.0F) {
                    activeConsumer = new UVOffsetVertexConsumer(vc, 0, uvOffset);
                }
            }
        }

        // 🚨 REPARADO: Pasamos el 'activeConsumer' en lugar del 'vc' original
        super.renderRecursively(ps, entity, bone, renderType, buf, activeConsumer,
                isReRender, partialTick, light, overlay, r, g, b, a);
    }

    // ── Cálculo del Tinte de Armadura ─────────────────────────────────────────

    // ── Cálculo del Tinte de Armadura (Versión Corregida) ─────────────────────

    @Nullable
    private float[] computeBoneTint(GoblinEntity entity, String boneName, float r, float g, float b) {
        ItemStack stack = ItemStack.EMPTY;

        // 1. Mapeamos el hueso al slot de inventario (🚨 Agregamos el Casco aquí)
        switch (boneName) {
            case "armorHelmet" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.HELMET_ITEM);
            case "braBoobL", "braBoobR", "armorNippleR", "armorNippleL" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.CHEST_ITEM);
            case "turnable", "static", "slip" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.LEG_ITEM);
            case "kneeL", "kneeR", "shinL", "shinR", "sockL", "sockR" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.FEET_ITEM);
        }

        // 2. Si no es un hueso de armadura o el slot está vacío, color normal sin offset
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            return new float[]{r, g, b, 0.0F};
        }

        // 3. Armadura de Cuero (Tintable)
        if (armor instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(stack);
            float cr = ((color >> 16) & 0xFF) / 255.0F;
            float cg = ((color >> 8) & 0xFF) / 255.0F;
            float cb = (color & 0xFF) / 255.0F;
            return new float[]{r * cr, g * cg, b * cb, -0.09375F};
        }

        // 4. Offset UV para otros materiales (Hierro, Oro, Diamante...)
        float uvOffset = -0.1875F; // Default (Cota de malla / Otros)

        if (armor.getMaterial() == ArmorMaterials.IRON) {
            uvOffset = -0.15625F;
        } else if (armor.getMaterial() == ArmorMaterials.GOLD ||
                armor.getMaterial() == ArmorMaterials.DIAMOND ||
                armor.getMaterial() == ArmorMaterials.NETHERITE) {
            uvOffset = -0.125F;
        }

        return new float[]{r, g, b, uvOffset};
    }
// ── Clase Ayudante para mover las UVs (Texturas) ─────────────────────────

    private record UVOffsetVertexConsumer(VertexConsumer delegate, float uOffset, float vOffset) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) { return delegate.vertex(x, y, z); }
        @Override
        public VertexConsumer color(int r, int g, int b, int a) { return delegate.color(r, g, b, a); }
        @Override
        public VertexConsumer uv(float u, float v) {
            // 🚨 AQUÍ SUCEDE LA MAGIA: Sumamos el desplazamiento a la textura
            return delegate.uv(u + uOffset, v + vOffset);
        }
        @Override
        public VertexConsumer overlayCoords(int u, int v) { return delegate.overlayCoords(u, v); }
        @Override
        public VertexConsumer uv2(int u, int v) { return delegate.uv2(u, v); }
        @Override
        public VertexConsumer normal(float x, float y, float z) { return delegate.normal(x, y, z); }
        @Override
        public void endVertex() { delegate.endVertex(); }
        @Override
        public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }
        @Override
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}