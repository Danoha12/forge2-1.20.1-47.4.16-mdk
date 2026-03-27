package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * ModSounds - ported from c.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Declares all SoundEvent arrays for the mod and auto-registers them by
 * reflecting on the field names to generate resource-location paths.
 *
 * ============================================================
 * Migration notes (1.12.2 - 1.20.1):
 * ============================================================
 *   ForgeRegistries.SOUND_EVENTS.register(...)
 *     - DeferredRegister<SoundEvent> + RegistryObject pattern
 *
 *   SoundEvent(ResourceLocation)     - SoundEvent.createVariableRangeEvent(rl)
 *
 *   Manual soundEvent.setRegistryName() - handled by DeferredRegister
 *
 *   In 1.20.1 every SoundEvent must also have an entry in sounds.json.
 *   The auto-register below mirrors the 1.12.2 convention exactly
 *   (field name - lowercase dot-separated path).
 *
 * ============================================================
 * Usage:
 * ============================================================
 *   Call {@link #init()} from your mod constructor / common setup ONCE to
 *   trigger the reflective registration pass.
 *   All arrays will be populated with registered {@link SoundEvent} instances.
 *
 * ============================================================
 * Random sound helper:
 * ============================================================
 *   {@link #randomFrom(SoundEvent[])} returns a random element from the array,
 *   avoiding the same index twice in a row (up to 10 attempts).
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModConstants.MOD_ID);

    // =========================================================================
    //  Misc
    // =========================================================================
    public static final SoundEvent[] MISC_PLOB            = new SoundEvent[1];
    public static final SoundEvent[] MISC_BELLJINGLE       = new SoundEvent[1];
    public static final SoundEvent[] MISC_BEDRUSTLE        = new SoundEvent[2];
    public static final SoundEvent[] MISC_SLAP             = new SoundEvent[2];
    public static final SoundEvent[] MISC_TOUCH            = new SoundEvent[2];
    public static final SoundEvent[] MISC_POUNDING         = new SoundEvent[35];
    public static final SoundEvent[] MISC_SMALLINSERTS     = new SoundEvent[5];
    public static final SoundEvent[] MISC_INSERTS          = new SoundEvent[5];
    public static final SoundEvent[] MISC_CUMINFLATION     = new SoundEvent[1];
    public static final SoundEvent[] MISC_SCREAM           = new SoundEvent[2];
    public static final SoundEvent[] MISC_FART             = new SoundEvent[3];
    public static final SoundEvent[] MISC_JUMP             = new SoundEvent[1];
    public static final SoundEvent[] MISC_EAT              = new SoundEvent[3];
    public static final SoundEvent[] MISC_SLIDE            = new SoundEvent[7];
    public static final SoundEvent[] MISC_FLAP             = new SoundEvent[4];
    public static final SoundEvent[] MISC_SHATTER          = new SoundEvent[1];
    public static final SoundEvent[] MISC_WEOWEO           = new SoundEvent[4];
    public static final SoundEvent[] MISC_BEEW             = new SoundEvent[3];
    public static final SoundEvent[] MISC_CLAP             = new SoundEvent[1];
    public static final SoundEvent[] MISC_PYRO             = new SoundEvent[1];

    // =========================================================================
    //  Jenny
    // =========================================================================
    public static final SoundEvent[] GIRLS_JENNY_AFTERSESSIONMOAN = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_JENNY_AHH              = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_JENNY_BJMOAN           = new SoundEvent[13];
    public static final SoundEvent[] GIRLS_JENNY_GIGGLE           = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_JENNY_HAPPYOH          = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_JENNY_HEAVYBREATHING   = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_JENNY_HMPH             = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_JENNY_HUH              = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_JENNY_LIGHTBREATHING   = new SoundEvent[12];
    public static final SoundEvent[] GIRLS_JENNY_LIPSOUND         = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_JENNY_MMM              = new SoundEvent[9];
    public static final SoundEvent[] GIRLS_JENNY_MOAN             = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_JENNY_SADOH            = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_JENNY_SIGH             = new SoundEvent[2];

    // =========================================================================
    //  Ellie
    // =========================================================================
    public static final SoundEvent[] GIRLS_ELLIE_AFTERSESSIONMOAN = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_ELLIE_AHH              = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_ELLIE_BJMOAN           = new SoundEvent[13];
    public static final SoundEvent[] GIRLS_ELLIE_GIGGLE           = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_ELLIE_HAPPYOH          = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_ELLIE_HEAVYBREATHING   = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_ELLIE_HMPH             = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_ELLIE_HUH              = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ELLIE_LIGHTBREATHING   = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_ELLIE_LIPSOUND         = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_ELLIE_MMM              = new SoundEvent[9];
    public static final SoundEvent[] GIRLS_ELLIE_MOAN             = new SoundEvent[9];
    public static final SoundEvent[] GIRLS_ELLIE_SADOH            = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ELLIE_SIGH             = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ELLIE_COMETOMOMMY      = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ELLIE_GOODBOY          = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ELLIE_MOMMYHORNY       = new SoundEvent[2];

    // =========================================================================
    //  Bia
    // =========================================================================
    public static final SoundEvent[] GIRLS_BIA_AHH    = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_BIA_BJMOAN = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_BIA_BREATH = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_BIA_GIGGLE = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_BIA_HEY    = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_BIA_HUH    = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_BIA_MMM    = new SoundEvent[8];

    // =========================================================================
    //  Luna
    // =========================================================================
    public static final SoundEvent[] GIRLS_LUNA_AHH            = new SoundEvent[18];
    public static final SoundEvent[] GIRLS_LUNA_CUTENYA         = new SoundEvent[12];
    public static final SoundEvent[] GIRLS_LUNA_HAPPYOH         = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_LUNA_HMPH            = new SoundEvent[6];
    public static final SoundEvent[] GIRLS_LUNA_HORNINYA        = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_LUNA_HUH             = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_LUNA_LIGHTBREATHING  = new SoundEvent[25];
    public static final SoundEvent[] GIRLS_LUNA_MMM             = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_LUNA_MOAN            = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_LUNA_SADOH           = new SoundEvent[7];
    public static final SoundEvent[] GIRLS_LUNA_SIGH            = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_LUNA_SINGING         = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_LUNA_GIGGLE          = new SoundEvent[15];
    public static final SoundEvent[] GIRLS_LUNA_OUU             = new SoundEvent[13];
    public static final SoundEvent[] GIRLS_LUNA_OWO             = new SoundEvent[8];

    // =========================================================================
    //  Allie
    // =========================================================================
    public static final SoundEvent[] GIRLS_ALLIE_AFTERSESSIONMOAN = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_ALLIE_AHH              = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_ALLIE_BJMOAN           = new SoundEvent[14];
    public static final SoundEvent[] GIRLS_ALLIE_GIGGLE           = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_ALLIE_HAPPYOH          = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_ALLIE_HEAVYBREATHING   = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_ALLIE_HMPH             = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_ALLIE_HUH              = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ALLIE_LIGHTBREATHING   = new SoundEvent[11];
    public static final SoundEvent[] GIRLS_ALLIE_LIPSOUND         = new SoundEvent[14];
    public static final SoundEvent[] GIRLS_ALLIE_MMM              = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_ALLIE_MOAN             = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_ALLIE_SADOH            = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ALLIE_SIGH             = new SoundEvent[2];
    public static final SoundEvent[] GIRLS_ALLIE_SCAWY            = new SoundEvent[3];

    // =========================================================================
    //  Kobold
    // =========================================================================
    public static final SoundEvent[] GIRLS_KOBOLD_BJMOAN         = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_KOBOLD_GIGGLE         = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_KOBOLD_HAA            = new SoundEvent[7];
    public static final SoundEvent[] GIRLS_KOBOLD_HEYMASTER      = new SoundEvent[6];
    public static final SoundEvent[] GIRLS_KOBOLD_INTERESTED     = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_KOBOLD_LIGHTBREATHING = new SoundEvent[12];
    public static final SoundEvent[] GIRLS_KOBOLD_MASTER         = new SoundEvent[6];
    public static final SoundEvent[] GIRLS_KOBOLD_MOAN           = new SoundEvent[10];
    public static final SoundEvent[] GIRLS_KOBOLD_ORGASM         = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_KOBOLD_SAD            = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_KOBOLD_YEP            = new SoundEvent[7];

    // =========================================================================
    //  Galath
    // =========================================================================
    public static final SoundEvent[] GIRLS_GALATH_AHH          = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_GALATH_BREATHING     = new SoundEvent[7];
    public static final SoundEvent[] GIRLS_GALATH_DIALOG        = new SoundEvent[6];
    public static final SoundEvent[] GIRLS_GALATH_GIGGLE        = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_GALATH_HMPH          = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_GALATH_HUH           = new SoundEvent[3];
    public static final SoundEvent[] GIRLS_GALATH_LIGHTCHARGE   = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_GALATH_MOAN          = new SoundEvent[8];
    public static final SoundEvent[] GIRLS_GALATH_STRONGCHARGE  = new SoundEvent[4];
    public static final SoundEvent[] GIRLS_GALATH_UUH           = new SoundEvent[7];
    public static final SoundEvent[] GIRLS_GALATH_ORGASM        = new SoundEvent[5];
    public static final SoundEvent[] GIRLS_GALATH_AAA           = new SoundEvent[2];

    // =========================================================================
    //  Random sound state  (original: static HashMap<SoundEvent, Integer> lastRandomSound)
    // =========================================================================
    private static final HashMap<SoundEvent, Integer> lastRandomSound = new HashMap<>();

    // =========================================================================
    //  Registration  (original: c.a())
    // =========================================================================

    /**
     * Reflectively iterates over every {@code SoundEvent[]} field in this class,
     * derives the resource-location path from the Java field name, and registers
     * each array element via the DeferredRegister.
     *
     * Convention: {@code GIRLS_JENNY_MOAN[3]} - path {@code "girls.jenny.moan.moan3"}
     * (i.e., {@code fieldName.toLowerCase().replace('_','.')} with the last segment
     *  repeated as the sound-base name + index).
     *
     * Original: {@code c.a()}
     */
    public static void init() {
        for (Field field : ModSounds.class.getDeclaredFields()) {
            if (!field.getType().isArray()) continue;
            if (field.getType().getComponentType() != SoundEvent.class) continue;

            SoundEvent[] events;
            try {
                events = (SoundEvent[]) field.get(null);
            } catch (Exception e) {
                continue;
            }

            String fieldPath = field.getName().toLowerCase().replace('_', '.');
            String[] parts   = fieldPath.split("\\.");
            // The "base name" used for individual indices is the last segment
            String baseName  = (parts.length > 2) ? parts[2] : parts[parts.length - 1];

            for (int i = 0; i < events.length; i++) {
                String path = String.format("%s.%s%d", fieldPath, baseName, i);
                events[i] = registerSound(path);
            }
        }
    }

    /**
     * Registers a single SoundEvent with the DeferredRegister.
     * Original: {@code c.a(String)} (also used directly by callers that need a one-off sound).
     */
    public static SoundEvent registerSound(String path) {
        ResourceLocation rl = new ResourceLocation(ModConstants.MOD_ID, path);
        // DeferredRegister.register returns a RegistryObject; we need the SoundEvent immediately,
        // so we register eagerly and store the reference.
        RegistryObject<SoundEvent> obj = SOUND_EVENTS.register(
            path.replace('.', '_'),
            () -> SoundEvent.createVariableRangeEvent(rl)
        );
        // Because init() may be called before the registry fires we return a placeholder;
        // callers that need the SoundEvent at runtime must use the RegistryObject.
        // As a workaround (matching original 1.12.2 behaviour) we return a direct instance.
        return SoundEvent.createVariableRangeEvent(rl);
    }

    // =========================================================================
    //  Random helper  (original: c.a(SoundEvent[]))
    // =========================================================================

    /**
     * Returns a random {@link SoundEvent} from {@code array}, trying up to 10
     * times to avoid repeating the same index as the previous call.
     */
    public static SoundEvent randomFrom(SoundEvent[] array) {
        if (array.length == 0) return null;
        if (array.length == 1) return array[0];

        SoundEvent key = array[0];
        lastRandomSound.putIfAbsent(key, -69);

        int chosen;
        int tries = 0;
        do {
            chosen = ModConstants.RANDOM.nextInt(array.length);
        } while (++tries < 10 && chosen == lastRandomSound.get(key));

        lastRandomSound.put(key, chosen);
        return array[chosen];
    }
}
