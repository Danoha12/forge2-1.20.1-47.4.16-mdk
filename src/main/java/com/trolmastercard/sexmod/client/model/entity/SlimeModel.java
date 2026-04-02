package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Arrays;
import java.util.List;

/**
 * SlimeModel — Portado a 1.20.1 / GeckoLib 4.
 * * GeoModel para el NPC "Slime".
 * * Maneja tres archivos `.geo.json` distintos según el estado de la ropa.
 * * Lógica custom para mostrar la base de la cama de slime (bedSlime) e IK de sombrero.
 */
public class SlimeModel extends BaseNpcModel<BaseNpcEntity> {

    /** Estados en los que el hueso de la base de slime debe ser VISIBLE. */
    private static final List<AnimState> DOGGY_STATES = Arrays.asList(
            AnimState.STARTDOGGY, AnimState.DOGGYSLOW, AnimState.DOGGYFAST,
            AnimState.DOGGYCUM, AnimState.DOGGYSTART, AnimState.WAITDOGGY
    );

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/slime/nude.geo.json"),    // [0]
                new ResourceLocation("sexmod", "geo/slime/armored.geo.json"), // [1]
                new ResourceLocation("sexmod", "geo/slime/dressed.geo.json")  // [2]
        };
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        ResourceLocation[] geoFiles = getGeoFiles();

        if (entity.level() == null || entity.level() instanceof FakeWorld) {
            return geoFiles[0];
        }

        // Asumiendo que usas DATA_OUTFIT_INDEX o MODEL_INDEX, ajusta si es necesario
        int idx = entity.getEntityData().get(BaseNpcEntity.DATA_OUTFIT_INDEX);

        if (idx < 0 || idx >= geoFiles.length) {
            System.out.println("[SexMod] Girl doesn't have an outfit Nr." + idx + " so im just making her nude lol");
            return geoFiles[0];
        }

        if (entity instanceof PlayerKoboldEntity) {
            return geoFiles[idx];
        }

        // Lógica original: Si es 1 (vestida), usa el archivo dressed [2]
        if (idx == 1 && geoFiles.length > 2) {
            return geoFiles[2];
        }

        return geoFiles[0];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/slime/slime.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/slime/slime.animation.json");
    }

    // ── Animaciones Customizadas (GeckoLib 4) ────────────────────────────────

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        if (entity.level() instanceof FakeWorld) return;

        AnimationProcessor<BaseNpcEntity> processor = getAnimationProcessor();

        // 1. Visibilidad de bedSlime y bedSlimeLayer
        CoreGeoBone bedSlime = processor.getBone("bedSlime");
        CoreGeoBone bedSlimeLayer = processor.getBone("bedSlimeLayer");

        if (bedSlime != null && bedSlimeLayer != null) {
            boolean inDoggy = DOGGY_STATES.contains(entity.getAnimState());
            bedSlime.setHidden(!inDoggy);
            bedSlimeLayer.setHidden(!inDoggy);
        }

        // 2. Hat IK (No se aplica si el jugador asume el control como Avatar)
        if (!(entity instanceof PlayerKoboldEntity)) {
            applyHatIK(processor, new String[]{ "head" }, "hat");
        }
    }

    /**
     * Promedia la rotación y posición de los huesos fuente y lo aplica al objetivo.
     * Útil para anclar accesorios a la cabeza.
     */
    private void applyHatIK(AnimationProcessor<BaseNpcEntity> processor, String[] sourceBoneNames, String targetBoneName) {
        CoreGeoBone target = processor.getBone(targetBoneName);
        if (target == null) return;

        Vector3f rot = new Vector3f(0, 0, 0);
        Vector3f pos = new Vector3f(0, 0, 0);

        for (String sourceName : sourceBoneNames) {
            CoreGeoBone sourceBone = processor.getBone(sourceName);
            if (sourceBone != null) {
                rot.add(sourceBone.getRotX(), sourceBone.getRotY(), sourceBone.getRotZ());
                pos.add(sourceBone.getPosX(), sourceBone.getPosY(), sourceBone.getPosZ());
            }
        }

        // GeckoLib 4 usa updateRotation y updatePosition para forzar el recálculo de la matriz
        target.updateRotation(rot.x, rot.y, rot.z);
        target.updatePosition(pos.x, pos.y, pos.z);
    }

    // ── Arrays de Huesos para el Renderizador (Slots) ───────────────────────

    @Override public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }
    @Override public String[] getFeatureBones() { return new String[]{ "bigblob" }; }
    @Override public String[] getChestBones() { return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" }; }
    @Override public String[] getUpperFleshBones() { return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR", "cloth" }; }

    @Override public String[] getLowerArmorBones() {
        return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
                "armorPantsUpR", "armorPantsUpL", "armorHip" };
    }

    @Override public String[] getLowerFleshBones() {
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    @Override public String[] getShoeBones() { return new String[]{ "armorShoesL", "armorShoesR" }; }
}