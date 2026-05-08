package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

import javax.annotation.Nullable;

/**
 * KoboldModel — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja la geometría dinámica, personalización de huesos y transformaciones de transición.
 * Incluye: Escala de órganos, variantes de cuernos, pecas y seguimiento de mirada.
 */
public class KoboldModel extends BaseNpcModel<KoboldEntity> {

    private static final float EYE_SCALE_MIN = 1.0F;
    private static final float EYE_SCALE_MAX = 1.2F;

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/kobold/kobold.geo.json"),
                new ResourceLocation("sexmod", "geo/kobold/armored.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(KoboldEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/kobold.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEntity entity) {
        return new ResourceLocation("sexmod", "animations/kobold/kobold.animation.json");
    }

    // ── Configuración de Capas Visuales (Bones) ──────────────────────────────

    @Override public String[] getHelmetBones() { return new String[] { "armorHelmet" }; }
    @Override public String[] getChestBones() { return new String[] { "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" }; }
    @Override public String[] getUpperFleshBones() { return new String[] { "boobsFlesh", "upperBodyL", "upperBodyR" }; }
    @Override public String[] getLowerArmorBones() { return new String[] { "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip" }; }
    @Override public String[] getLowerFleshBones() { return new String[] { "fleshL", "fleshR", "vagina", "fuckhole", "curvesL", "curvesR" }; }
    @Override public String[] getShoeBones() { return new String[] { "armorShoesL", "armorShoesR" }; }

    // ── Animaciones Personalizadas y Overrides de Huesos ─────────────────────

    @Override
    public void setCustomAnimations(KoboldEntity entity, long instanceId, @Nullable AnimationState<KoboldEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);

        if (Minecraft.getInstance().isPaused() || entity.level() == null) return;

        var processor = getAnimationProcessor();

        // 1. Elementos de Estatus (Corona/Huevo)
        CoreGeoBone crown = processor.getBone("crown");
        CoreGeoBone egg = processor.getBone("egg");

        if (!entity.isInteractiveMode()) {
            if (crown != null) crown.setHidden(!entity.isLeader());
            if (egg != null) egg.setHidden(!entity.isGrowthStateActive());
        }

        // 2. Aplicar Datos de Personalización (desde NBT)
        String[] data = entity.getCustomizationData();
        if (data != null && data.length >= 7) {
            applyHorns(processor, data[0], "hornUL", "hornUR");
            applyHorns(processor, data[1], "hornDL", "hornDR");
            applyBoneScale(processor, data[3], EYE_SCALE_MIN, EYE_SCALE_MAX, "eyeL", "eyeR");
            applyFreckles(processor, data[4], "frecklesAL", "frecklesAR");
            applyFreckles(processor, data[5], "frecklesHL", "frecklesHR");
            applyBackpack(entity, processor, data[6]);
        }

        // 3. Lógica de Transición de Cuerpo
        applyBodyPositioning(entity, processor);

        // 4. Seguimiento de Cabeza (Head Look)
        if (state != null) {
            applyHeadRotation(entity, state, processor);
        }
    }

    // ── Utilidades de Transformación ─────────────────────────────────────────

    private void applyHeadRotation(KoboldEntity entity, AnimationState<KoboldEntity> state, software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor) {
        if (entity.getAnimState() != AnimState.NULL || !entity.onGround()) return;

        double yDelta = Math.abs(Math.abs(entity.yOld) - Math.abs(entity.getY()));
        if (yDelta > 0.1) return;
        // if (!entity.shouldFollowLook()) return;

        var modelData = state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        software.bernie.geckolib.core.animatable.model.CoreGeoBone head = processor.getBone("head");
        if (head != null) {
            head.setRotY(modelData.netHeadYaw() * ((float) Math.PI / 180.0F));
            head.setRotX(modelData.headPitch() * ((float) Math.PI / 180.0F));
        }

        software.bernie.geckolib.core.animatable.model.CoreGeoBone body = processor.getBone("body");
        if (body == null) body = processor.getBone("dd"); // Respaldo por si el hueso se llama 'dd'
        if (body != null) {
            body.setRotY(0.0F);
        }
    }

    private void applyBodyPositioning(KoboldEntity entity, software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor) {
        AnimationController<KoboldEntity> controller = entity.getMainAnimController();
        if (controller == null || controller.getAnimationState() != AnimationController.State.TRANSITIONING) return;

        float transProgress = entity.getAnimationProgress();
        float f = 0.25F - transProgress;

        CoreGeoBone body = processor.getBone("body");
        if (body == null) return;

        AnimState state = entity.getAnimState();

        // 🚨 REPARACIÓN: Type A (Doggy-style entrance)
        if (AnimState.isOneOf(state, AnimState.STARTDOGGY, AnimState.DOGGYSTART, AnimState.DOGGYSLOW, AnimState.DOGGYFAST)) {
            body.setPosZ(11.43F + f * -7.0F);
        }
        // 🚨 REPARACIÓN: Type B (Standing sex / Missionary / Paizuri)
        else if (AnimState.isOneOf(state, AnimState.MISSIONARY_START, AnimState.PAIZURI_START, AnimState.RAPE_INTRO)) {
            body.setPosX(1.78F  + f * -1.5F);
            body.setPosY(13.07F + f * -11.0F);
            body.setPosZ(2.05F  + f * -8.0F);
        }
        // 🚨 REPARACIÓN: Type C (Cowgirl / Reverse)
        else if (AnimState.isOneOf(state, AnimState.COWGIRLSTART, AnimState.REVERSE_COWGIRL_START, AnimState.COWGIRL_SITTING_INTRO)) {
            body.setPosX(0.0F);
            body.setPosY(2.85F);
            body.setPosZ(-7.0F + f * 4.7F);
        }
    }

    private void applyBackpack(KoboldEntity entity, software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor, String type) {
        int i = Integer.parseInt(type);
        CoreGeoBone bp = processor.getBone("backpack");
        CoreGeoBone tp = processor.getBone("tailpack");
        if (bp == null || tp == null) return;

        bp.setHidden(i != 0 && i != 1 && entity.getAnimState() != AnimState.PAYMENT);
        tp.setHidden(i != 1 && i != 2);
    }

    private void applyFreckles(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor, String variant, String prefixL, String prefixR) {
        int v = Integer.parseInt(variant);
        for (int n = 1; n <= 2; n++) {
            CoreGeoBone bL = processor.getBone(prefixL + n);
            CoreGeoBone bR = processor.getBone(prefixR + n);
            if (bL != null) bL.setHidden(v != n);
            if (bR != null) bR.setHidden(v != n);
        }
    }

    private void applyHorns(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor, String variant, String prefixL, String prefixR) {
        int v = Integer.parseInt(variant);
        // Ocultar todos los cuernos y mostrar solo el seleccionado
        for (int n = 0; n < 10; n++) {
            CoreGeoBone bL = processor.getBone(prefixL + n);
            CoreGeoBone bR = processor.getBone(prefixR + n);
            if (bL != null) bL.setHidden(v != n);
            if (bR != null) bR.setHidden(v != n);
        }
    }

    private void applyBoneScale(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor, String val, float min, float max, String... names) {
        float f = Mth.lerp(Float.parseFloat(val) / 100.0F, min, max);
        for (String name : names) {
            CoreGeoBone bone = processor.getBone(name);
            if (bone != null) {
                bone.setScaleX(f);
                bone.setScaleY(f);
                bone.setScaleZ(f);
            }
        }
    }
}