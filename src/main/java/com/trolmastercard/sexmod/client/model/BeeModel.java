package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.BeeEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * BeeModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * GeoModel para la entidad NPC "Bee". Dos archivos geo: base y capa de armadura.
 * Este modelo ajusta el seguimiento de la cabeza y oculta secciones de la malla
 * base (chest) dependiendo de la animación en curso.
 */
public class BeeModel extends BaseNpcModel<BeeEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/bee/bee.geo.json"),
                new ResourceLocation("sexmod", "geo/bee/armored.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BeeEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/bee/bee.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BeeEntity entity) {
        return new ResourceLocation("sexmod", "animations/bee/bee.animation.json");
    }

    // =========================================================================
    //  Animaciones Personalizadas y Cinemática Inversa (IK)
    // =========================================================================

    @Override
    public void setCustomAnimations(BeeEntity entity, long instanceId, AnimationState<BeeEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // Proteger contra renderizados en menús (FakeLevel)
        if (entity.level() == null) return;

        var processor = this.getAnimationProcessor();

        // ---- Visibilidad dinámica de la sección superior ----
        CoreGeoBone chest = processor.getBone("chest");
        if (chest != null && animState.getController() != null) {
            var currentAnim = animState.getController().getCurrentAnimation();
            boolean chestAnim = currentAnim != null && currentAnim.name().contains("chest");
            chest.setHidden(!chestAnim);
        }

        // ---- Seguimiento de la cabeza (Head Look) ----
        AnimState state = entity.getAnimState();
        if (state == AnimState.NULL || state == AnimState.ATTACK || state == AnimState.BOW) {
            if (animState != null) {
                var md = animState.getData(DataTickets.ENTITY_MODEL_DATA);
                if (md != null) {
                    CoreGeoBone neck = processor.getBone("neck");
                    CoreGeoBone head = processor.getBone("head");
                    CoreGeoBone body = processor.getBone("body");
                    if (body == null) body = processor.getBone("dd"); // Fallback a nombre de hueso antiguo

                    float netHeadYaw = md.netHeadYaw() * Mth.DEG_TO_RAD;
                    float headPitch = md.headPitch() * Mth.DEG_TO_RAD;

                    if (neck != null) {
                        neck.updateRotation(neck.getRotX(), netHeadYaw * 0.5F, neck.getRotZ());
                    }
                    if (head != null) {
                        head.updateRotation((float)Math.PI + headPitch, netHeadYaw, head.getRotZ());
                    }
                    if (body != null) {
                        body.updateRotation(body.getRotX(), 0.0F, body.getRotZ());
                    }
                }
            }
        }
    }

    // =========================================================================
    //  Arreglos de huesos (Mapeo exacto a Blockbench)
    // =========================================================================

    @Override
    public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    /** Huesos para las antenas. */
    public String[] getFeelerBones() {
        return new String[]{ "band", "feeler", "feeler2", "brow", "brow2", "brow3", "brow4" };
    }

    @Override
    public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    @Override
    public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR" };
    }

    @Override
    public String[] getLowerArmorBones() {
        return new String[]{
                "armorBootyR", "armorBootyL",
                "armorPantsLowL", "armorPantsLowR", "armorPantsLowR",
                "armorPantsUpR", "armorPantsUpL", "armorHip"
        };
    }

    @Override
    public String[] getLowerFleshBones() {
        return new String[]{ "sideL", "sideR", "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    @Override
    public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}