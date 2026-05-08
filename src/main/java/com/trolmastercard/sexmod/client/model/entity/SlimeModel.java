package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone; // Cambiado de CoreGeoBone para mejor compatibilidad
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Arrays;
import java.util.List;

/**
 * SlimeModel — Portado a 1.20.1 / GeckoLib 4.
 */
public class SlimeModel extends BaseNpcModel<BaseNpcEntity> {

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

        // REPARACIÓN: Eliminamos la referencia a FakeWorld
        if (entity.level() == null) {
            return geoFiles[0];
        }

        int idx = entity.getEntityData().get(BaseNpcEntity.DATA_OUTFIT_INDEX);

        if (idx < 0 || idx >= geoFiles.length) {
            return geoFiles[0];
        }

        if (entity instanceof PlayerKoboldEntity) {
            return geoFiles[idx];
        }

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

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // REPARACIÓN: Limpieza de FakeWorld
        if (entity.level() == null) return;

        AnimationProcessor<BaseNpcEntity> processor = getAnimationProcessor();

        // 1. Visibilidad de bedSlime (Casteo a GeoBone para evitar errores de CoreGeoBone)
        GeoBone bedSlime = (GeoBone) processor.getBone("bedSlime");
        GeoBone bedSlimeLayer = (GeoBone) processor.getBone("bedSlimeLayer");

        if (bedSlime != null && bedSlimeLayer != null) {
            boolean inDoggy = DOGGY_STATES.contains(entity.getAnimState());
            bedSlime.setHidden(!inDoggy);
            bedSlimeLayer.setHidden(!inDoggy);
        }

        // 2. Hat IK
        if (!(entity instanceof PlayerKoboldEntity)) {
            applyHatIK(processor, new String[]{ "head" }, "hat");
        }
    }

    private void applyHatIK(AnimationProcessor<BaseNpcEntity> processor, String[] sourceBoneNames, String targetBoneName) {
        GeoBone target = (GeoBone) processor.getBone(targetBoneName);
        if (target == null) return;

        Vector3f rot = new Vector3f(0, 0, 0);
        Vector3f pos = new Vector3f(0, 0, 0);

        for (String sourceName : sourceBoneNames) {
            GeoBone sourceBone = (GeoBone) processor.getBone(sourceName);
            if (sourceBone != null) {
                rot.add(sourceBone.getRotX(), sourceBone.getRotY(), sourceBone.getRotZ());
                pos.add(sourceBone.getPosX(), sourceBone.getPosY(), sourceBone.getPosZ());
            }
        }

        target.updateRotation(rot.x, rot.y, rot.z);
        target.updatePosition(pos.x, pos.y, pos.z);
    }

    // --- Slots de Armadura ---
    @Override public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }
    @Override public String[] getFeatureBones() { return new String[]{ "bigblob" }; }
    @Override public String[] getChestBones() { return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" }; }
    @Override public String[] getUpperFleshBones() { return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR", "cloth" }; }
    @Override public String[] getLowerArmorBones() { return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip" }; }
    @Override public String[] getLowerFleshBones() { return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" }; }
    @Override public String[] getShoeBones() { return new String[]{ "armorShoesL", "armorShoesR" }; }
}