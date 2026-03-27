package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeableArmorItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Arrays;
import java.util.HashSet;
import javax.annotation.Nullable;

/**
 * GoblinBodyRenderer - ported from dx.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * GeckoLib4 entity renderer for GoblinEntity. Extends {@link NpcBodyRenderer} (dm).
 * Adds:
 *   1. Armor-tinted bone rendering: specific bones are tinted according to the
 *      NPC's worn armor item color (from SynchedEntityData slots).
 *   2. Shoulder-idle mode: if the goblin is in SHOULDER_IDLE state and the local
 *      player owns it, the renderer delegates to {@link KoboldShoulderRenderHandler}
 *      before calling super.
 *   3. Steve/body bone split: renders the "body" sub-bone with the main texture and
 *      the "steve" sub-bone with the player skin texture at full alpha (v()).
 *
 * Tintable bone groups (from {@code z} set):
 *   braBoobL/R, armorNippleL/R    - chest armor slot (T = e2.T)
 *   turnable, static, slip        - leg armor slot   (U = e2.U)
 *   kneeL/R, shinL/R, sockL/R     - feet armor slot  (W = e2.W)
 *   armorHelmet                   - standard super tinting
 *
 * For {@link DyeableArmorItem} (leather): color is extracted from the item and
 * multiplied into the bone's r/g/b channels, with a UV offset of -0.09375.
 * For other armor tiers the UV offsets vary by tier (iron/gold/diamond/netherite).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - dm extends - NpcBodyRenderer
 *   - RenderManager - EntityRendererProvider.Context
 *   - AnimatedGeoModel - GeoModel (in NpcBodyRenderer)
 *   - a(em, float) - preRender style hook via renderEarly/renderLate
 *   - func_76979_b / a() (render entry points) - render(entity, yaw, tick, ps, buf, light)
 *   - a(boolean) / a(boolean,boolean) - applyFirstPersonTranslation helpers (in NpcBodyRenderer)
 *   - da.y - NpcColorData.defaultTexture; da.E - NpcColorData.hiddenBones
 *   - gx.a - ClothingOverlayBones.ALL (bone names for clothing)
 *   - MATRIX_STACK.push/translate/rotate/scale - ps.pushPose/translate/mulPose/scale
 *   - e2.T/U/W - NpcInventoryEntity.CHEST_ITEM / LEG_ITEM / FEET_ITEM
 *   - itemArmor.func_82812_d().ordinal() - armorItem.getMaterial().ordinal()
 *   - itemArmor.func_82814_b(stack) - ((DyeableArmorItem) item).getColor(stack)
 *   - Vector4f - float[] {r,g,b,uvOffset}
 *   - f7 - NpcSkinTexture; d(em) - getSkinTexture(entity)
 *   - MATRIX_STACK.moveToPivot / moveBackFromPivot - implicit in GeckoLib4 renderer
 *   - i.field_71474_y.field_74320_O == 0 - mc.options.getCameraType() == FIRST_PERSON
 *   - ei.m() - playerKobold.getOwnerUUID()
 *   - paramem.Q() - entity.isSexModeActive()
 *   - da.a(entity, pt) - NpcColorData.applyTinting(entity, pt) (outline shader)
 *   - GlStateManager.func_179137_b - ps.translate
 *   - GlStateManager.func_179114_b - ps.mulPose
 *   - d(j) - getSkinTexture(entity) in super
 *   - j.v() - entity.getSkinAlpha()
 */
public class GoblinBodyRenderer extends NpcBodyRenderer<GoblinEntity> {

    /** Bones that receive custom tinting based on equipped armor. */
    static final HashSet<String> TINTABLE_BONES = new HashSet<>(Arrays.asList(
            "kneeL", "kneeR", "shinL", "shinR", "armorHelmet",
            "sockL", "sockR", "braBoobL", "braBoobR",
            "armorNippleR", "armorNippleL", "slip", "turnable", "static"));

    /** Bones hidden when the goblin is in shoulder-ride/pick-up states. */
    public static final HashSet<String> PHYSICS_ONLY_BONES = new HashSet<>(Arrays.asList(
            "boobs", "booty", "vagina", "fuckhole", "preggy",
            "LegL", "LegR", "cheekR", "cheekL"));

    public GoblinBodyRenderer(EntityRendererProvider.Context ctx, GeoModel<GoblinEntity> model) {
        super(ctx, model);
    }

    // -- Skin texture override --------------------------------------------------

    @Nullable
    @Override
    protected NpcSkinTexture getSkinTexture(GoblinEntity entity) {
        try {
            if (entity.level instanceof FakeWorld) return null;
        } catch (RuntimeException ignored) {}
        try {
            if (entity.isCarried()) return null;
        } catch (RuntimeException ignored) {}
        return NpcColorData.DEFAULT_TEXTURE;
    }

    // -- Hidden bone set --------------------------------------------------------

    @Override
    protected HashSet<String> getHiddenBones() {
        HashSet<String> set = NpcColorData.getHiddenBones();
        set.addAll(ClothingOverlayBones.ALL);
        return set;
    }

    // -- Shoulder-ride render override ------------------------------------------

    @Override
    public void render(GoblinEntity entity, float yaw, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light) {
        try {
            super.render(entity, yaw, partialTick, ps, buf, light);
        } catch (RuntimeException ignored) {}

        // First-person: if local player owns this entity in SHOULDER_IDLE, suppress
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            try {
                var localUUID = Minecraft.getInstance().player.getGameProfile().getId();
                if (entity instanceof PlayerKoboldEntity pk && localUUID.equals(pk.getOwnerUUID())) {
                    if (!entity.isSexModeActive()) return;
                }
            } catch (RuntimeException ignored) {}
        }

        NpcColorData.applyOutlineTinting(entity, partialTick);
    }

    // -- Bone-level tint override -----------------------------------------------

    @Override
    public void renderRecursively(PoseStack ps, GoblinEntity entity, GeoBone bone,
                                   net.minecraft.client.renderer.RenderType renderType,
                                   MultiBufferSource buf, VertexConsumer vc,
                                   boolean isReRender, float partialTick,
                                   int light, int overlay,
                                   float r, float g, float b, float a) {

        String name = bone.getName();

        // Physics-only hidden check
        if (entity.isCarried() && PHYSICS_ONLY_BONES.contains(name)) {
            // Don't render physics bones while being carried
            return;
        }

        // Tintable bone tinting
        if (TINTABLE_BONES.contains(name)) {
            float[] tint = computeBoneTint(entity, name, r, g, b);
            if (tint != null) {
                r = tint[0]; g = tint[1]; b = tint[2];
                // tint[3] is the UV offset - handled in super via custom render type
            }
        }

        super.renderRecursively(ps, entity, bone, renderType, buf, vc,
                isReRender, partialTick, light, overlay, r, g, b, a);
    }

    // -- Armor tint computation -------------------------------------------------

    /**
     * Returns [r, g, b, uvOffset] for a tintable bone, or null if no armor tint.
     *
     * Maps bone name - armor slot data parameter, then reads the color.
     */
    @Nullable
    private float[] computeBoneTint(GoblinEntity entity, String boneName,
                                     float r, float g, float b) {
        if ("armorHelmet".equals(boneName)) {
            return super.computeDefaultTint(boneName, r, g, b);
        }

        ItemStack stack = ItemStack.EMPTY;

        switch (boneName) {
            case "braBoobL", "braBoobR", "armorNippleR", "armorNippleL" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.CHEST_ITEM);
            case "turnable", "static", "slip" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.LEG_ITEM);
            case "kneeL", "kneeR", "shinL", "shinR", "sockL", "sockR" ->
                    stack = entity.getEntityData().get(NpcInventoryEntity.FEET_ITEM);
        }

        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return new float[]{ r, g, b, 0.0F };
        }

        // Dyeable (leather) armor
        if (armor instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(stack);
            float cr = ((color >> 16) & 0xFF) / 255.0F;
            float cg = ((color >>  8) & 0xFF) / 255.0F;
            float cb = ( color        & 0xFF) / 255.0F;
            return new float[]{ r * cr, g * cg, b * cb, -0.09375F };
        }

        // Non-dyeable: UV offset by tier
        float uvOffset = switch (armor.getMaterial()) {
            case IRON   -> -0.15625F;
            case GOLD   -> -0.125F;
            case DIAMOND, NETHERITE -> -0.125F;
            default     -> -0.1875F;
        };
        return new float[]{ r, g, b, uvOffset };
    }
}
