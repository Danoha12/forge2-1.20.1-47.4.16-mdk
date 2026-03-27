package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animation.AnimationController;

/**
 * AnimState - PORT COMPLETO 1.20.1
 * Se han mantenido TODAS las constantes originales para asegurar la estabilidad del mod.
 * Los nombres han sido neutralizados pero la lógica técnica es 100% fiel al original.
 */
public enum AnimState {

    // --- INTERACCIONES DE REGALO (Original: Blowjob) ---
    NULL              (0,  false, true),
    GIFT_START        (2,  true,  false),
    GIFT_LOOP         (2,  true,  false),
    GIFT_BLINK        (2,  true,  true),
    GIFT_FINISH       (0,  true,  false),
    GIFT_ACTION       (2,  true,  false),

    // --- ACCIONES GENERALES ---
    PAYMENT           (5,  true,  false),
    STRIP             (5,  false, false), // Ahora es "Cambiar Ropa"
    DASH              (2,  false, false),
    UNDRESS           (2,  false, true),
    DRESS             (2,  false, true),

    // --- AFECTO ---
    HUG               (2,  true,  false),
    HUGIDLE           (0,  true,  true),
    HUGSELECTED       (0,  true,  false),

    // --- JUEGOS DE PERSECUCIÓN (Original: Doggy) ---
    GAME_CHASE_START  (2,  false, false),
    GAME_CHASE_WAIT   (0,  false, true),
    GAME_CHASE_ACTIVE (0,  true,  false),
    GAME_CHASE_SLOW   (2,  true,  false),
    GAME_CHASE_FAST   (2,  true,  false),
    GAME_CHASE_FINISH (2,  true,  false),

    // --- BAILE / SENTARSE (Original: Cowgirl) ---
    SITDOWN           (2,  false, false, 60.0F,  -90.0F, true),
    SITDOWNIDLE       (0,  false, true,  60.0F,  -60.0F, true),
    DANCE_START       (0,  true,  false, 60.0F,  -60.0F, false),
    DANCE_SLOW        (10, true,  false, 60.0F,  -60.0F, false),
    DANCE_FAST        (10, true,  false, 60.0F,  -60.0F, false),
    DANCE_FINISH      (2,  true,  false, 60.0F,  -60.0F, false),

    // --- COMBATE Y UTILIDAD ---
    ATTACK        (0,  false, true),
    BOW           (2,  false, true),
    RIDE          (0,  false, true),
    SIT           (0,  false, true),
    THROW_PEARL   (0,  false, false),
    DOWNED        (7,  false, true),

    // --- ABRAZO CERCANO (Original: Paizuri) ---
    CLOSE_HUG_START           (0, true,  false, -56.0F, -90.0F, false, true),
    CLOSE_HUG_IDLE            (0, true,  false, -56.0F, -90.0F, false, true),
    CLOSE_HUG_SLOW            (0, true,  true,  -56.0F, -90.0F, false, true),
    CLOSE_HUG_FAST            (0, true,  false, -56.0F, -90.0F, false, true),
    CLOSE_HUG_FAST_CONTINUES  (0, true,  false, -56.0F, -90.0F, false, true),
    CLOSE_HUG_FINISH          (0, true,  false, -56.0F, -90.0F, false, true),

    // --- DESCANSO (Original: Missionary) ---
    REST_START (0, true,  false, 30.0F, -90.0F, true),
    REST_SLOW  (2, true,  false, 30.0F, -90.0F, true),
    REST_FAST  (2, true,  false, 30.0F, -90.0F, true),
    REST_FINISH(2, true,  false, 30.0F, -90.0F, true),

    // --- CHARLA ---
    TALK_HAPPY    (5, true,  false), // Antes TALK_HORNY
    TALK_IDLE     (0, true,  true),
    TALK_RESPONSE (2, true,  false),

    // --- MINERÍA / TRABAJO (Original: Anal) ---
    WORK_PREPARE (5,  false, false),
    WORK_WAIT    (0,  false, true),
    WORK_START   (0,  true,  false),
    WORK_SLOW    (2,  true,  true),
    WORK_FAST    (0,  true,  false),
    WORK_FINISH  (2,  true,  false),

    // --- KOBOLD TRABAJO ---
    KOBOLD_WORK_START (0, true, false, false, 4.0F, -80.0F, true),
    KOBOLD_WORK_SLOW  (0, true, true,  false, 4.0F, -80.0F, true),
    KOBOLD_WORK_FAST  (0, true, false, false, 4.0F, -80.0F, true),
    KOBOLD_WORK_FINISH(2, true, false, false, 4.0F, -80.0F, true),

    // --- SUMMON ---
    SUMMON              (0, false, false, false, true),
    SUMMON_WAIT         (0, false, true,  false, true),
    SUMMON_NORMAL       (0, false, false),
    SUMMON_SAND         (0, false, false),
    SUMMON_NORMAL_WAIT  (2, false, true),

    // --- ALLIE / HEAD PAT ---
    HEAD_PAT                (0, true,  false),
    ALLIE_PREPARE_FIRST_TIME(0, false, false, 40.0F, -40.0F, false),
    ALLIE_PREPARE_NORMAL    (2, false, false, 40.0F, -40.0F, false),

    // --- MAGIA PROFUNDA (Original: Deepthroat) ---
    MAGIC_DEEP_START (0, true, false, true, 40.0F, -40.0F, false),
    MAGIC_DEEP_SLOW  (2, true, false, true, 40.0F, -40.0F, false),
    MAGIC_DEEP_FAST  (2, true, false, true, 40.0F, -40.0F, false),
    MAGIC_DEEP_FINISH(2, true, false, true, 40.0F, -40.0F, false),

    // --- DESEOS / CIUDADANO ---
    RICH_FIRST_TIME (0, false, false),
    RICH_NORMAL     (0, false, false),
    CITIZEN_START   (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_SLOW    (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_FAST    (0, true,  false, 10.0F, -90.0F, false),
    CITIZEN_FINISH  (2, true,  false, 10.0F, -90.0F, false),

    // --- PESCA ---
    FISHING_START      (5, false, false),
    FISHING_IDLE       (0, false, true),
    FISHING_EAT        (0, false, false),
    FISHING_THROW_AWAY (0, false, false),

    // --- CARICIAS (Original: Touch boobs) ---
    TOUCH_ARM_INTRO (0, true, false),
    TOUCH_ARM_SLOW  (2, true, false),
    TOUCH_ARM_FAST  (2, true, false),
    TOUCH_ARM_FINISH(2, true, false),

    // --- GATO / BAILE SENTADO ---
    WAIT_CAT               (0, false, false, 30.0F, -90.0F, true),
    DANCE_SITTING_INTRO    (0, true,  false),
    DANCE_SITTING_SLOW     (5, true,  false),
    DANCE_SITTING_FAST     (5, true,  false),
    DANCE_SITTING_FINISH   (5, true,  false),

    // --- MISC ---
    MINE  (0, false, false),
    SLEEP (5, false, false),

    // --- PRESIÓN DE COOPERACIÓN (Original: Mating press) ---
    COOP_PRESS_START (0, true, false, false, -50.0F, -90.0F, false),
    COOP_PRESS_SOFT  (0, true, false, -50.0F, -90.0F, false),
    COOP_PRESS_HARD  (0, true, false, -50.0F, -90.0F, false),
    COOP_PRESS_FINISH(2, true, false, -30.0F, -90.0F, false),

    // --- CARGAR / RECOGER ---
    SHOULDER_IDLE (0, false, true,  false, true),
    PICK_UP       (0, true,  false, 10.0F, -90.0F, true, true),

    // --- CORRER / ATRAPAR ---
    RUN            (5, false, true),
    CATCH          (0, true,  false),
    CATCH_ACTION   (0, true,  false),
    CATCH_IDLE     (0, true,  false),
    START_THROWING (0, true,  true),
    THROWN         (0, false, true),

    // --- SALTO ---
    JUMP_0 (0, true,  false),
    JUMP_1 (0, false, false),
    JUMP_2 (0, false, false),

    // --- ENTRENAMIENTO (Original: Breeding) ---
    TRAIN_INTRO_0 (0, true,  false),
    TRAIN_INTRO_1 (0, false, false),
    TRAIN_INTRO_2 (0, false, false),
    TRAIN_SLOW_0  (0, true,  false),
    TRAIN_1       (0, false, false),
    TRAIN_SLOW_2  (5, false, false),
    TRAIN_FAST_0  (0, true,  false),
    TRAIN_FAST_2  (5, false, false),
    TRAIN_FINISH_0(0, true,  false),
    TRAIN_FINISH_1(0, false, false),
    TRAIN_FINISH_2(0, false, false),

    // --- MISC STATE ---
    AWAIT_PICK_UP (0, false, true),
    VANISH        (0, false, true),
    STAND_UP      (0, false, false),

    // --- LUCHA (Original: Nelson hold) ---
    WRESTLE_INTRO (0, true, false, 30.0F, -20.0F, false, true),
    WRESTLE_SLOW  (0, true, false, 30.0F, -20.0F, false, true),
    WRESTLE_FAST  (0, true, false, 30.0F, -20.0F, false, true),
    WRESTLE_FINISH(0, true, false, 30.0F, -20.0F, false, true),

    // --- TRANSPORTE (CARRY) ---
    CARRY_SLOW  (0, true, false, true, true),
    CARRY_FAST  (0, true, false, true, true),
    CARRY_FINISH(0, true, false, true, true),
    CARRY_INTRO (0, true, false, true, true, 191, CARRY_SLOW),

    // --- JUEGO EN EL SUELO (Original: Prone doggy) ---
    FLOOR_GAME_INTRO  (0, true, false, true, true),
    FLOOR_GAME_SOFT   (0, true, false, true, true),
    FLOOR_GAME_HARD   (0, true, false, true, true, 34,  FLOOR_GAME_SOFT),
    FLOOR_GAME_INSERT (2, true, false, true, true, 42,  FLOOR_GAME_SOFT),
    FLOOR_GAME_FINISH (0, true, false, true, true),

    // --- BAILE REVERSO (Original: Reverse cowgirl) ---
    REV_DANCE_SLOW           (0, true, false, true, 30.0F, -90.0F, true),
    REV_DANCE_FAST_START     (0, true, false, true, 34, REV_DANCE_SLOW, 30.0F, -90.0F, true),
    REV_DANCE_FAST_CONTINUE  (0, true, false, true, 39, REV_DANCE_SLOW, 30.0F, -90.0F, true),
    REV_DANCE_FINISH         (0, true, false, true, 30.0F, -90.0F, true),
    REV_DANCE_START          (0, true, false, true, 88, REV_DANCE_SLOW, 30.0F, -90.0F, true),

    // --- SALUDO (WAVE) ---
    WAVE_IDLE (0, false, false, false, true),
    WAVE      (0, false, false, true, false, 71, WAVE_IDLE),

    // --- VUELO / COMBATE ---
    FLY               (0, false, true),
    SUMMON_SKELETON   (0, false, false),
    ATTACK_SWORD      (0, false, false),
    KNOCK_OUT_FLY     (5, false, false),
    KNOCK_OUT_GROUND  (3, false, false),
    KNOCK_OUT_STAND_UP(0, false, false),

    // --- ACCIÓN DE SORPRESA (Original: Rape) ---
    SURPRISE_PREPARE (0, false, false),
    SURPRISE_CHARGE  (0, false, false),
    SURPRISE_ACTIVE  (0, true,  false, true,  60.0F, -30.0F, false),
    SURPRISE_INTRO   (0, true,  false, false, true, 46, SURPRISE_ACTIVE),
    SURPRISE_IDLE    (0, true,  false, true),
    SURPRISE_FINISH  (0, true,  false, true, 34, SURPRISE_IDLE, 60.0F, -30.0F, false),

    // --- CORRUPCIÓN ---
    CORRUPT_SLOW  (0, true, false, -30.0F, -90.0F, false),
    CORRUPT_FAST  (0, true, false, -30.0F, -90.0F, false),
    CORRUPT_FINISH(0, true, false, false,  -30.0F, -90.0F, false),
    CORRUPT_INTRO (0, true, false, true, 29, CORRUPT_SLOW),

    // --- VUELO GALATH ---
    CONTROLLED_FLIGHT (0, true,  true,  true, true),
    BOOST             (3, true,  false, true, true, 43, CONTROLLED_FLIGHT),
    GALATH_SUMMON     (0, false, false, false, true, 15, NULL),
    GALATH_DE_SUMMON  (0, false, false, false, true),
    GIVE_COIN         (0, true,  false, true,  true, 140, NULL),

    // --- TRABAJO EN EQUIPO / TERCERO ---
    TEAM_WORK_SLOW      (0, true,  false, false, true),
    TEAM_WORK_FAST      (0, true,  false, false, true),
    TEAM_WORK_FINISH    (0, true,  false, false, true),
    LICKING             (0, false, true,  false),
    SITTING_ACTION      (0, false, true,  false),
    SITTING_FINISH      (0, false, false, false),

    // --- ACCIÓN MATUTINA ---
    MORNING_ACTION_SLOW (0, true, true,  true),
    MORNING_ACTION_FAST (0, true, true,  true),
    MORNING_ACTION_FINISH(0, true, false, true);

    // ==========================================
    // VARIABLES TÉCNICAS (ESTRUCTURA ORIGINAL)
    // ==========================================
    public final int transitionTick;
    public final boolean hasPlayer;
    public final boolean autoBlink;
    public final float maxGirlPitch;
    public final float minGirlPitch;
    public final boolean flipGirlYaw;
    public int length;
    public AnimState followUp = null;
    public boolean useBoyCam;
    public boolean hideNameTag;

    // --- CONSTRUCTORES ---
    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink) {
        this.transitionTick = transitionTick;
        this.hasPlayer = hasPlayer;
        this.autoBlink = autoBlink;
        this.maxGirlPitch = 30.0F;
        this.minGirlPitch = -90.0F;
        this.flipGirlYaw = false;
        this.useBoyCam = false;
        this.hideNameTag = false;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam) {
        this(transitionTick, hasPlayer, autoBlink);
        this.useBoyCam = useBoyCam;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean useBoyCam, boolean hideNameTag) {
        this(transitionTick, hasPlayer, autoBlink, useBoyCam);
        this.hideNameTag = hideNameTag;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, float maxPitch, float minPitch, boolean flip) {
        this(transitionTick, hasPlayer, autoBlink);
        this.maxGirlPitch = maxPitch;
        this.minGirlPitch = minPitch;
        this.flipGirlYaw = flip;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, float maxPitch, float minPitch, boolean flip, boolean hideTag) {
        this(transitionTick, hasPlayer, autoBlink, maxPitch, minPitch, flip);
        this.hideNameTag = hideTag;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean boyCam, float maxPitch, float minPitch, boolean flip) {
        this(transitionTick, hasPlayer, autoBlink, maxPitch, minPitch, flip);
        this.useBoyCam = boyCam;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, int len, AnimState follow) {
        this(transitionTick, hasPlayer, autoBlink);
        this.length = len;
        this.followUp = follow;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean boyCam, int len, AnimState follow) {
        this(transitionTick, hasPlayer, autoBlink, boyCam);
        this.length = len;
        this.followUp = follow;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean boyCam, int len, AnimState follow, float max, float min, boolean flip) {
        this(transitionTick, hasPlayer, autoBlink, boyCam, max, min, flip);
        this.length = len;
        this.followUp = follow;
    }

    AnimState(int transitionTick, boolean hasPlayer, boolean autoBlink, boolean hideTag, boolean boyCam, int len, AnimState follow) {
        this(transitionTick, hasPlayer, autoBlink, boyCam, hideTag);
        this.length = len;
        this.followUp = follow;
    }

    // ==========================================
    // HELPERS ESTÁTICOS
    // ==========================================
    public static boolean isOneOf(AnimState state, AnimState... targets) {
        for (AnimState t : targets) if (state == t) return true;
        return false;
    }

    public static double getAnimationLength(AnimationController<?> controller) {
        if (controller == null || controller.getCurrentAnimation() == null) return 0.0D;
        return controller.getCurrentAnimation().animation().lengthInTicks() / 20.0;
    }

    @OnlyIn(Dist.CLIENT)
    public static float getAnimationProgress(BaseNpcEntity entity, float partialTick) {
        double length = getAnimationLength(entity.getMainAnimationController());
        if (length <= 0.0) return 0.0F;
        double tick = entity.getAnimatableInstanceCache().getManagerForId(entity.getUUID().hashCode()).getTick(entity);
        return Mth.clamp((float)(tick / (length * 20.0)), 0.0F, 1.0F);
    }
}