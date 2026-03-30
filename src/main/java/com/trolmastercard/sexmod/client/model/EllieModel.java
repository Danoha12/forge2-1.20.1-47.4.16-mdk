package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.EllieEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.HashMap;
import java.util.Map;

/**
 * EllieModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * GeoModel para la entidad NPC Ellie.
 * Incluye seguimiento de cabeza (Head-tracking) dinámico cuando el NPC está sentado
 * y mapeo de huesos para el sistema de capas de armadura/vestimenta.
 */
public class EllieModel extends BaseNpcModel<EllieEntity> {

    private final Map<Integer, float[]> headAngleConfigs = new HashMap<>();

    public EllieModel() {
        super();
        // Configuración de límites de rotación según el Yaw de la entidad
        // float[]{angleOffset, minClamp, maxClamp}
        headAngleConfigs.put(0,   new float[]{0.0f, -1.2f, 1.2f});
        headAngleConfigs.put(-90, new float[]{2.0f, -71.56f, -68.0f});
        headAngleConfigs.put(90,  new float[]{-2.0f, 68.0f, 70.5f});
    }

    // ── Localizaciones de Recursos ──────────────────────────────────────────

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[]{
                new ResourceLocation("sexmod", "geo/ellie/nude.geo.json"),
                new ResourceLocation("sexmod", "geo/ellie/dressed.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(EllieEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/ellie/ellie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EllieEntity entity) {
        return new ResourceLocation("sexmod", "animations/ellie/ellie.animation.json");
    }

    // ── Animaciones Personalizadas (Head Tracking) ──────────────────────────

    @Override
    public void setCustomAnimations(EllieEntity entity, long instanceId, AnimationState<EllieEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // Evitar cálculos en mundos nulos o durante renderizados de jugador
        if (entity.level() == null) return;

        // El seguimiento de cabeza solo se activa en estado de reposo sentado
        if (entity.getAnimState() != AnimState.SITDOWNIDLE) return;

        var nearestPlayer = entity.level().getNearestPlayer(entity, 15.0);
        if (nearestPlayer == null) return;

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        Vec3 delta = entity.position().subtract(nearestPlayer.position());
        int entityYaw = Math.round(entity.getYRot());

        float rotationY;
        // Lógica de cálculo de ángulo según la orientación del NPC
        if (entityYaw == 180 || entityYaw == -180) {
            rotationY = (float) (Mth.atan2(delta.x, delta.z) * 1.2f);
            if (rotationY > 0) rotationY = Mth.clamp(rotationY, 1.5f, 3.14f);
            else rotationY = Mth.clamp(rotationY, -3.14f, -1.5f);

            boolean atLimit = (rotationY == 1.5f || rotationY == 3.14f || rotationY == -3.14f || rotationY == -1.5f);
            rotationY = atLimit ? 0f : rotationY + 3.0f;
        } else {
            float[] cfg = headAngleConfigs.get(entityYaw);
            if (cfg == null) return;

            float rawAngle = (float) (Mth.atan2(delta.x, delta.z) + cfg[0]) + (entity.getYRot() * 0.017453292F) * 0.8f;
            rawAngle = Mth.clamp(rawAngle, cfg[1], cfg[2]);

            boolean atLimit = (rawAngle == cfg[1] || rawAngle == cfg[2]);
            rotationY = atLimit ? 0f : rawAngle;
        }

        // Cálculo de Pitch (Inclinación vertical)
        float rotationX = (rotationY != 0f)
                ? Mth.clamp((float) ((nearestPlayer.getY() - entity.getY()) * 0.5), -0.75f, 0.75f)
                : 0f;

        // Aplicar rotaciones al hueso de la cabeza en GeckoLib 4
        head.updateRotation(rotationX, rotationY, head.getRotZ());
    }

    // ── Mapeo de Huesos para Armaduras y Vestimenta ─────────────────────────

    @Override public String[] getHelmetBones() { return new String[]{"armorHelmet"}; }

    @Override public String[] getHeadwearBones() { return new String[]{"headband"}; }

    @Override public String[] getChestBones() {
        return new String[]{"armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs"};
    }

    @Override public String[] getUpperFleshBones() {
        return new String[]{"boobsFlesh", "upperBodyL", "upperBodyR"};
    }

    @Override public String[] getLowerArmorBones() {
        return new String[]{"armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"};
    }

    @Override public String[] getLowerFleshBones() {
        return new String[]{"fleshL", "fleshR", "vagina", "hotpants", "slip", "curvesL", "curvesR", "kneeL", "kneeR"};
    }

    @Override public String[] getShoeBones() { return new String[]{"armorShoesL", "armorShoesR"}; }
}