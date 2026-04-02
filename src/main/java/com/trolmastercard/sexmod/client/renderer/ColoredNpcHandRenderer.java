package com.trolmastercard.sexmod.client.renderer; // Ajustar paquete si es necesario

import com.trolmastercard.sexmod.NpcHandRenderer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.client.model.BaseNpcModel; // Descomenta cuando portes el modelo base
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * ColoredNpcHandRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador abstracto que permite tintar huesos individuales y aplicar
 * * cinemática inversa (IK) manual para arcos y escudos.
 */
public abstract class ColoredNpcHandRenderer extends NpcHandRenderer {

    // ── Caché de Color (Por Instancia, NO Estática) ──────────────────────────
    // Se limpia automáticamente por frame o cuando la entidad cambia su NBT de ropa.
    protected final HashMap<String, Vec3i> colorCache = new HashMap<>();

    protected static final Vec3i WHITE = new Vec3i(255, 255, 255);

    // ── Constructor ──────────────────────────────────────────────────────────

    public ColoredNpcHandRenderer(EntityRendererProvider.Context context, GeoModel<BaseNpcEntity> model, double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // ── API de Color ─────────────────────────────────────────────────────────

    /**
     * Devuelve el color RGB cacheados para el hueso.
     * En GeckoLib 4, esto usualmente se aplica en el render recursivo multiplicando
     * los valores R, G, B en el PoseStack o en el VertexConsumer.
     */
    protected Vec3i getBoneColor(GeoBone bone) {
        if (animatable == null) return WHITE; // En GL4, 'animatable' es la entidad

        String name = bone.getName();
        return colorCache.computeIfAbsent(name, this::getBoneColorByName);
    }

    protected abstract Vec3i getBoneColorByName(String boneName);

    public void clearColorCache() {
        colorCache.clear();
    }

    // ── Utilidades de Huesos Hijos ───────────────────────────────────────────

    protected void showOnlyChildAtIndex(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).setHidden(i != index);
        }
    }

    protected GeoBone getChildAtIndex(GeoBone parent, int index) {
        List<GeoBone> sorted = parent.getChildBones().stream()
                .sorted(Comparator.comparingDouble(GeoBone::getPivotY))
                .toList();

        GeoBone result = null;
        for (int i = 0; i < sorted.size(); i++) {
            GeoBone child = sorted.get(i);
            child.setHidden(i != index);
            if (i == index) result = child;
        }
        return result;
    }

    // ── Offsets de Objetos ───────────────────────────────────────────────────

    protected float getItemDisplayAngle() { return 1.0F; }

    protected Vec3 getItemDisplayOffset(ItemStack item) {
        return new Vec3(-90.0, 0.0, 0.0);
    }

    // ── Procesamiento de Huesos (Físicas Visuales) ───────────────────────────

    @Override
    protected void onBoneProcess(String boneName, GeoBone bone) {
        if (animatable == null) return;

        // ── Sentado ──
        if (animatable.isSitting()) {
            switch (boneName) {
                case "upperBody" -> bone.setRotX(bone.getRotX() - 0.5F);
                case "head"      -> bone.setRotX(bone.getRotX() + 0.5F);
                case "legL", "legR" -> bone.setPosZ(bone.getPosZ() + 1.0F);
            }
        }

        boolean mainBow = isHoldingBow(InteractionHand.MAIN_HAND);
        boolean offBow = isHoldingBow(InteractionHand.OFF_HAND);
        boolean holdsShield = isHoldingShield();

        // ── Apuntar Arco ──
        if (mainBow || offBow) {
            float pitchOffset = animatable.getXRot() / 50.0F;

            // Si el arco está en la mano secundaria (izquierda), rotamos el brazo izquierdo.
            // Si está en la principal (derecha), rotamos el brazo derecho.
            if ("armR".equals(boneName) && mainBow) {
                bone.setRotX(bone.getRotX() - pitchOffset);
            } else if ("armL".equals(boneName) && offBow) {
                bone.setRotY(bone.getRotY() - pitchOffset); // Podría ser RotX dependiendo del rig del modelo
            }
        }

        // ── Bloqueo con Escudo ──
        if (holdsShield) {
            if ("armR".equals(boneName) && isHoldingShield(InteractionHand.MAIN_HAND)) {
                bone.setRotZ(0.0F);
                bone.setRotX(0.5F);
            } else if ("armL".equals(boneName) && isHoldingShield(InteractionHand.OFF_HAND)) {
                bone.setRotZ(0.0F);
                bone.setRotX(0.5F);
            }
        }

        // No llamamos a super.onBoneProcess porque ColoredNpcHandRenderer es el que define la lógica principal
        // Si NpcHandRenderer tiene lógica esencial, descomenta la siguiente línea:
        // super.onBoneProcess(boneName, bone);
    }

    // ── Helpers Visuales Seguros ─────────────────────────────────────────────

    private boolean isHoldingBow(InteractionHand hand) {
        return animatable.getItemInHand(hand).getItem() instanceof BowItem;
    }

    private boolean isHoldingShield(InteractionHand hand) {
        return animatable.getItemInHand(hand).getItem() instanceof ShieldItem;
    }

    private boolean isHoldingShield() {
        return isHoldingShield(InteractionHand.MAIN_HAND) || isHoldingShield(InteractionHand.OFF_HAND);
    }

    // ── Offset UV para Armaduras ─────────────────────────────────────────────

    // Si tu NpcHandRenderer base define este método, déjalo con @Override.
    // Si no, puedes quitarle la anotación.
    protected double getUvOffsetForBone(String boneName, float red, float green, float blue) {
        if (!boneName.startsWith("armor") || !(animatable instanceof NpcInventoryEntity e2) || e2.getArmorSlotCount() == 0) {
            return 0.0;
        }

        // Asegúrate de que el modelo base tenga este método
        /*
        if (!(getGeoModel() instanceof BaseNpcModel<?> baseModel)) return 0.0;
        ItemStack armorStack = baseModel.getArmorItemForBone(animatable, boneName);

        if (!(armorStack.getItem() instanceof ArmorItem armor)) return 0.0;

        float f = switch (armor.getType()) {
            case HELMET -> 1.0F;
            case CHESTPLATE, LEGGINGS -> 2.0F;
            case BOOTS -> 4.0F;
            default -> 0.0F;
        };

        // En 1.20.1, las armaduras de cuero teñibles implementan DyeableArmorItem
        if (armor.getMaterial() == ArmorMaterials.LEATHER && armor instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(armorStack);
            // El tint se maneja en el RenderType o VertexConsumer, aquí solo calculamos UV
        }

        return 72.0F * f / 4096.0F;
        */

        return 0.0; // Descomenta y ajusta el bloque superior cuando BaseNpcModel esté listo
    }
}