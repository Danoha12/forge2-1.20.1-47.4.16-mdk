package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcStateAccessor;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.GoblinEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.UUID;

/**
 * GoblinModel - Portado a 1.20.1 / GeckoLib 4.
 * Modelo GeoModel para la entidad NPC "Goblin" (Duende).
 * Maneja lógicas visuales complejas para cuando es cargada, lanzada,
 * o cuando activa sus estados visuales secundarios.
 */
public class GoblinModel extends BaseNpcModel<BaseNpcEntity> {

    private static final float MAX_TOSS_ANGLE = 60.0F;
    private final Minecraft mc = Minecraft.getInstance();

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

    /** Verifica si la entidad debe usar la skin clásica (no slim). */
    protected boolean useClassicSkin(BaseNpcEntity entity) {
        if (!(entity instanceof GoblinEntity goblin)) {
            return true;
        }
        UUID ownerUUID = goblin.getOwnerUUID();
        if (ownerUUID == null) ownerUUID = goblin.getPlayerBindUUID(); // Asumiendo que getPlayerBindUUID existe
        if (ownerUUID == null) return true;

        net.minecraft.client.player.AbstractClientPlayer player =
                (net.minecraft.client.player.AbstractClientPlayer) goblin.level().getPlayerByUUID(ownerUUID);

        if (player == null) return true;
        return "default".equals(player.getModelName());
    }

    // =========================================================================
    //  Animaciones Procedurales (setCustomAnimations)
    // =========================================================================

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId,
                                    AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // Evitar cálculos innecesarios en mundos falsos (ej. menús)
        if (entity.level().getClass().getSimpleName().equals("FakeWorld")) return;

        var proc    = getAnimationProcessor();
        boolean isGoblin = (entity instanceof GoblinEntity);
        NpcStateAccessor accessor = (NpcStateAccessor) entity;

        // ---- Hueso de Característica Especial (Mantenemos ID original) ----
        CoreGeoBone preggy = proc.getBone("preggy");
        if (preggy != null && isGoblin) {
            boolean isSpecialState = entity.getEntityData().get(GoblinEntity.IS_PREGNANT);
            preggy.setHidden(!isSpecialState);
        }

        CoreGeoBone body = proc.getBone("body");
        CoreGeoBone head = proc.getBone("head");
        AnimState state  = entity.getAnimState();

        // ---- Ajuste de cámara 1ra persona durante acciones sincronizadas ----
        // Usamos los nuevos nombres de AnimState que definimos anteriormente
        if (state == AnimState.TRAIN_SLOW_2 || state == AnimState.TRAIN_FAST_2 || state == AnimState.TRAIN_FINISH_2) {
            if (mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON) {
                if (body != null) body.setPosY(body.getPosY() + 1.5F);
            }
        }

        // ---- Seguimiento de cabeza al desvanecerse o esperar ser recogida ----
        if ((isGoblin && state == AnimState.AWAIT_PICK_UP) || state == AnimState.VANISH) {
            applyHeadLookAtPlayer(entity, head);
        }

        // ---- Seguimiento de cabeza al estar sentada ----
        if (isGoblin && state == AnimState.SIT) {
            applySitHeadLook(entity, head);
        }

        // ---- Transformación del cuerpo al ser lanzada ----
        if (state == AnimState.START_THROWING) {
            boolean isLocal = mc.player != null && mc.player.getUUID().equals(accessor.getMasterUUID());
            if (isLocal) {
                applyThrowBodyLocal(body, entity);
            } else {
                applyThrowBodyRemote(body);
            }
        } else if (body != null) {
            body.setHidden(false);
        }

        // ---- Desplazamiento del cuerpo en vuelo (Lanzamiento) ----
        if (state == AnimState.THROWN || state == AnimState.START_THROWING) {
            Vec3 disp = BaseNpcEntity.getBodyDisplacement(entity);
            if (body != null) {
                body.setRotX((float) disp.x);
                body.setPosY((float) disp.y);
                body.setPosZ((float) disp.z);
            }
        }

        // ---- Ocultar huesos al ser cargada (PICK_UP) ----
        if (state == AnimState.START_THROWING || state == AnimState.PICK_UP) {
            applyPickUpBones(proc, accessor);
        }

        // ---- Inverse Kinematics para avatares de jugador ----
        if (!isGoblin) {
            applyArmLegIK(proc, entity);
            applyArmHideIfNeeded(proc, entity);
        }
    }

    // =========================================================================
    //  Helpers de Cinemática Inversa (IK) y Visuales
    // =========================================================================

    private void applyThrowBodyLocal(CoreGeoBone body, BaseNpcEntity entity) {
        if (body == null || mc.player == null) return;
        if (mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON
                && mc.player.getUUID().equals(((PlayerKoboldEntity) entity).getOwnerUUID())) {
            body.setHidden(true);
        }
    }

    private void applyThrowBodyRemote(CoreGeoBone body) {
        if (body != null) body.setHidden(false);
    }

    private void applyPickUpBones(AnimationProcessor proc, NpcStateAccessor accessor) {
        UUID masterUUID = accessor.getMasterUUID();
        if (masterUUID == null || mc.player == null) return;
        if (mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON
                && mc.player.getUUID().equals(masterUUID)) return;

        CoreGeoBone body  = proc.getBone("body");
        CoreGeoBone steve = proc.getBone("steve");
        if (body != null) body.setPosY(body.getPosY() - 32.0F);
        if (steve != null) steve.setPosY(steve.getPosY() - 32.0F);
    }

    private void applyHeadLookAtPlayer(BaseNpcEntity entity, CoreGeoBone head) {
        Player nearest = entity.level().getNearestPlayer(entity, 15.0D);
        if (nearest == null || head == null) return;

        Vec3 diff = nearest.position().subtract(entity.position());
        float yaw = entity.getYRot();
        float bodyYaw2 = 0.0F;

        switch ((int) yaw) {
            case 0:   bodyYaw2 = (float)-(Math.atan2(diff.z, diff.x) * 57.29577) - 90.0F; break;
            case 180: bodyYaw2 = (float)-(Math.atan2(diff.z, diff.x) * 57.29577) + 90.0F; break;
            case 90:  bodyYaw2 = (float)-(Math.atan2(diff.z, diff.x) * 57.29577) + 180.0F; break;
            default:  bodyYaw2 = (float)-(Math.atan2(diff.z, diff.x) * 57.29577); break;
        }

        float headPitch2 = MathUtil.clamp(
                (float)(nearest.getEyeHeight() + nearest.getY() - entity.getEyeHeight() - entity.getY()),
                -HEAD_PITCH_CLAMP, HEAD_PITCH_CLAMP);

        head.setRotY(MathUtil.toRad(bodyYaw2));
        head.setRotX(headPitch2);
    }

    private void applySitHeadLook(BaseNpcEntity entity, CoreGeoBone head) {
        Player nearest = entity.level().getNearestPlayer(entity, 15.0D);
        if (nearest == null || head == null) return;

        Vec3 diff = nearest.position().subtract(entity.position());
        float yaw = (float)-(Math.atan2(diff.z, diff.x) * 57.29577) + 90.0F;
        float pitch = MathUtil.clamp(
                (float)(nearest.getEyeHeight() + nearest.getY() - entity.getEyeHeight() - entity.getY()),
                -HEAD_PITCH_CLAMP, HEAD_PITCH_CLAMP);

        head.setRotY(MathUtil.toRad(yaw));
        head.setRotX(pitch);
    }

    private void applyArmLegIK(AnimationProcessor proc, BaseNpcEntity entity) {
        UUID masterUUID = ((NpcStateAccessor) entity).getMasterUUID();
        if (masterUUID == null) return;
        Player rider = entity.level().getPlayerByUUID(masterUUID);
        if (rider == null) return;

        float partial = mc.getPartialTick();
        float bodyAnim = com.trolmastercard.sexmod.util.MathUtil.lerpYaw(
                rider.animationSpeed, rider.animationSpeedOld, partial);
        float walkPos  = rider.animationPosition;
        float legSwing = MathUtil.toRad(MAX_TOSS_ANGLE * (float) Math.sin(walkPos) * bodyAnim);

        CoreGeoBone leftLeg  = proc.getBone("LeftLeg");
        CoreGeoBone rightLeg = proc.getBone("RightLeg");
        if (leftLeg  != null) leftLeg.setRotX(legSwing);
        if (rightLeg != null) rightLeg.setRotX(-legSwing);
    }

    private void applyArmHideIfNeeded(AnimationProcessor proc, BaseNpcEntity entity) {
        AnimState state = entity.getAnimState();
        if (state != AnimState.PICK_UP) return;

        UUID masterUUID = ((NpcStateAccessor) entity).getMasterUUID();
        if (masterUUID != null && mc.player != null && mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON
                && mc.player.getUUID().equals(masterUUID)) return;

        CoreGeoBone body  = proc.getBone("body");
        CoreGeoBone steve = proc.getBone("steve");
        if (body != null) body.setPosY(body.getPosY() - 32.0F);
        if (steve != null) steve.setPosY(steve.getPosY() - 32.0F);
    }

    private static final float HEAD_PITCH_CLAMP = 0.75F;

    // =========================================================================
    //  Mapeado de Huesos por Slot (NO CAMBIAR STRINGS)
    // =========================================================================

    @Override public String[] getHelmetBones()      { return new String[]{ "armorHelmet" }; }
    @Override public String[] getChestBones()       { return new String[]{ "armorBoobL", "armorBoobR" }; }
    @Override public String[] getUpperFleshBones()  { return new String[]{ "nippleL", "nippleR" }; }
    @Override public String[] getLowerArmorBones()  { return new String[]{ "armorCheekR", "armorCheekL", "armorLegL", "armorLegR", "armorShinL", "armorShinR", "armorTorso" }; }
    @Override public String[] getLowerFleshBones()  { return new String[]{ "fuckhole", "vagina", "meatCheekR", "meatCheekL", "meatLegL", "meatLegR", "meatShinL", "meatShinR" }; }
    @Override public String[] getShoeBones()        { return new String[]{ "armorFootL", "armorFootR" }; }
    public String[] getShoeFleshBones()             { return new String[]{ "meatFootL", "meatFootR" }; }
}