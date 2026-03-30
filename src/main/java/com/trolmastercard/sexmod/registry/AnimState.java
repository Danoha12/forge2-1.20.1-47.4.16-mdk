package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;

/**
 * AnimState — Portado de 1.12.2 a 1.20.1.
 * Nombres de estados originales restaurados para compatibilidad con modelos y lógica legacy.
 */
public enum AnimState {

    // -------------------------------------------------------------------------
    //  Blowjob / Oral
    // -------------------------------------------------------------------------
    NULL              (0,  false, true),
    STARTBLOWJOB      (2,  true,  false),
    SUCKBLOWJOB       (2,  true,  false),
    SUCKBLOWJOB_BLINK (2,  true,  true),
    CUMBLOWJOB        (0,  true,  false),
    THRUSTBLOWJOB     (2,  true,  false),

    // -------------------------------------------------------------------------
    //  Misc NPC actions
    // -------------------------------------------------------------------------
    PAYMENT           (5,  true,  false),
    STRIP             (5,  false, false),
    DASH              (2,  false, false),
    UNDRESS           (2,  false, true),
    DRESS             (2,  false, true),

    // -------------------------------------------------------------------------
    //  Hug
    // -------------------------------------------------------------------------
    HUG               (2,  true,  false),
    HUGIDLE           (0,  true,  true),
    HUGSELECTED       (0,  true,  false),

    // -------------------------------------------------------------------------
    //  Doggy
    // -------------------------------------------------------------------------
    STARTDOGGY        (2,  false, false),
    WAITDOGGY         (0,  false, true),
    DOGGYSTART        (0,  true,  false),
    DOGGYSLOW         (2,  true,  false),
    DOGGYFAST         (2,  true,  false),
    DOGGYCUM          (2,  true,  false),

    // -------------------------------------------------------------------------
    //  Sitting / Cowgirl
    // -------------------------------------------------------------------------
    SITDOWN           (2,  false, false, 60.0F,  -90.0F, true),
    SITDOWNIDLE       (0,  false, true,  60.0F,  -60.0F, true),
    COWGIRLSTART      (0,  true,  false, 60.0F,  -60.0F, false),
    COWGIRLSLOW       (10, true,  false, 60.0F,  -60.0F, false),
    COWGIRLFAST       (10, true,  false, 60.0F,  -60.0F, false),
    COWGIRLCUM        (2,  true,  false, 60.0F,  -60.0F, false),

    // -------------------------------------------------------------------------
    //  Combat / utility
    // -------------------------------------------------------------------------
    ATTACK            (0,  false, true),
    BOW               (2,  false, true),
    RIDE              (0,  false, true),
    SIT               (0,  false, true),
    THROW_PEARL       (0,  false, false),
    DOWNED            (7,  false, true),

    // -------------------------------------------------------------------------
    //  Paizuri
    // -------------------------------------------------------------------------
    PAIZURI_START           (0, true,  false, -56.0F, -90.0F, false, true),
    PAIZURI_IDLE            (0, true,  false, -56.0F, -90.0F, false, true),
    PAIZURI_SLOW            (0, true,  true,  -56.0F, -90.0F, false, true),
    PAIZURI_FAST            (0, true,  false, -56.0F, -90.0F, false, true),
    PAIZURI_FAST_CONTINUES  (0, true,  false, -56.0F, -90.0F, false, true),
    PAIZURI_CUM             (0, true,  false, -56.0F, -90.0F, false, true),

    // -------------------------------------------------------------------------
    //  Missionary
    // -------------------------------------------------------------------------
    MISSIONARY_START (0, true,  false, 30.0F, -90.0F, true),
    MISSIONARY_SLOW  (2, true,  false, 30.0F, -90.0F, true),
    MISSIONARY_FAST  (2, true,  false, 30.0F, -90.0F, true),
    MISSIONARY_CUM   (2, true,  false, 30.0F, -90.0F, true),

    // -------------------------------------------------------------------------
    //  Talk
    // -------------------------------------------------------------------------
    TALK_HORNY    (5, true,  false),
    TALK_IDLE     (0, true,  true),
    TALK_RESPONSE (2, true,  false),

    // -------------------------------------------------------------------------
    //  Anal
    // -------------------------------------------------------------------------
    ANAL_PREPARE (5,  false, false),
    ANAL_WAIT    (0,  false, true),
    ANAL_START   (0,  true,  false),
    ANAL_SLOW    (2,  true,  true),
    ANAL_FAST    (0,  true,  false),
    ANAL_CUM     (2,  true,  false),

    // -------------------------------------------------------------------------
    //  Kobold anal
    // -------------------------------------------------------------------------
    KOBOLD_ANAL_START (0, true, false, false, 4.0F, -80.0F, true),
    KOBOLD_ANAL_SLOW  (0, true, true,  false, 4.0F, -80.0F, true),
    KOBOLD_ANAL_FAST  (0, true, false, false, 4.0F, -80.0F, true),
    KOBOLD_ANAL_CUM   (2, true, false, false, 4.0F, -80.0F, true),

    // -------------------------------------------------------------------------
    //  Summon
    // -------------------------------------------------------------------------
    SUMMON              (0, false, false, false, true),
    SUMMON_WAIT         (0, false, true,  false, true),
    SUMMON_NORMAL       (0, false, false),
    SUMMON_SAND         (0, false, false),
    SUMMON_NORMAL_WAIT  (2, false, true),

    // -------------------------------------------------------------------------
    //  Head pat / Allie
    // -------------------------------------------------------------------------
    HEAD_PAT                (0, true,  false),
    ALLIE_PREPARE_FIRST_TIME(0, false, false, 40.0F, -40.0F, false),
    ALLIE_PREPARE_NORMAL    (2, false, false, 40.0F, -40.0F, false),

    // -------------------------------------------------------------------------
    //  Deepthroat
    // -------------------------------------------------------------------------
    DEEPTHROAT_START (0, true, false, true, 40.0F, -40.0F, false),
    DEEPTHROAT_SLOW  (2, true, false, true, 40.0F, -40.0F, false),
    DEEPTHROAT_FAST  (2, true, false, true, 40.0F, -40.0F, false),
    DEEPTHROAT_CUM   (2, true, false, true, 40.0F, -40.0F, false),

    // -------------------------------------------------------------------------
    //  Rich / Citizen
    // -------------------------------------------------------------------------
    RICH_FIRST_TIME (0, false, false),
    RICH_NORMAL     (0, false, false),
    CITIZEN_START   (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_SLOW    (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_FAST    (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_CUM     (2, true,  false, 10.0F, -90.0F, false),

    // -------------------------------------------------------------------------
    //  Fishing
    // -------------------------------------------------------------------------
    FISHING_START      (5, false, false),
    FISHING_IDLE       (0, false, true),
    FISHING_EAT        (0, false, false),
    FISHING_THROW_AWAY (0, false, false),

    // -------------------------------------------------------------------------
    //  Touch boobs
    // -------------------------------------------------------------------------
    TOUCH_BOOBS_INTRO (0, true, false),
    TOUCH_BOOBS_SLOW  (2, true, false),
    TOUCH_BOOBS_FAST  (2, true, false),
    TOUCH_BOOBS_CUM   (2, true, false),

    // -------------------------------------------------------------------------
    //  Cat / Cowgirl sitting
    // -------------------------------------------------------------------------
    WAIT_CAT               (0, false, false, 30.0F, -90.0F, true),
    COWGIRL_SITTING_INTRO  (0, true,  false),
    COWGIRL_SITTING_SLOW   (5, true,  false),
    COWGIRL_SITTING_FAST   (5, true,  false),
    COWGIRL_SITTING_CUM    (5, true,  false),

    // -------------------------------------------------------------------------
    //  Misc
    // -------------------------------------------------------------------------
    MINE  (0, false, false),
    SLEEP (5, false, false),

    // -------------------------------------------------------------------------
    //  Mating press
    // -------------------------------------------------------------------------
    MATING_PRESS_START (0, true, false, false, -50.0F, -90.0F, false),
    MATING_PRESS_SOFT  (0, true, false, -50.0F, -90.0F, false),
    MATING_PRESS_HARD  (0, true, false, -50.0F, -90.0F, false),
    MATING_PRESS_CUM   (2, true, false, -30.0F, -90.0F, false),

    // -------------------------------------------------------------------------
    //  Shoulder / pick-up
    // -------------------------------------------------------------------------
    SHOULDER_IDLE (0, false, true,  false, true),
    PICK_UP       (0, true,  false, 10.0F, -90.0F, true, true),

    // -------------------------------------------------------------------------
    //  Run / catch / throw
    // -------------------------------------------------------------------------
    RUN            (5, false, true),
    CATCH          (0, true,  false),
    CATCH_BJ       (0, true,  false),
    CATCH_BJ_IDLE  (0, true,  false),
    START_THROWING (0, true,  true),
    THROWN         (0, false, true),

    // -------------------------------------------------------------------------
    //  Jump
    // -------------------------------------------------------------------------
    JUMP_0 (0, true,  false),
    JUMP_1 (0, false, false),
    JUMP_2 (0, false, false),

    // -------------------------------------------------------------------------
    //  Breeding
    // -------------------------------------------------------------------------
    BREEDING_INTRO_0 (0, true,  false),
    BREEDING_INTRO_1 (0, false, false),
    BREEDING_INTRO_2 (0, false, false),
    BREEDING_SLOW_0  (0, true,  false),
    BREEDING_1       (0, false, false),
    BREEDING_SLOW_2  (5, false, false),
    BREEDING_FAST_0  (0, true,  false),
    BREEDING_FAST_2  (5, false, false),
    BREEDING_CUM_0   (0, true,  false),
    BREEDING_CUM_1   (0, false, false),
    BREEDING_CUM_2   (0, false, false),

    // -------------------------------------------------------------------------
    //  Misc state
    // -------------------------------------------------------------------------
    AWAIT_PICK_UP (0, false, true),
    VANISH        (0, false, true),
    STAND_UP      (0, false, false),

    // -------------------------------------------------------------------------
    //  Nelson hold
    // -------------------------------------------------------------------------
    NELSON_INTRO (0, true, false, 30.0F, -20.0F, false, true),
    NELSON_SLOW  (0, true, false, 30.0F, -20.0F, false, true),
    NELSON_FAST  (0, true, false, 30.0F, -20.0F, false, true),
    NELSON_CUM   (0, true, false, 30.0F, -20.0F, false, true),

    // -------------------------------------------------------------------------
    //  Carry (with followUp)
    // -------------------------------------------------------------------------
    CARRY_SLOW  (0, true, false, true, true),
    CARRY_FAST  (0, true, false, true, true),
    CARRY_CUM   (0, true, false, true, true),
    CARRY_INTRO (0, true, false, true, true, 191, CARRY_SLOW),

    // -------------------------------------------------------------------------
    //  Prone doggy (with followUp)
    // -------------------------------------------------------------------------
    PRONE_DOGGY_INTRO  (0, true, false, true, true),
    PRONE_DOGGY_SOFT   (0, true, false, true, true),
    PRONE_DOGGY_HARD   (0, true, false, true, true, 34,  PRONE_DOGGY_SOFT),
    PRONE_DOGGY_INSERT (2, true, false, true, true, 42,  PRONE_DOGGY_SOFT),
    PRONE_DOGGY_CUM    (0, true, false, true, true),

    // -------------------------------------------------------------------------
    //  Reverse cowgirl (with followUp)
    // -------------------------------------------------------------------------
    REVERSE_COWGIRL_SLOW           (0, true, false, true, 30.0F, -90.0F, true),
    REVERSE_COWGIRL_FAST_START     (0, true, false, true, 34, REVERSE_COWGIRL_SLOW, 30.0F, -90.0F, true),
    REVERSE_COWGIRL_FAST_CONTINUES (0, true, false, true, 39, REVERSE_COWGIRL_SLOW, 30.0F, -90.0F, true),
    REVERSE_COWGIRL_CUM            (0, true, false, true, 30.0F, -90.0F, true),
    REVERSE_COWGIRL_START          (0, true, false, true, 88, REVERSE_COWGIRL_SLOW, 30.0F, -90.0F, true),

    // -------------------------------------------------------------------------
    //  Wave (with followUp)
    // -------------------------------------------------------------------------
    WAVE_IDLE (0, false, false, false, true),
    WAVE      (0, false, false, true, false, 71, WAVE_IDLE),

    // -------------------------------------------------------------------------
    //  Flight / combat
    // -------------------------------------------------------------------------
    FLY              (0, false, true),
    SUMMON_SKELETON  (0, false, false),
    ATTACK_SWORD     (0, false, false),
    KNOCK_OUT_FLY    (5, false, false),
    KNOCK_OUT_GROUND (3, false, false),
    KNOCK_OUT_STAND_UP(0, false, false),

    // -------------------------------------------------------------------------
    //  Rape sequence (with followUp)
    // -------------------------------------------------------------------------
    RAPE_PREPARE  (0, false, false),
    RAPE_CHARGE   (0, false, false),
    RAPE_ON_GOING (0, true,  false, true,  60.0F, -30.0F, false),
    RAPE_INTRO    (0, true,  false, false, true, 46, RAPE_ON_GOING),
    RAPE_CUM_IDLE (0, true,  false, true),
    RAPE_CUM      (0, true,  false, true, 34, RAPE_CUM_IDLE, 60.0F, -30.0F, false),

    // -------------------------------------------------------------------------
    //  Corrupt (with followUp)
    // -------------------------------------------------------------------------
    CORRUPT_SLOW  (0, true, false, -30.0F, -90.0F, false),
    CORRUPT_FAST  (0, true, false, -30.0F, -90.0F, false),
    CORRUPT_CUM   (0, true, false, false,  -30.0F, -90.0F, false),
    CORRUPT_INTRO (0, true, false, true, 29, CORRUPT_SLOW),

    // -------------------------------------------------------------------------
    //  Galath flight (with followUp)
    // -------------------------------------------------------------------------
    CONTROLLED_FLIGHT (0, true,  true,  true, true),
    BOOST             (3, true,  false, true, true, 43, CONTROLLED_FLIGHT),
    GALATH_SUMMON     (0, false, false, false, true, 15, NULL),
    GALATH_DE_SUMMON  (0, false, false, false, true),
    GIVE_COIN         (0, true,  false, true,  true, 140, NULL),

    // -------------------------------------------------------------------------
    //  Masturbation / threesome / licking
    // -------------------------------------------------------------------------
    MASTERBATE          (0, false, false),
    HUG_MANG            (0, false, false, 239, NULL),
    RIDE_MOMMY_HEAD     (0, false, true),
    THREESOME_SLOW      (0, true,  false, false, true),
    THREESOME_FAST      (0, true,  false, false, true),
    THREESOME_CUM       (0, true,  false, false, true),
    PUSSY_LICKING       (0, false, true,  false),
    MASTERBATE_SITTING   (0, false, true,  false),
    MASTERBATE_SITTING_CUM(0, false, false, false),

    // -------------------------------------------------------------------------
    //  Morning blowjob
    // -------------------------------------------------------------------------
    MORNING_BLOWJOB_SLOW (0, true, true,  true),
    MORNING_BLOWJOB_FAST (0, true, true,  true),
    MORNING_BLOWJOB_CUM  (0, true, false, true);

    // =========================================================================
    //  Fields
    // =========================================================================

    public final int transitionTick;
    public final boolean hasPlayer;
    public final boolean autoBlink;
    public final float maxGirlPitch;
    public final float minGirlPitch;
    public final boolean flipGirlYaw;
    public int length;
    public int[] ticksPlaying = new int[]{0, 0};
    public AnimState followUp = null;
    public boolean useBoyCam;
    public boolean hideNameTag;

    // =========================================================================
    //  Constructors
    // =========================================================================

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink) {
        this(transitionTick, hasPlayer, autoBlink, 30.0F, -90.0F, false);
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam) {
        this(transitionTick, hasPlayer, autoBlink, 30.0F, -90.0F, false);
        this.useBoyCam = useBoyCam;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam, boolean hideNameTag) {
        this(transitionTick, hasPlayer, autoBlink, 30.0F, -90.0F, false);
        this.useBoyCam = useBoyCam;
        this.hideNameTag = hideNameTag;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, float maxPitch, float minPitch, boolean flipYaw) {
        this.transitionTick = transitionTick;
        this.hasPlayer = hasPlayer;
        this.autoBlink = autoBlink;
        this.maxGirlPitch = maxPitch;
        this.minGirlPitch = minPitch;
        this.flipGirlYaw = flipYaw;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, float maxPitch, float minPitch, boolean flipYaw, boolean hideNameTag) {
        this(transitionTick, hasPlayer, autoBlink, maxPitch, minPitch, flipYaw);
        this.hideNameTag = hideNameTag;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam, float maxPitch, float minPitch, boolean flipYaw) {
        this(transitionTick, hasPlayer, autoBlink, maxPitch, minPitch, flipYaw);
        this.useBoyCam = useBoyCam;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, int length, AnimState followUp) {
        this(transitionTick, hasPlayer, autoBlink);
        this.length = length;
        this.followUp = followUp;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam, int length, AnimState followUp) {
        this(transitionTick, hasPlayer, autoBlink, useBoyCam);
        this.length = length;
        this.followUp = followUp;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam, int length, AnimState followUp, float maxPitch, float minPitch, boolean flipYaw) {
        this(transitionTick, hasPlayer, autoBlink, maxPitch, minPitch, flipYaw);
        this.useBoyCam = useBoyCam;
        this.length = length;
        this.followUp = followUp;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean hideNameTag, boolean useBoyCam, int length, AnimState followUp) {
        this(transitionTick, hasPlayer, autoBlink, useBoyCam, hideNameTag);
        this.length = length;
        this.followUp = followUp;
    }

    // =========================================================================
    //  Static helpers (GeckoLib 4 Optimizado)
    // =========================================================================

    public static boolean isOneOf(AnimState state, AnimState... targets) {
        for (AnimState target : targets) {
            if (state == target) return true;
        }
        return false;
    }

    public static boolean isOneOf(BaseNpcEntity entity, AnimState... targets) {
        return isOneOf(entity.getAnimState(), targets);
    }

    public static double getAnimationLength(AnimationController<?> controller) {
        if (controller == null || controller.getCurrentAnimation() == null) return 0.0D;
        // GeckoLib 4: lengthInTicks() es lo más preciso para tu lógica de follow-up
        return controller.getCurrentAnimation().animation().length();
    }

    @OnlyIn(Dist.CLIENT)
    public static float getAnimationLength(BaseNpcEntity entity) {
        return (float) getAnimationLength(entity.getMainAnimationController());
    }

    @OnlyIn(Dist.CLIENT)
    public static float getElapsedTicks(BaseNpcEntity entity, float partialTick) {
        AnimationController<?> controller = entity.getMainAnimationController();
        if (controller == null) return 0.0F;
        // GeckoLib 4: Uso del manager oficial para obtener el tick exacto
        double managerTick = entity.getAnimatableInstanceCache()
                .getManagerForId(entity.getUUID().hashCode())
                .getTick(entity);
        return (float)(managerTick + partialTick - controller.tickOffset);
    }

    @OnlyIn(Dist.CLIENT)
    public static float getElapsedSeconds(BaseNpcEntity entity, float partialTick) {
        return getElapsedTicks(entity, partialTick) / 20.0F;
    }

    @OnlyIn(Dist.CLIENT)
    public static float getAnimationProgress(BaseNpcEntity entity, float partialTick) {
        float length = getAnimationLength(entity);
        if (length <= 0.0F) return 0.0F;
        // Sincronización basada en segundos para GeckoLib 4
        return Mth.clamp(getElapsedSeconds(entity, partialTick) / length, 0.0F, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isAnimationFinished(BaseNpcEntity entity, float partialTick) {
        return getAnimationProgress(entity, partialTick) >= 1.0F;
    }
}