package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * ModSounds — Portado a 1.20.1.
 * * Registra cientos de sonidos usando un DeferredRegister y Reflexión.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModConstants.MOD_ID);

    // ── Miscelánea ───────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] MISC_PLOB            = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_BELLJINGLE      = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_BEDRUSTLE       = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] MISC_SLAP            = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] MISC_TOUCH           = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] MISC_POUNDING        = new RegistryObject[35];
    public static final RegistryObject<SoundEvent>[] MISC_SMALLINSERTS    = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] MISC_INSERTS         = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] MISC_CUMINFLATION    = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_SCREAM          = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] MISC_FART            = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] MISC_JUMP            = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_EAT             = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] MISC_SLIDE           = new RegistryObject[7];
    public static final RegistryObject<SoundEvent>[] MISC_FLAP            = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] MISC_SHATTER         = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_WEOWEO          = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] MISC_BEEW            = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] MISC_CLAP            = new RegistryObject[1];
    public static final RegistryObject<SoundEvent>[] MISC_PYRO            = new RegistryObject[1];

    // ── Jenny ────────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_AFTERSESSIONMOAN = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_AHH              = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_BJMOAN           = new RegistryObject[13];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_GIGGLE           = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_HAPPYOH          = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_HEAVYBREATHING   = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_HMPH             = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_HUH              = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_LIGHTBREATHING   = new RegistryObject[12];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_LIPSOUND         = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_MMM              = new RegistryObject[9];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_MOAN             = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_SADOH            = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_JENNY_SIGH             = new RegistryObject[2];

    // ── Ellie ────────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_AFTERSESSIONMOAN = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_AHH              = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_BJMOAN           = new RegistryObject[13];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_GIGGLE           = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_HAPPYOH          = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_HEAVYBREATHING   = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_HMPH             = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_HUH              = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_LIGHTBREATHING   = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_LIPSOUND         = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_MMM              = new RegistryObject[9];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_MOAN             = new RegistryObject[9];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_SADOH            = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_SIGH             = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_COMETOMOMMY      = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_GOODBOY          = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ELLIE_MOMMYHORNY       = new RegistryObject[2];

    // ── Bia ──────────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_AHH    = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_BJMOAN = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_BREATH = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_GIGGLE = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_HEY    = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_HUH    = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] GIRLS_BIA_MMM    = new RegistryObject[8];

    // ── Luna ─────────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_AHH            = new RegistryObject[18];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_CUTENYA        = new RegistryObject[12];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_HAPPYOH        = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_HMPH           = new RegistryObject[6];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_HORNINYA       = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_HUH            = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_LIGHTBREATHING = new RegistryObject[25];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_MMM            = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_MOAN           = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_SADOH          = new RegistryObject[7];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_SIGH           = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_SINGING        = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_GIGGLE         = new RegistryObject[15];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_OUU            = new RegistryObject[13];
    public static final RegistryObject<SoundEvent>[] GIRLS_LUNA_OWO            = new RegistryObject[8];

    // ── Allie ────────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_AFTERSESSIONMOAN = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_AHH              = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_BJMOAN           = new RegistryObject[14];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_GIGGLE           = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_HAPPYOH          = new RegistryObject[3];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_HEAVYBREATHING   = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_HMPH             = new RegistryObject[5];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_HUH              = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_LIGHTBREATHING   = new RegistryObject[11];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_LIPSOUND         = new RegistryObject[14];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_MMM              = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_MOAN             = new RegistryObject[8];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_SADOH            = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_SIGH             = new RegistryObject[2];
    public static final RegistryObject<SoundEvent>[] GIRLS_ALLIE_SCAWY            = new RegistryObject[3];

    // ── Kobold ───────────────────────────────────────────────────────────────
    public static final RegistryObject<SoundEvent>[] GIRLS_KOBOLD_BJMOAN         = new RegistryObject[10];
    public static final RegistryObject<SoundEvent>[] GIRLS_KOBOLD_GIGGLE         = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_KOBOLD_HMPH           = new RegistryObject[4];
    public static final RegistryObject<SoundEvent>[] GIRLS_KOBOLD_LIPSOUND       = new RegistryObject[6];
    public static final RegistryObject<SoundEvent>[] GIRLS_KOBOLD_MOAN           = new RegistryObject[9];

    // ── Registro Automático (Reflexión 1.20.1) ───────────────────────────────

    /**
     * Llama a esto desde el constructor de la clase Main.
     */
    public static void register(IEventBus eventBus) {
        try {
            // Itera sobre todos los campos estáticos de esta clase
            for (Field field : ModSounds.class.getDeclaredFields()) {
                // Si el campo es un Array de RegistryObject<SoundEvent>
                if (field.getType().isArray() && field.getType().getComponentType() == RegistryObject.class) {

                    @SuppressWarnings("unchecked")
                    RegistryObject<SoundEvent>[] array = (RegistryObject<SoundEvent>[]) field.get(null);

                    // Convertimos el nombre de la variable (ej: GIRLS_JENNY_MOAN) a minúsculas
                    String baseName = field.getName().toLowerCase(Locale.ROOT);

                    // Reemplazamos los "_" por "." para que coincida con el formato de sounds.json
                    baseName = baseName.replace('_', '.');

                    // Registramos cada sonido en el DeferredRegister y lo guardamos en el array
                    for (int i = 0; i < array.length; i++) {
                        String soundName = baseName + "." + i;
                        array[i] = registerSound(soundName);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            System.err.println("[SexMod] Error crítico al registrar sonidos: " + e.getMessage());
        }

        SOUNDS.register(eventBus);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ModConstants.MOD_ID, name)));
    }

    // ── Reproductor Aleatorio ────────────────────────────────────────────────

    /**
     * Obtiene un sonido aleatorio de un array específico.
     */
    public static SoundEvent getRandom(RegistryObject<SoundEvent>[] soundArray, RandomSource random) {
        if (soundArray == null || soundArray.length == 0) return null;
        return soundArray[random.nextInt(soundArray.length)].get();
    }
}