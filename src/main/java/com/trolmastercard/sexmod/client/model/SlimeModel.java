package com.trolmastercard.sexmod.client.model;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Arrays;
import java.util.List;

/**
 * SlimeModel - ported from cr.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * GeoModel for the "Slime" NPC entity. Three geo files: nude, armored, dressed.
 *
 * Custom animations:
 *   - {@code bedSlime} / {@code bedSlimeLayer} bones: hidden unless entity is in a doggy AnimState.
 *   - Hat IK: averages rotation + position of {@code head[]} bones and applies to {@code hat}.
 *
 * Bone-slot arrays mirror the standard layout with "cloth" and "bigblob" extras.
 *
 * Field mapping:
 *   f = DOGGY_STATES  (fp[] - AnimState values for doggy positions)
 *   c = geoFiles array (from BaseNpcModel)
 *
 * GeckoLib 3 - 4 changes:
 *   IBone                          - CoreGeoBone
 *   IBone.getRotationX/Y/Z()       - CoreGeoBone.getRotX/Y/Z()
 *   IBone.setRotationX/Y/Z/Pos()   - CoreGeoBone.setRotX/Y/Z / setPosX/Y/Z
 *   IBone.setHidden(bool)          - CoreGeoBone.setHidden(bool)
 *   getAnimationProcessor()        - same
 *   javax.vecmath.Vector3f         - org.joml.Vector3f
 *   AnimationEvent.getPartialTick()- animState.getPartialTick()
 *   paramem instanceof ec          - entity instanceof PlayerKoboldEntity
 *   em.y()                         - entity.getAnimState()
 */
public class SlimeModel extends BaseNpcModel<BaseNpcEntity> {

    /** AnimStates for which the bedSlime bone should be VISIBLE. */
    private static final List<AnimState> DOGGY_STATES = Arrays.asList(
        AnimState.STARTDOGGY, AnimState.DOGGYCUM, AnimState.DOGGYSLOW,
        AnimState.DOGGYFAST, AnimState.DOGGYCUM, AnimState.DOGGYSTART, AnimState.WAITDOGGY);

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
            new ResourceLocation("sexmod", "geo/slime/nude.geo.json"),
            new ResourceLocation("sexmod", "geo/slime/armored.geo.json"),
            new ResourceLocation("sexmod", "geo/slime/dressed.geo.json")
        };
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        if (entity.level() == null) return c[0];
        int idx = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);
        if (idx > c.length) {
            System.out.println("Girl doesn't have an outfit Nr." + idx + " so im just making her nude lol");
            return c[0];
        }
        if (entity instanceof PlayerKoboldEntity) return c[idx];
        if (idx == 1) return c[2];
        return c[0];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/slime/slime.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/slime/slime.animation.json");
    }

    // =========================================================================
    //  setCustomAnimations  (original: cr.a(em, Integer, AnimationEvent))
    // =========================================================================

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId,
                                    AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);
        if (entity.level() instanceof com.trolmastercard.sexmod.client.FakeWorld) return;

        var processor = getAnimationProcessor();

        // ---- bedSlime visibility ----
        CoreGeoBone bedSlime      = processor.getBone("bedSlime");
        CoreGeoBone bedSlimeLayer = processor.getBone("bedSlimeLayer");
        if (bedSlime != null && bedSlimeLayer != null) {
            boolean inDoggy = DOGGY_STATES.contains(entity.getAnimState());
            bedSlime.setHidden(!inDoggy);
            bedSlimeLayer.setHidden(!inDoggy);
        }

        // ---- Hat IK (skip for PlayerKobold) ----
        if (!(entity instanceof PlayerKoboldEntity)) {
            applyHatIK(new String[]{ "head" }, "hat");
        }
    }

    /**
     * Averages rotation + position from all {@code sourceBones} and applies the
     * result to {@code targetBoneName}.
     * Original: {@code cr.a(String[], String)}
     */
    void applyHatIK(String[] sourceBoneNames, String targetBoneName) {
        var processor = getAnimationProcessor();
        CoreGeoBone target = processor.getBone(targetBoneName);
        if (target == null) return;

        CoreGeoBone[] sources = new CoreGeoBone[sourceBoneNames.length];
        for (int i = 0; i < sourceBoneNames.length; i++) {
            sources[i] = processor.getBone(sourceBoneNames[i]);
        }

        Vector3f rot = new Vector3f(0, 0, 0);
        Vector3f pos = new Vector3f(0, 0, 0);
        for (CoreGeoBone bone : sources) {
            if (bone == null) continue;
            rot.add(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            pos.add(bone.getPosX(), bone.getPosY(), bone.getPosZ());
        }

        target.setRotX(rot.x);
        target.setRotY(rot.y);
        target.setRotZ(rot.z);
        target.setPosX(pos.x);
        target.setPosY(pos.y);
        target.setPosZ(pos.z);
    }

    // ---- Slot bone arrays ---------------------------------------------------

    @Override public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }
    @Override public String[] getFeatureBones() {
        return new String[]{ "bigblob" };
    }
    @Override public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }
    @Override public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR", "cloth" };
    }
    @Override public String[] getLowerArmorBones() {
        return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
            "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip" };
    }
    @Override public String[] getLowerFleshBones() {
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }
    @Override public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}
