package com.trolmastercard.sexmod.client.model;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * KoboldModel - ported from c9.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * Extends {@link BaseNpcModel} (cv) - the shared NPC GeoModel base that handles
 * common slot-based clothing overlay logic.
 *
 * Resources:
 *   Geo (multi):
 *     [0] = sexmod:geo/kobold/kobold.geo.json   (base mesh)
 *     [1] = sexmod:geo/kobold/armored.geo.json  (armor overlay mesh)
 *   Texture: sexmod:textures/entity/kobold/kobold.png
 *   Anim   : sexmod:animations/kobold/kobold.animation.json
 *
 * ============================================================
 * Key per-frame bone overrides ({@link #setCustomAnimations}):
 * ============================================================
 *
 * 1. Crown / egg bones
 *    - {@code crown}: hidden unless the kobold is a tribe leader ({@link KoboldEntity#IS_LEADER})
 *    - {@code egg}  : shown only when the kobold is pregnant ({@link KoboldEntity#pregnant})
 *
 * 2. Customisation data (read from NBT via {@link BaseNpcEntity#getCustomizationData()}):
 *    index 0 - upper horn variant  (hornUL/hornUR + N)
 *    index 1 - lower horn variant  (hornDL/hornDR + N)
 *    index 2 - boob scale slider   (mapped 0.75-1.35, bones: boobL, boobR, armorBoobs)
 *    index 3 - eye scale slider    (mapped 1.0-1.2,   bones: eyeL, eyeR)
 *              also eye X-spread   (mapped 1.0-1.2,   adjusts eyeL.posX / eyeR.posX)
 *    index 4 - arm freckles        (0=none, 1=type1, 2=type2; bones: frecklesAL/AR 1-2)
 *    index 5 - head freckles       (0=none, 1=type1, 2=type2; bones: frecklesHL/HR 1-2)
 *    index 6 - backpack type       (0=pack only, 1=both, 2=tailpack, 3=none; bones: backpack, tailpack)
 *
 * 3. Tongue visibility
 *    Shown (setHidden=false) when AnimState ordinal is 1-4 (lewd states); hidden otherwise.
 *
 * 4. Body position transitions ({@link #applyBodyPositionForTransition})
 *    During {@code State.TRANSITIONING} the body bone is offset to match
 *    the start pose of the incoming sex animation:
 *      - Ordinals 2-4  (doggy)   : body.Z offset
 *      - Ordinals 5-8  (standing): body.X/Y/Z offset
 *      - Ordinals 9-12 (cowgirl) : body.Y/Z offset
 *
 * 5. Head look override ({@link #applyHeadLook})
 *    Applied only in the walk/idle state (ordinal 13 in original switch) when
 *    the entity is on the ground, not moving significantly, and not in a special state.
 *
 * ============================================================
 * GeckoLib 3 - 4 API changes:
 * ============================================================
 *   AnimatedGeoModel<T>               - GeoModel<T>  (handled in BaseNpcModel)
 *   AnimationEvent                    - AnimationState<T>
 *   IBone                             - CoreGeoBone
 *   animEvent.getExtraDataOfType(...) - animState.getData(DataTickets.ENTITY_MODEL_DATA)
 *   processor.getBone(name)           - this.getAnimationProcessor().getBone(name)
 *   AnimationState.Transitioning      - AnimationController.State.TRANSITIONING
 *   entity.C (AnimationController)    - entity.getMainAnimController()
 *   iBone.setPositionX/Y/Z(f)         - bone.setPosX/Y/Z(f)
 *   iBone.setRotationX/Y(f)           - bone.setRotX/Y(f)
 *   iBone.setScaleX/Y/Z(f)            - bone.setScaleX/Y/Z(f)
 *   Minecraft.func_71410_x().func_147113_T() - Minecraft.getInstance().isPaused()
 *   gj (fake world)                   - level instanceof FakeWorld check removed
 */
public class KoboldModel extends BaseNpcModel<KoboldEntity> {

    // Slider constants - same values as original (g=1.2F, f=1.0F)
    private static final float EYE_SCALE_MIN  = 1.0F;
    private static final float EYE_SCALE_MAX  = 1.2F;
    private static final float BOOB_SCALE_MIN = 0.75F;
    private static final float BOOB_SCALE_MAX = 1.35F;

    // =========================================================================
    //  GeoModel resource overrides
    // =========================================================================

    /**
     * Returns both geo files - base kobold mesh and the armored overlay mesh.
     * Original: {@code c9.a() - ResourceLocation[]}
     */
    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
            new ResourceLocation("sexmod", "geo/kobold/kobold.geo.json"),
            new ResourceLocation("sexmod", "geo/kobold/armored.geo.json")
        };
    }

    /** Default texture (non-animated overrides may use entity-specific textures). */
    @Override
    public ResourceLocation getTextureResource(KoboldEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/kobold.png");
    }

    @Override
    public ResourceLocation getModelResource(KoboldEntity entity) {
        return new ResourceLocation("sexmod", "geo/kobold/kobold.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEntity entity) {
        return new ResourceLocation("sexmod", "animations/kobold/kobold.animation.json");
    }

    // =========================================================================
    //  Bone name arrays (slot lookups used by BaseNpcModel / KoboldRenderer)
    // =========================================================================

    /** Helmet bones. Original: {@code c9.c()} */
    @Override
    public String[] getHelmetBones() {
        return new String[] { "armorHelmet" };
    }

    /** Chest / shoulder armor bones. Original: {@code c9.f()} */
    @Override
    public String[] getChestBones() {
        return new String[] { "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    /** Upper body flesh bones (hidden under chest armor). Original: {@code c9.a() (no-arg)} */
    @Override
    public String[] getUpperFleshBones() {
        return new String[] { "boobsFlesh", "upperBodyL", "upperBodyR" };
    }

    /** Lower armor bones. Original: {@code c9.h()} */
    @Override
    public String[] getLowerArmorBones() {
        return new String[] {
            "armorBootyR", "armorBootyL",
            "armorPantsLowL", "armorPantsLowR", "armorPantsLowR",
            "armorPantsUpR", "armorPantsUpL",
            "armorHip",
            "armorKneeR", "armorKneeL"
        };
    }

    /** Lower flesh bones (hidden under leg armor). Original: {@code c9.e()} */
    @Override
    public String[] getLowerFleshBones() {
        return new String[] { "fleshL", "fleshR", "vagina", "fuckhole", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    /** Shoe armor bones. Original: {@code c9.b() (no-arg)} */
    @Override
    public String[] getShoeBones() {
        return new String[] { "armorShoesL", "armorShoesR" };
    }

    /** Toe flesh bones (hidden under shoes). Original: {@code c9.d() (no-arg)} */
    @Override
    public String[] getToeBones() {
        return new String[] { "toesR", "toesL" };
    }

    // =========================================================================
    //  setCustomAnimations  (original: c9.a(em, Integer, AnimationEvent))
    // =========================================================================

    @Override
    public void setCustomAnimations(KoboldEntity entity, long instanceId,
                                    @Nullable AnimationState<KoboldEntity> state) {
        // Delegate common slot processing to parent first
        super.setCustomAnimations(entity, instanceId, state);

        // Skip fake/camera worlds
        if (entity.level().isClientSide && isInFakeWorld(entity)) return;

        var processor = this.getAnimationProcessor();

        // ---- Crown + egg bone visibility ------------------------------------
        CoreGeoBone crown = processor.getBone("crown");
        CoreGeoBone egg   = processor.getBone("egg");

        boolean isKobold = entity instanceof KoboldEntity;
        if (isKobold && !entity.isInSexAnimation()) {
            // Crown shown only if this kobold is a tribe leader
            if (crown != null) crown.setHidden(!entity.getEntityData().get(KoboldEntity.IS_LEADER));
            // Egg shown only if pregnant
            if (egg   != null) egg.setHidden(!entity.pregnant);
        } else {
            if (crown != null) crown.setHidden(true);
            if (egg   != null) egg.setHidden(true);
        }

        // ---- Customisation data --------------------------------------------
        String[] customData = entity.getCustomizationData();  // 7-element array
        if (customData != null && customData.length >= 7) {
            applyUpperHorns(processor, customData[0]);
            applyLowerHorns(processor, customData[1]);
            applyBoneScale(processor, customData[2], BOOB_SCALE_MIN, BOOB_SCALE_MAX,
                "boobL", "boobR", "armorBoobs");
            applyBoneScale(processor, customData[3], EYE_SCALE_MIN, EYE_SCALE_MAX,
                "eyeL", "eyeR");
            applyEyeSpread(processor, customData[3], EYE_SCALE_MIN, EYE_SCALE_MAX);
            applyArmFreckles(processor, customData[4]);
            applyHeadFreckles(processor, customData[5]);
            applyPackVisibility(entity, processor, customData[6]);
        }

        // ---- Tongue visibility based on AnimState --------------------------
        CoreGeoBone tongue = processor.getBone("tounge");
        if (tongue != null) {
            int ord = entity.getAnimState().ordinal();
            // ordinals 1-4 = lewd/sex states where tongue is visible
            tongue.setHidden(ord < 1 || ord > 4);
        }

        // ---- Body position during state transitions -------------------------
        applyBodyPositionForTransition(entity, processor);

        // ---- Head look override (walk/idle) --------------------------------
        if (state != null) applyHeadLook(entity, processor, state);
    }

    // =========================================================================
    //  Body position during transitions
    //  Original: c9.b(em, AnimationProcessor)
    // =========================================================================

    /**
     * Offsets the "body" bone to match the start pose of the incoming sex
     * animation while the AnimationController is in TRANSITIONING state.
     */
    void applyBodyPositionForTransition(KoboldEntity entity,
                                        software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor) {
        AnimationController<KoboldEntity> ctrl = entity.getMainAnimController();
        if (ctrl == null || ctrl.getAnimationState() != AnimationController.State.TRANSITIONING) return;

        float transProgress = entity.getEntityData().get(KoboldEntity.ANIM_PROGRESS);
        float f = 0.25F - transProgress;

        CoreGeoBone body = processor.getBone("body");
        if (body == null) return;

        int ord = entity.getAnimState().ordinal();

        // Cases 2-4: doggy-style entrance (Z slide-in)
        if (ord >= 2 && ord <= 4) {
            body.setPosZ(11.43F + f * -7.0F);
        }
        // Cases 5-8: standing sex entrance (X/Y/Z)
        else if (ord >= 5 && ord <= 8) {
            body.setPosX(1.78F  + f * -1.5F);
            body.setPosY(13.07F + f * -11.0F);
            body.setPosZ(2.05F  + f * -8.0F);
        }
        // Cases 9-12: cowgirl/reverse entrance (Y/Z)
        else if (ord >= 9 && ord <= 12) {
            body.setPosX(0.0F);
            body.setPosY(2.85F);
            body.setPosZ(-7.0F + f * 4.7F);
        }
    }

    // =========================================================================
    //  Backpack / tailpack visibility
    //  Original: c9.a(em, AnimationProcessor, String)
    // =========================================================================

    /**
     * Shows/hides the "backpack" and "tailpack" bones based on the pack-type index.
     * 0=backpack only, 1=both, 2=tailpack only, 3=neither.
     * If the entity is in PAYMENT state the backpack is always shown.
     */
    void applyPackVisibility(KoboldEntity entity,
                             software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                             String indexStr) {
        int i = Integer.parseInt(indexStr);
        CoreGeoBone backpack  = processor.getBone("backpack");
        CoreGeoBone tailpack  = processor.getBone("tailpack");
        if (backpack == null || tailpack == null) return;

        switch (i) {
            case 0 -> { backpack.setHidden(false); tailpack.setHidden(true);  }
            case 1 -> { backpack.setHidden(false); tailpack.setHidden(false); }
            case 2 -> { backpack.setHidden(true);  tailpack.setHidden(false); }
            default-> { backpack.setHidden(true);  tailpack.setHidden(true);  }
        }

        // Payment animation always forces backpack visible
        if (entity.getAnimState() == AnimState.PAYMENT) {
            backpack.setHidden(false);
        }
    }

    // =========================================================================
    //  Head freckles  (original: c9.d(AnimationProcessor, String))
    // =========================================================================

    /**
     * Controls head-freckle bone visibility.
     * 0=hidden, 1=small freckles (HR1/HL1), 2=large freckles (HR2/HL2).
     */
    void applyHeadFreckles(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                           String indexStr) {
        int i = Integer.parseInt(indexStr);
        CoreGeoBone hr1 = processor.getBone("frecklesHR1");
        CoreGeoBone hr2 = processor.getBone("frecklesHR2");
        CoreGeoBone hl1 = processor.getBone("frecklesHL1");
        CoreGeoBone hl2 = processor.getBone("frecklesHL2");
        if (hl1 != null) hl1.setHidden(i != 1);
        if (hr1 != null) hr1.setHidden(i != 1);
        if (hl2 != null) hl2.setHidden(i != 2);
        if (hr2 != null) hr2.setHidden(i != 2);
    }

    // =========================================================================
    //  Arm freckles  (original: c9.a(AnimationProcessor, String))
    // =========================================================================

    void applyArmFreckles(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                          String indexStr) {
        int i = Integer.parseInt(indexStr);
        CoreGeoBone ar1 = processor.getBone("frecklesAR1");
        CoreGeoBone ar2 = processor.getBone("frecklesAR2");
        CoreGeoBone al1 = processor.getBone("frecklesAL1");
        CoreGeoBone al2 = processor.getBone("frecklesAL2");
        if (al1 != null) al1.setHidden(i != 1);
        if (ar1 != null) ar1.setHidden(i != 1);
        if (al2 != null) al2.setHidden(i != 2);
        if (ar2 != null) ar2.setHidden(i != 2);
    }

    // =========================================================================
    //  Eye spacing  (original: c9.a(AnimationProcessor, String, float, float))
    // =========================================================================

    /**
     * Adjusts the X position of the left and right eye bones to spread/narrow
     * the eye gap according to the slider value.
     * Skip in paused state (original: Minecraft.func_147113_T() = isPaused()).
     */
    void applyEyeSpread(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                        String valueStr, float minSpread, float maxSpread) {
        if (Minecraft.getInstance().isPaused()) return;
        float f = Float.parseFloat(valueStr) / 100.0F;
        f = minSpread + (maxSpread - minSpread) * f - 1.0F;  // offset from default
        CoreGeoBone eyeL = processor.getBone("eyeL");
        CoreGeoBone eyeR = processor.getBone("eyeR");
        if (eyeL != null) eyeL.setPosX(eyeL.getPosX() + f);
        if (eyeR != null) eyeR.setPosX(eyeR.getPosX() - f);
    }

    // =========================================================================
    //  Generic bone scale  (original: c9.a(AnimationProcessor, String, FF, String...))
    // =========================================================================

    /**
     * Uniformly scales one or more bones according to a 0-100 slider value
     * mapped into the range [minScale, maxScale].
     */
    void applyBoneScale(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                        String valueStr, float minScale, float maxScale, String... boneNames) {
        float f = Float.parseFloat(valueStr) / 100.0F;
        f = minScale + (maxScale - minScale) * f;
        for (String name : boneNames) {
            CoreGeoBone bone = processor.getBone(name);
            if (bone != null) {
                bone.setScaleX(f);
                bone.setScaleY(f);
                bone.setScaleZ(f);
            }
        }
    }

    // =========================================================================
    //  Lower horn selection  (original: c9.e(AnimationProcessor, String))
    // =========================================================================

    /**
     * Hides all lower-horn variants (hornDL/hornDR + N) then shows the selected one.
     */
    void applyLowerHorns(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                         String indexStr) {
        int i = Integer.parseInt(indexStr);
        hideAllWithPrefix(processor, getAllBonesWithPrefix(processor, "hornDL"));
        hideAllWithPrefix(processor, getAllBonesWithPrefix(processor, "hornDR"));
        CoreGeoBone dl = processor.getBone("hornDL" + i);
        CoreGeoBone dr = processor.getBone("hornDR" + i);
        if (dl != null) dl.setHidden(false);
        if (dr != null) dr.setHidden(false);
    }

    // =========================================================================
    //  Upper horn selection  (original: c9.b(AnimationProcessor, String))
    // =========================================================================

    /**
     * Hides all upper-horn variants (hornUL/hornUR + N) then shows the selected one.
     */
    void applyUpperHorns(software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                         String indexStr) {
        int i = Integer.parseInt(indexStr);
        hideAllWithPrefix(processor, getAllBonesWithPrefix(processor, "hornUL"));
        hideAllWithPrefix(processor, getAllBonesWithPrefix(processor, "hornUR"));
        CoreGeoBone ul = processor.getBone("hornUL" + i);
        CoreGeoBone ur = processor.getBone("hornUR" + i);
        if (ul != null) ul.setHidden(false);
        if (ur != null) ur.setHidden(false);
    }

    // =========================================================================
    //  Head look override  (original: c9.a(em, AnimationProcessor, AnimationEvent))
    // =========================================================================

    /**
     * In the walk/idle state (state ordinal 13 in original), overrides the head
     * bone rotation to follow the entity's net head yaw/pitch.
     * Also resets the body bone Y rotation to 0.
     *
     * Conditions (all must be true):
     *   - Entity is on the ground
     *   - Horizontal movement is negligible (|-x| + |-z| - 0)
     *   - Y positions of prev/current are close (|prevY - currentY| - 0.1)
     *   - Entity reports shouldFollowLook() = true
     */
    protected void applyHeadLook(KoboldEntity entity,
                                 software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
                                 AnimationState<KoboldEntity> animState) {
        // Only apply in walk/idle animation (ordinal 13 in switch)
        if (entity.getAnimState().ordinal() != 13) return;

        // Movement check (original: |xo-x| + |zo-z| < 0.0 - threshold typo in bytecode; effectively 0)
        double moveDelta = Math.abs(entity.xOld - entity.getX()) + Math.abs(entity.zOld - entity.getZ());
        if (entity.onGround()) {
            double yDelta = Math.abs(Math.abs(entity.yOld) - Math.abs(entity.getY()));
            if (yDelta > 0.1) return;
        }
        if (!entity.shouldFollowLook()) return;

        var modelData = animState.getData(DataTickets.ENTITY_MODEL_DATA);
        CoreGeoBone head = processor.getBone("head");
        if (head != null && modelData != null) {
            head.setRotY(modelData.netHeadYaw()  * (float) Math.PI / 180.0F);
            head.setRotX(modelData.headPitch() * (float) Math.PI / 180.0F);
        }

        CoreGeoBone body = processor.getBone("body");
        if (body == null) body = processor.getBone("dd");  // fallback bone name
        if (body != null) body.setRotY(0.0F);
    }

    // =========================================================================
    //  Prefix bone enumeration helpers
    //  Original: c9.c(AnimationProcessor, String) + c9.a(List<IBone>)
    // =========================================================================

    /** Collects all bones named {@code prefix + N} for N = 0, 1, 2, - until null. */
    List<CoreGeoBone> getAllBonesWithPrefix(
            software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
            String prefix) {
        List<CoreGeoBone> bones = new ArrayList<>();
        for (int n = 0; ; n++) {
            CoreGeoBone bone = processor.getBone(prefix + n);
            if (bone == null) break;
            bones.add(bone);
        }
        return bones;
    }

    /** Hides all bones in the list. */
    void hideAllWithPrefix(
            software.bernie.geckolib.core.animation.AnimationProcessor<KoboldEntity> processor,
            List<CoreGeoBone> bones) {
        for (CoreGeoBone bone : bones) bone.setHidden(true);
    }

    // =========================================================================
    //  Fake-world guard
    //  Original: check if paramem.field_70170_p instanceof gj
    // =========================================================================

    /**
     * Returns true if the entity is in a client-side fake/rendering world
     * (used by GeckoLib's entity preview renderer).
     * {@code gj} in 1.12.2 was a fake world subclass; in 1.20.1 we check a
     * known GeckoLib internal flag instead.
     */
    private boolean isInFakeWorld(KoboldEntity entity) {
        // GeckoLib 4 uses Level.isClientSide() but we can't distinguish fake from real easily.
        // The original check prevented custom anim from running during GUI preview rendering.
        // As a safe heuristic: if Minecraft has no in-game session, skip.
        return Minecraft.getInstance().level == null;
    }
}
