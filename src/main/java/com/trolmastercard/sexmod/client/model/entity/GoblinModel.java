package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.UUID;

/**
 * GoblinModel — Portado a 1.20.1 / GeckoLib 4.
 * * Define el modelo 3D del Goblin y manipula dinámicamente sus huesos (IK)
 * basándose en el estado de la animación (ej. embarazo, mirada, ser lanzado).
 */
public class GoblinModel extends BaseNpcModel<BaseNpcEntity> {

    private static final float MAX_TOSS_ANGLE = 60.0F;
    private static final float HEAD_PITCH_CLAMP = 0.75F;
    private final Minecraft mc = Minecraft.getInstance();

    // Nota: Asumo que BaseNpcModel maneja el getGeoFiles() custom que tienes.
    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/goblin/goblin.geo.json"),
                new ResourceLocation("sexmod", "geo/goblin/armored.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/goblin/goblin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/goblin/goblin.animation.json");
    }

    /**
     * Determina si el jugador dueño usa el modelo "Steve" (clásico) o "Alex" (slim).
     */
    protected boolean useClassicSkin(BaseNpcEntity entity) {
        if (!(entity instanceof GoblinEntity goblin)) return true;

        UUID ownerUUID = goblin.getOwnerUUID();
        if (ownerUUID == null) ownerUUID = goblin.getPlayerBindUUID();
        if (ownerUUID == null) return true;

        AbstractClientPlayer player = (AbstractClientPlayer) goblin.level().getPlayerByUUID(ownerUUID);
        return player == null || "default".equals(player.getModelName());
    }

    // ── Manipulación Manual de Huesos (GeckoLib 4) ──────────────────────────

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        if (entity.level() instanceof FakeWorld) return;

        AnimationProcessor<BaseNpcEntity> proc = getAnimationProcessor();
        boolean isGoblin = entity instanceof GoblinEntity;
        AnimState state = entity.getAnimState();

        // 1. Lógica del hueso de Embarazo
        CoreGeoBone preggy = proc.getBone("preggy");
        if (preggy != null) {
            boolean isPregnant = entity.getEntityData().get(GoblinEntity.IS_PREGNANT);
            preggy.setHidden(!isPregnant);
        }

        CoreGeoBone body = proc.getBone("body");
        CoreGeoBone head = proc.getBone("head");

        // 2. Ajuste de altura para escenas en primera persona
        if (state == AnimState.BREEDING_SLOW_2 || state == AnimState.BREEDING_FAST_2 || state == AnimState.BREEDING_CUM_2) {
            if (mc.options.getCameraType().isFirstPerson() && body != null) {
                body.updatePosition(body.getPosX(), body.getPosY() + 1.5F, body.getPosZ());
            }
        }

        // 3. Seguimiento de cabeza (Look-At)
        if (head != null && body != null) {
            if ((isGoblin && state == AnimState.AWAIT_PICK_UP) || state == AnimState.VANISH) {
                applyHeadLookAtPlayer(entity, head);
            } else if (isGoblin && state == AnimState.SIT) {
                applySitHeadLook(entity, head);
            }
        }

        // 4. Transformaciones de Lanzamiento y Carga (Throw / Pick Up)
        if (state == AnimState.START_THROWING) {
            boolean isLocal = mc.player != null && mc.player.getUUID().equals(entity.getMasterUUID());
            if (isLocal && body != null) {
                body.setHidden(mc.options.getCameraType().isFirstPerson());
            } else if (body != null) {
                body.setHidden(false);
            }
        } else if (body != null) {
            body.setHidden(false);
        }

        if (state == AnimState.THROWN || state == AnimState.START_THROWING) {
            Vec3 disp = BaseNpcEntity.getBodyDisplacement(entity);
            if (body != null) {
                body.updateRotation((float) disp.x, body.getRotY(), body.getRotZ());
                body.updatePosition(body.getPosX(), (float) disp.y, (float) disp.z);
            }
        }

        if (state == AnimState.START_THROWING || state == AnimState.PICK_UP) {
            applyPickUpBones(proc, entity);
        }

        // 5. Cinemática Inversa (IK) para entidades que no son el Goblin Base
        if (!isGoblin) {
            applyArmLegIK(proc, entity);
            applyArmHideIfNeeded(proc, entity);
        }
    }

    // ── Helpers de Cinemática (IK) y Rotación ───────────────────────────────

    private void applyPickUpBones(AnimationProcessor<BaseNpcEntity> proc, BaseNpcEntity entity) {
        UUID masterUUID = entity.getMasterUUID();
        if (masterUUID == null || mc.player == null) return;

        if (mc.options.getCameraType().isFirstPerson() && mc.player.getUUID().equals(masterUUID)) return;

        CoreGeoBone body = proc.getBone("body");
        CoreGeoBone steve = proc.getBone("steve");

        // Escondemos el cuerpo bajándolo fuera de cámara durante la animación de recogida
        if (body != null) body.updatePosition(body.getPosX(), body.getPosY() - 32.0F, body.getPosZ());
        if (steve != null) steve.updatePosition(steve.getPosX(), steve.getPosY() - 32.0F, steve.getPosZ());
    }

    private void applyHeadLookAtPlayer(BaseNpcEntity entity, CoreGeoBone head) {
        Player nearest = entity.level().getNearestPlayer(entity, 15.0D);
        if (nearest == null) return;

        Vec3 diff = nearest.position().subtract(entity.position());
        float yaw = entity.getYRot();

        // Conversión limpia de radianes a grados usando Math.toDegrees
        float bodyYaw2 = (float) -(Math.toDegrees(Math.atan2(diff.z, diff.x)));

        // Ajuste cardinal tosco heredado de la 1.12.2
        int cardinalYaw = Math.round(yaw / 90.0F) * 90;
        switch (cardinalYaw) {
            case 0 -> bodyYaw2 -= 90.0F;
            case 180, -180 -> bodyYaw2 += 90.0F;
            case 90 -> bodyYaw2 += 180.0F;
        }

        float headPitch2 = Mth.clamp((float) ((nearest.getEyeHeight() + nearest.getY()) - (entity.getEyeHeight() + entity.getY())), -HEAD_PITCH_CLAMP, HEAD_PITCH_CLAMP);

        head.updateRotation(headPitch2, (float) Math.toRadians(bodyYaw2), head.getRotZ());
    }

    private void applySitHeadLook(BaseNpcEntity entity, CoreGeoBone head) {
        Player nearest = entity.level().getNearestPlayer(entity, 15.0D);
        if (nearest == null) return;

        Vec3 diff = nearest.position().subtract(entity.position());
        float targetYaw = (float) -(Math.toDegrees(Math.atan2(diff.z, diff.x))) + 90.0F;
        float pitch = Mth.clamp((float) ((nearest.getEyeHeight() + nearest.getY()) - (entity.getEyeHeight() + entity.getY())), -HEAD_PITCH_CLAMP, HEAD_PITCH_CLAMP);

        head.updateRotation(pitch, (float) Math.toRadians(targetYaw), head.getRotZ());
    }

    private void applyArmLegIK(AnimationProcessor<BaseNpcEntity> proc, BaseNpcEntity entity) {
        UUID masterUUID = entity.getMasterUUID();
        if (masterUUID == null) return;

        Player rider = entity.level().getPlayerByUUID(masterUUID);
        if (rider == null) return;

        float partial = mc.getFrameTime();
        // Sincronizar el balanceo de piernas con la animación de caminata del jugador
        float walkPos = rider.walkAnimation.position(partial);
        float walkSpeed = rider.walkAnimation.speed(partial);

        float legSwing = (float) Math.toRadians(MAX_TOSS_ANGLE * Math.sin(walkPos) * walkSpeed);

        CoreGeoBone leftLeg = proc.getBone("LeftLeg");
        CoreGeoBone rightLeg = proc.getBone("RightLeg");

        if (leftLeg != null) leftLeg.updateRotation(legSwing, leftLeg.getRotY(), leftLeg.getRotZ());
        if (rightLeg != null) rightLeg.updateRotation(-legSwing, rightLeg.getRotY(), rightLeg.getRotZ());
    }

    private void applyArmHideIfNeeded(AnimationProcessor<BaseNpcEntity> proc, BaseNpcEntity entity) {
        if (entity.getAnimState() != AnimState.PICK_UP) return;

        UUID masterUUID = entity.getMasterUUID();
        if (masterUUID != null && mc.player != null && mc.options.getCameraType().isFirstPerson()
                && mc.player.getUUID().equals(masterUUID)) {
            return;
        }

        CoreGeoBone body = proc.getBone("body");
        CoreGeoBone steve = proc.getBone("steve");

        if (body != null) body.updatePosition(body.getPosX(), body.getPosY() - 32.0F, body.getPosZ());
        if (steve != null) steve.updatePosition(steve.getPosX(), steve.getPosY() - 32.0F, steve.getPosZ());
    }

    // ── Arrays de Huesos para el Renderizador (Slots) ─────────────────────────

    @Override public String[] getHelmetBones()     { return new String[]{ "armorHelmet" }; }
    @Override public String[] getChestBones()      { return new String[]{ "armorBoobL", "armorBoobR" }; }
    @Override public String[] getUpperFleshBones() { return new String[]{ "nippleL", "nippleR" }; }
    @Override public String[] getLowerArmorBones() { return new String[]{ "armorCheekR", "armorCheekL", "armorLegL", "armorLegR", "armorShinL", "armorShinR", "armorTorso" }; }
    @Override public String[] getLowerFleshBones() { return new String[]{ "fuckhole", "vagina", "meatCheekR", "meatCheekL", "meatLegL", "meatLegR", "meatShinL", "meatShinR" }; }
    @Override public String[] getShoeBones()       { return new String[]{ "armorFootL", "armorFootR" }; }
    public String[] getShoeFleshBones()            { return new String[]{ "meatFootL", "meatFootR" }; }
}