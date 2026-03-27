package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.command.FutaCommand;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * GalathModel - Portado a 1.20.1 / GeckoLib 4.
 * Modelo complejo para la entidad jefe/compañera Galath.
 * Controla la visibilidad de armaduras, alas, y cinemáticas de combate y magia.
 */
public class GalathModel extends BaseNpcModel<BaseNpcEntity> {

    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/entity/galath/galath.png");

    // -- Variables de sincronización de ataques --
    private float headSwayPrev = 0.0F;
    private long  swordStart   = -1L;
    private long  swordEnd     = -1L;

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),
                new ResourceLocation("sexmod", "geo/galath/galath_con_mang.geo.json") // Modelo combinado para cinemáticas
        };
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        // Seleccionamos el modelo basado en si está combinada con su invocación o no
        if (entity instanceof GalathEntity galath && galath.getMangleLie(false) != null) {
            return getGeoFiles()[2];
        }
        // Fallback al índice del DataParameter (Suele ser 0)
        int index = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);
        if (index >= 0 && index < getGeoFiles().length) return getGeoFiles()[index];
        return getGeoFiles()[0];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/galath/galath.animation.json");
    }

    // =========================================================================
    //  Control Principal de Animaciones Procedurales (setCustomAnimations)
    // =========================================================================

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId,
                                    AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // Ejecutamos las modificaciones matemáticas sobre los huesos
        updateMagicFocusIK(entity);
        updateBoostWings(entity);
        updateSurpriseCharge(entity);
        updateSwordAttack(entity);
        updateKnockOutFly(entity);
        updateAppearanceBones(entity);
        updateWings(entity);
        updateSpecialAnatomyBones();
        updateBodySnapshot(entity);
        updateInteractionIK(entity);
        updateTransitionBones(entity);
        updateCoinHidden(entity);

        if (!(entity instanceof GalathEntity galath)) return;

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) galath.headRotX = head.getRotX();
    }

    // =========================================================================
    //  Cinemáticas y Movimientos de Combate
    // =========================================================================

    /** Movimiento de cabeza durante estado de concentración mágica */
    private void updateMagicFocusIK(BaseNpcEntity entity) {
        if (com.trolmastercard.sexmod.client.ClientProxy.IS_PRELOADING) return;
        if (entity.getAnimState() != AnimState.REST_SLOW) return; // Antes MASTERBATE

        var mc = Minecraft.getInstance();
        var player = entity.getOwnerPlayer();
        if (player == null) player = mc.player;

        var proc = getAnimationProcessor();
        var headBone = proc.getBone("head");
        if (headBone == null) return;

        Vec3 offset = com.trolmastercard.sexmod.util.NpcPositionUtil.getOffsetToPlayer(
                        entity, player, mc.getPartialTick())
                .subtract(BaseNpcEntity.getBoneOffset(entity, "head"));

        float yaw   = (float) MathUtil.normalizeAngle(Math.atan2(offset.z, offset.x)) - entity.getYRot();
        float pitch = (float) MathUtil.normalizeAngle(Math.atan2(offset.y, Math.sqrt(offset.x * offset.x + offset.z * offset.z)));
        double dist = Math.abs(offset.x) + Math.abs(offset.y) + Math.abs(offset.z);

        // Pasamos variables al MolangParser de GeckoLib
        var parser = software.bernie.geckolib.loading.math.MathParser.INSTANCE;
        try {
            parser.setVariable("pitch",    dist * 7.0D - 20.0D + pitch - 80.0D);
            parser.setVariable("armpitch", dist * 5.0D - 20.0D + pitch - 110.0D);
            parser.setVariable("armyaw",   yaw + 80.0F);
            parser.setVariable("yaw",      yaw + 90.0F);
        } catch (Exception ignored) {}
    }

    /** Control de alas durante el vuelo de impulso (BOOST) */
    private void updateBoostWings(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        float yawOffset = 0.0F;
        AnimState state = galath.getAnimState();

        if (state == AnimState.BOOST) {
            // El Boost tiene un delay antes de girar las alas
            yawOffset = 45.0F;
        } else if (state == AnimState.KNOCK_OUT_FLY || state == AnimState.KNOCK_OUT_GROUND) {
            yawOffset = 0.0F;
        } else {
            return;
        }

        float partial = Minecraft.getInstance().getPartialTick();
        CoreGeoBone rotTool = getAnimationProcessor().getBone("rotationTool");
        if (rotTool == null) return;

        var yp = galath.getYawPitch(); // Necesitas asegurarte de que GalathEntity tiene getYawPitch()
        if (yp != null) {
            rotTool.setRotX((float) MathUtil.lerp(yp.pitchPrev + yawOffset, yp.pitch + yawOffset, partial));
            rotTool.setRotZ((float) MathUtil.lerp(yp.yawPrev,  yp.yaw,  partial));
        }
    }

    /** Control de rotación del cuerpo durante el ataque sorpresa */
    private void updateSurpriseCharge(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        if (galath.getAnimState() != AnimState.SURPRISE_CHARGE) return; // Antes RAPE_CHARGE

        // MangleLieModel tiene la matemática para volar en arco. Mantenemos la lógica de posición
        CoreGeoBone body    = getAnimationProcessor().getBone("body");
        CoreGeoBone rotTool = getAnimationProcessor().getBone("rotationTool");
        if (body == null || rotTool == null) return;

        // Simulamos el vector (Si tienes MangleLieModel portado, usa ese método aquí)
        float spin = galath.getEntityData().get(GalathEntity.DATA_FLOAT); // SPIN_PROGRESS
        body.setRotY((float) Math.toRadians(spin * 180.0F));
    }

    /** Lógica de destellos e inclinación durante el ataque de espada */
    private void updateSwordAttack(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        if (galath.getAnimState() != AnimState.ATTACK_SWORD) {
            swordStart = -1L; swordEnd = -1L; return;
        }

        int frame = galath.getAttackAnimIdx(); // Antes getSwordAnimFrame()
        if (frame == 24 && swordStart == -1L) {
            swordStart = entity.level().getGameTime();
            swordEnd   = swordStart + 8L;
        }
        if (!MathUtil.between(frame, 24.0D, 32.0D)) return;

        CoreGeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;

        float partial = Minecraft.getInstance().getPartialTick();
        float t = ((float)(entity.level().getGameTime() + partial) - swordStart) / (float)(swordEnd - swordStart);

        // Simulación de inclinación/balanceo (Reemplaza getBodySwayAt si no lo tienes)
        body.setRotX(body.getRotX() * (1.0F - t));
    }

    private void updateKnockOutFly(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        if (!galath.getEntityData().get(GalathEntity.DATA_PARALYZED)) return; // IS_CARRIED
        if (galath.getAnimState() != AnimState.KNOCK_OUT_FLY) return;

        CoreGeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;

        Vec3 prevPos = new Vec3(entity.xOld, entity.yOld, entity.zOld);
        Vec3 delta   = entity.position().subtract(prevPos);
        boolean stationary = (Math.abs(delta.x) + Math.abs(delta.z) < 0.01D);

        if (stationary) {
            body.setRotX((float) Math.toRadians(-90.0F));
            body.setPosY(0.0F);
            body.setPosZ(0.0F);
        }
    }

    // =========================================================================
    //  Control de Visibilidad de Huesos (Ropa, Anatomía, Accesorios)
    // =========================================================================

    /** Controla qué piezas de ropa se renderizan según el estado actual */
    private void updateAppearanceBones(BaseNpcEntity entity) {
        var proc = getAnimationProcessor();
        CoreGeoBone nipR     = proc.getBone("nippleR");
        CoreGeoBone nipL     = proc.getBone("nippleL");
        CoreGeoBone braBoobL = proc.getBone("braBoobL");
        CoreGeoBone braBoobR = proc.getBone("braBoobR");
        CoreGeoBone slip     = proc.getBone("slip");

        if (nipR == null || braBoobL == null) return;

        boolean isAlternate = entity instanceof GalathEntity g && g.alternateAppearance;
        boolean isActionState = false;

        if (entity.getAnimState() != null) {
            isActionState = AnimState.isOneOf(entity.getAnimState(),
                    AnimState.LICKING, AnimState.SITTING_ACTION, AnimState.SITTING_FINISH);
        }

        // Huesos intactos para mantener la compatibilidad con el JSON
        nipR.setHidden(!isAlternate);
        nipL.setHidden(!isAlternate);
        braBoobL.setHidden(isAlternate);
        braBoobR.setHidden(isAlternate);
        if (slip != null) slip.setHidden(!(isAlternate || isActionState));
    }

    private void updateWings(BaseNpcEntity entity) {
        boolean hasWings = true; // Si GalathEntity tiene hasWings(), úsalo: ((GalathEntity)entity).hasWings()
        CoreGeoBone wings = getAnimationProcessor().getBone("wings");
        if (wings != null) wings.setHidden(!hasWings);
    }

    private void updateSpecialAnatomyBones() {
        // Mantenemos los IDs de los huesos para que el ModCommand de Futa no rompa el render
        CoreGeoBone futaCock = getAnimationProcessor().getBone("futaCock");
        CoreGeoBone futaBallLL = getAnimationProcessor().getBone("futaBallLL");
        CoreGeoBone futaBallLR = getAnimationProcessor().getBone("futaBallLR");

        if (futaCock != null) futaCock.setHidden(!FutaCommand.futaModeEnabled);
        if (futaBallLL != null) futaBallLL.setHidden(!FutaCommand.futaModeEnabled);
        if (futaBallLR != null) futaBallLR.setHidden(!FutaCommand.futaModeEnabled);
    }

    private void updateBodySnapshot(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        CoreGeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;
        // Variables usadas para sincronizar modelos compuestos
        galath.bodyRotY   = body.getRotY();
        galath.bodyScaleY = body.getScaleY();
    }

    private void updateCoinHidden(BaseNpcEntity entity) {
        if (!(entity instanceof PlayerKoboldEntity)) return;
        CoreGeoBone coin = getAnimationProcessor().getBone("coin");
        if (coin != null) coin.setHidden(true);
    }

    // =========================================================================
    //  Cinemáticas de Interacción Amistosa
    // =========================================================================

    /** Control de inclinación de cabeza durante interacciones */
    private void updateInteractionIK(BaseNpcEntity entity) {
        if (entity.getAnimState() != AnimState.LICKING) return;
        if (!(entity instanceof GalathEntity galath)) return;

        var proc  = getAnimationProcessor();
        CoreGeoBone head = proc.getBone("head");
        if (head == null) return;

        float tick = Minecraft.getInstance().getPartialTick() + entity.level().getGameTime();

        // Simulación de oscilación (Sway)
        float swayRot = (float) Math.sin(tick * 0.1F) * 0.05F;

        head.setRotX(head.getRotX() + swayRot);
        head.setRotY(head.getRotY() + swayRot);
        head.setRotZ(head.getRotZ() + swayRot);

        // Sonidos de interacción (Lick -> Risas o sonidos mágicos)
        if (!galath.interactionFwd) {
            float sin = (float) Math.sin(tick * 0.3F) * 10.0F;
            if ((sin > 0 && headSwayPrev <= 0) || (sin < 0 && headSwayPrev >= 0)) {
                entity.playSound(com.trolmastercard.sexmod.registry.ModSounds.GIRLS_GALATH_GIGGLE[0], 1.0F, 1.0F);
            }
            headSwayPrev = sin;
        }
    }

    private void updateTransitionBones(BaseNpcEntity entity) {
        var proc = getAnimationProcessor();
        AnimState state = entity.getAnimState();
        if (state == AnimState.TEAM_WORK_SLOW) { // Antes HUG_MANG
            CoreGeoBone body2 = proc.getBone("body2");
            if (body2 == null) return;
            body2.setPosX(0.0F);
            body2.setPosY(-0.53F);
            body2.setPosZ(-40.05F);
        }
    }

    // =========================================================================
    //  Slots de Armadura
    // =========================================================================

    @Override
    public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }
}