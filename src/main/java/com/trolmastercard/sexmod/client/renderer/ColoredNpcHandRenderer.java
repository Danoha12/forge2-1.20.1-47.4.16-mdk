package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Ported from d9.java (1.12.2 - 1.20.1)
 * Abstract NpcHandRenderer subclass that adds per-bone colour tinting with
 * a static UUID+boneName keyed cache.
 *
 * Subclasses must implement {@link #getBoneColorByName(String)} to return
 * the Vec3i (r,g,b 0-255) for a given bone name.
 *
 * Additional utilities:
 *  - {@link #showOnlyChildAtIndex(CoreGeoBone, int)} / {@link #getChildAtIndex(CoreGeoBone, int)}
 *    for child-bone selection (e.g. hat variants).
 *  - {@link #getItemDisplayAngle()} / {@link #getItemDisplayOffset(ItemStack)} overridable item pose.
 *  - Bow aiming logic: pitch applied to armR/armL when bow is held.
 *  - Shield logic: fixed pose when shield is held.
 *
 * Original: {@code abstract class d9 extends dm}
 */
public abstract class ColoredNpcHandRenderer extends NpcHandRenderer {

    // -- Static cache: (boneName.hashCode + entity UUID.hashCode) - Vec3i -----
    protected static final HashMap<Integer, Vec3i> COLOR_CACHE = new HashMap<>();

    /** White fallback colour. */
    protected static final Vec3i WHITE = new Vec3i(255, 255, 255);

    // -- Constructor -----------------------------------------------------------

    public ColoredNpcHandRenderer(EntityRendererProvider.Context context,
                                   GeoModel<BaseNpcEntity> model,
                                   double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // -- Colour API ------------------------------------------------------------

    /**
     * Returns the cached Vec3i colour for the given bone, keyed on
     * (boneName.hashCode + entityUUID.hashCode). Falls back to
     * {@link #getBoneColorByName(String)} on cache miss.
     */
    protected Vec3i getBoneColor(CoreGeoBone bone) {
        if (entity == null) return WHITE;
        String name = bone.getName();
        int key = name.hashCode() + entity.getUUID().hashCode();
        Vec3i cached = COLOR_CACHE.get(key);
        if (cached != null) return cached;
        Vec3i color = getBoneColorByName(name);
        COLOR_CACHE.put(key, color);
        return color;
    }

    /**
     * Subclasses return the appropriate Vec3i (r,g,b 0-255) for the bone.
     * Called on cache miss; result is stored for subsequent frames.
     */
    protected abstract Vec3i getBoneColorByName(String boneName);

    /** Clears the entire colour cache (call when entity model changes). */
    public static void clearColorCache() {
        COLOR_CACHE.clear();
    }

    // -- Child-bone selection helpers ------------------------------------------

    /**
     * Shows only the child bone at {@code index}, hiding all others.
     * Children are iterated in list order; no sorting.
     */
    protected void showOnlyChildAtIndex(CoreGeoBone parent, int index) {
        List<CoreGeoBone> children = parent.getChildBones();
        for (int i = 0; i < children.size(); i++) {
            CoreGeoBone child = children.get(i);
            if (i == index) {
                child.setHidden(false);
                return;
            }
        }
    }

    /**
     * Shows the child bone at {@code index} (sorted by pivot Y ascending), hides the rest.
     * Returns the shown bone, or null if index out of range.
     */
    protected CoreGeoBone getChildAtIndex(CoreGeoBone parent, int index) {
        List<CoreGeoBone> sorted = parent.getChildBones().stream()
                .sorted(Comparator.comparingDouble(CoreGeoBone::getPivotY))
                .toList();
        CoreGeoBone result = null;
        for (int i = 0; i < sorted.size(); i++) {
            CoreGeoBone child = sorted.get(i);
            if (i == index) {
                child.setHidden(false);
                result = child;
            } else {
                child.setHidden(true);
            }
        }
        return result;
    }

    // -- Item display overrides ------------------------------------------------

    /** Default item display rotation angle (degrees). Subclasses may override. */
    protected float getItemDisplayAngle() { return 1.0F; }

    /**
     * Override the item display offset for the weapon bone.
     * Returns XYZ rotation (degrees) applied before the item is rendered.
     * Default: Vec3(-90, 0, 0).
     */
    protected Vec3 getItemDisplayOffset(ItemStack item) {
        return new Vec3(-90.0, 0.0, 0.0);
    }

    // -- Bone processing (called during renderRecursively / renderBone) --------

    /**
     * Called for each bone during traversal. Applies:
     *  - Sitting offset for upperBody/head/legs.
     *  - Bow aiming for armR/armL when bow is held.
     *  - Shield pose for armR/armL when shield is held.
     *  - Delegates to {@link #onBoneProcess(String, CoreGeoBone)} for subclass hooks.
     */
    @Override
    protected void onBoneProcess(String boneName, CoreGeoBone bone) {
        if (entity == null) return;

        // -- Sitting adjustments -----------------------------------------------
        if (entity.isSitting()) {
            switch (boneName) {
                case "upperBody" -> bone.setRotX(bone.getRotX() - 0.5F);
                case "head"      -> bone.setRotX(bone.getRotX() + 0.5F);
                case "legL", "legR" -> bone.setPosZ(bone.getPosZ() + 1.0F);
            }
        }

        boolean holdsBow    = isHoldingBow();
        boolean holdsShield = isHoldingShield();

        // -- Bow aiming --------------------------------------------------------
        if (holdsBow || isOffhandBow()) {
            if ("armR".equals(boneName)) {
                bone.setRotX(bone.getRotX() - entity.getXRot() / 50.0F);
            }
            if ("armL".equals(boneName)) {
                bone.setRotY(bone.getRotY() - entity.getXRot() / 50.0F);
            }
            // Swap main/offhand items if offhand carries the bow
            if (isOffhandBow()) swapHands();
        }

        // -- Shield pose -------------------------------------------------------
        if (holdsShield) {
            if ("armR".equals(boneName) && isMainHandShield()) {
                bone.setRotZ(0.0F);
                bone.setRotX(0.5F);
            }
            if ("armL".equals(boneName) && isOffhandShield()) {
                bone.setRotZ(0.0F);
                bone.setRotX(0.5F);
            }
        }

        // Delegate to subclass
        super.onBoneProcess(boneName, bone);
    }

    // -- Helpers ---------------------------------------------------------------

    private boolean isHoldingBow() {
        ItemStack main = entity.getMainHandItem();
        return main.getItem() instanceof net.minecraft.world.item.BowItem;
    }

    private boolean isOffhandBow() {
        ItemStack off = entity.getOffhandItem();
        return off.getItem() instanceof net.minecraft.world.item.BowItem;
    }

    private boolean isMainHandShield() {
        return entity.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
    }

    private boolean isOffhandShield() {
        return entity.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
    }

    private boolean isHoldingShield() {
        return isMainHandShield() || isOffhandShield();
    }

    /** Swap main-hand and offhand items (for rendering bow from offhand). */
    private void swapHands() {
        ItemStack main = entity.getMainHandItem();
        ItemStack off  = entity.getOffhandItem();
        entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, off);
        entity.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, main);
    }

    // -- Armour bone UV offset -------------------------------------------------

    /**
     * Returns the UV offset for armour bones (applied when bone name starts with "armor").
     * Non-armour bones - 0.0 (no offset).
     * Requires entity to implement {@link NpcInventoryEntity}.
     */
    @Override
    protected double getUvOffsetForBone(String boneName,
                                        float red, float green, float blue) {
        if (!boneName.startsWith("armor")) return 0.0;
        if (!(entity instanceof NpcInventoryEntity e2)) return 0.0;
        if (e2.getArmorSlotCount() == 0) return 0.0;

        GeoModel<?> modelProvider = getGeoModel();
        if (!(modelProvider instanceof BaseNpcModel<?> baseModel)) return 0.0;

        ItemStack armorStack = baseModel.getArmorItemForBone(entity, boneName);
        if (!(armorStack.getItem() instanceof net.minecraft.world.item.ArmorItem armor)) return 0.0;

        net.minecraft.world.item.ArmorItem.Type tier = armor.getType();
        float f = switch (tier) {
            case HELMET    -> 1.0F;
            case CHESTPLATE, LEGGINGS -> 2.0F;
            case BOOTS     -> 4.0F;
            default        -> 0.0F;
        };

        // Leather - apply dye colour tint
        if (armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.LEATHER) {
            int color = net.minecraft.world.item.DyeableLeatherItem.getColor(armorStack);
            float r2 = ((color >> 16) & 0xFF) / 255.0F;
            float g2 = ((color >>  8) & 0xFF) / 255.0F;
            float b2 = ( color        & 0xFF) / 255.0F;
            // Tint is applied in the render pipeline; f remains 4.0
        }

        return 72.0F * f / 4096.0F;
    }
}
