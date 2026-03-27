package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * BeeModel - Portado a 1.20.1 / GeckoLib 4.
 * * Modelo para la entidad Abeja de la Tribu.
 * Maneja capas de armadura, visibilidad dinámica de huesos y rotación de cabeza.
 */
public class BeeModel extends BaseNpcModel<BaseNpcEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/bee/bee.geo.json"),
                new ResourceLocation("sexmod", "geo/bee/armored.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/bee/bee.png");
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "geo/bee/bee.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/bee/bee.animation.json");
    }

    // =========================================================================
    //  Configuración de Animaciones Personalizadas
    // =========================================================================

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId,
                                    AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // El procesamiento de huesos solo ocurre en el cliente
        if (!entity.level().isClientSide) return;

        var processor = this.getAnimationProcessor();

        // ---- Visibilidad del Torso (Se oculta a menos que la animación lo pida) ----
        CoreGeoBone chest = processor.getBone("chest");
        if (chest != null) {
            var ctrl = entity.getMainAnimationController();
            boolean chestAnim = ctrl != null
                    && ctrl.getCurrentAnimation() != null
                    && ctrl.getCurrentAnimation().animation().name().contains("chest");
            chest.setHidden(!chestAnim);
        }

        // ---- Seguimiento de Cabeza (Solo en estados de espera o ataque) --------
        AnimState state = entity.getAnimState();
        if (state == AnimState.NULL || state == AnimState.ATTACK || state == AnimState.BOW) {
            if (animState != null) {
                EntityModelData md = animState.getData(DataTickets.ENTITY_MODEL_DATA);
                if (md != null) {
                    CoreGeoBone neck = processor.getBone("neck");
                    CoreGeoBone head = processor.getBone("head");
                    CoreGeoBone body = processor.getBone("body");
                    if (body == null) body = processor.getBone("dd");

                    // Aplicamos rotación suave basada en la mira del NPC
                    if (neck != null) neck.setRotY(md.netHeadYaw() * 0.5F * (float)Math.PI / 180.0F);
                    if (head != null) {
                        head.setRotY(md.netHeadYaw() * (float)Math.PI / 180.0F);
                        head.setRotX((float)Math.PI + md.headPitch() * (float)Math.PI / 180.0F);
                    }
                    if (body != null) body.setRotY(0.0F);
                }
            }
        }
    }

    // =========================================================================
    //  Mapeado de Huesos por Slot (IMPORTANTE: No cambiar los Strings)
    // =========================================================================

    /** Casco */
    @Override public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    /** Antenas / Adornos de cabeza */
    public String[] getFeelerBones() {
        return new String[]{ "band", "feeler", "feeler2", "brow", "brow2", "brow3", "brow4" };
    }

    /** Armadura de Pecho / Hombros */
    @Override public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    /** Detalles del Torso */
    @Override public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR" };
    }

    /** Pantalones / Cadera */
    @Override public String[] getLowerArmorBones() {
        return new String[]{
                "armorBootyR", "armorBootyL",
                "armorPantsLowL", "armorPantsLowR",
                "armorPantsUpR", "armorPantsUpL", "armorHip"
        };
    }

    /** Detalles de Piernas y Pelvis */
    @Override public String[] getLowerFleshBones() {
        return new String[]{ "sideL", "sideR", "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    /** Calzado */
    @Override public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}