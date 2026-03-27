package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.HashMap;

/**
 * EllieModel (cx) - GeoModel for the Ellie NPC.
 * Two geo files (nude / dressed), head-tracking in SITDOWNIDLE state.
 * The inner HashMap stores clamped yaw-angle ranges: key=yaw(0/-90/+90),
 * value=float[]{offset, minClamp, maxClamp}.
 */
public class EllieModel extends BaseNpcModel {

    /** key=rounded entity yaw - float[]{angleOffset, minClamp, maxClamp} */
    private final HashMap<Integer, float[]> headAngles = new HeadAngleMap();

    // -- Geo / texture / animation paths --------------------------------------

    @Override
    protected ResourceLocation[] getGeoLocations() {
        return new ResourceLocation[]{
            new ResourceLocation("sexmod", "geo/ellie/nude.geo.json"),
            new ResourceLocation("sexmod", "geo/ellie/dressed.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/ellie/ellie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/ellie/ellie.animation.json");
    }

    // -- Per-frame bone overrides ----------------------------------------------

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Skip in fake-world or player-NPC binding
        if (entity.level() instanceof FakeWorld) return;
        if (entity instanceof PlayerKoboldEntity) return;

        // Head-tracking only during SITDOWNIDLE
        if (entity.getAnimState() != AnimState.SITDOWNIDLE) return;

        var nearestPlayer = entity.level().getNearestPlayer(entity, 15.0);
        if (nearestPlayer == null) return;

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        var delta = entity.position().subtract(nearestPlayer.position());
        int yaw = Math.round(entity.getYRot());

        float angle;
        if (yaw == 180 || yaw == -180) {
            // Simple atan2 clamped to -[1.5, -]
            angle = (float)(Math.atan2(delta.x, delta.z) * 1.2f);
            if (angle > 0) angle = Math.max(1.5f, Math.min(3.14f, angle));
            else           angle = Math.min(-1.5f, Math.max(-3.14f, angle));
            boolean atLimit = (angle == 1.5f || angle == 3.14f || angle == -3.14f || angle == -1.5f);
            if (atLimit) angle = 0f;
            else         angle += 3.0f;
        } else {
            float[] cfg = headAngles.get(yaw);
            if (cfg == null) return;
            float raw = (float)(Math.atan2(delta.x, delta.z) + cfg[0]) + entity.getYRot() * 0.8f;
            raw = MathUtil.clamp(raw, cfg[1], cfg[2]);
            boolean atLimit = (raw == cfg[1] || raw == cfg[2]);
            angle = atLimit ? 0f : raw;
        }

        float pitchY = (angle != 0f)
            ? MathUtil.clamp((float)((nearestPlayer.getY() - entity.getY()) * 0.5), -0.75f, 0.75f)
            : 0f;

        head.setRotY(angle);
        head.setRotX(pitchY);
    }

    // -- Clothing bone slot arrays ---------------------------------------------

    @Override public String[] getHelmetBones()    { return new String[]{"armorHelmet"}; }
    @Override public String[] getHeadwearBones()  { return new String[]{"headband"}; }
    @Override public String[] getChestBones()     { return new String[]{"armorShoulderR","armorShoulderL","armorChest","armorBoobs"}; }
    @Override public String[] getUpperFleshBones(){ return new String[]{"boobsFlesh","upperBodyL","upperBodyR"}; }
    @Override public String[] getPantsBones()     { return new String[]{"armorBootyR","armorBootyL","armorPantsLowL","armorPantsLowR","armorPantsLowR","armorPantsUpR","armorPantsUpL","armorHip"}; }
    @Override public String[] getLowerFleshBones(){ return new String[]{"fleshL","fleshR","vagina","hotpants","slip","curvesL","curvesR","kneeL","kneeR"}; }
    @Override public String[] getShoesBones()     { return new String[]{"armorShoesL","armorShoesR"}; }

    // -- Inner head-angle configuration map -----------------------------------

    /** Pre-computed clamped-angle configs for each supported yaw value. */
    static class HeadAngleMap extends HashMap<Integer, float[]> {
        HeadAngleMap() {
            put(  0, new float[]{ 0.0f, -1.2f,  1.2f});
            put(-90, new float[]{ 2.0f, -71.56f, -68.0f});
            put( 90, new float[]{-2.0f,  68.0f,  70.5f});
        }
    }
}
