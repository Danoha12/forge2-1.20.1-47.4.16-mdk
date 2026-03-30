package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * EyeAndKoboldColor — Portado a 1.20.1.
 * * Define los colores primarios y secundarios para el cuerpo y ojos de los Kobolds.
 * * Vincula los colores con los IDs de lana (woolMeta) para los ítems de spawn.
 */
public enum EyeAndKoboldColor {
    GREEN      ( 69, 141, 113,  91, 167, 128,  9, ChatFormatting.DARK_GREEN),
    YELLOW     (241, 177,  77, 255, 226, 170,  4, ChatFormatting.YELLOW),
    RED        (230,  27,  57, 253, 232, 239, 14, ChatFormatting.RED),
    PURPLE     (196, 148, 207, 246, 188,  96, 10, ChatFormatting.DARK_PURPLE),
    LIGHT_GREEN(170, 208,  47, 230, 214, 104,  5, ChatFormatting.GREEN),
    OLD_BLUE   (173, 138, 128, 118, 151, 180,  2, ChatFormatting.LIGHT_PURPLE),
    DARK_GREY  ( 92,  92, 110, 198, 193, 165,  7, ChatFormatting.DARK_GRAY),
    BROWN      (200, 145, 112, 253, 228, 198, 12, ChatFormatting.GOLD),
    DARK_BLUE  ( 65,  84, 116, 104, 137, 146, 11, ChatFormatting.DARK_BLUE),
    LIGHT_BLUE (100, 163, 206, 138, 235, 242,  3, ChatFormatting.DARK_AQUA),
    SILVER     (136, 136, 134, 255, 255, 255,  0, ChatFormatting.GRAY);

    private final Vec3i mainColor;
    private final Vec3i secondaryColor;
    private final int woolMeta;
    private final ChatFormatting textColor;
    private final Style textStyle;

    EyeAndKoboldColor(int r1, int g1, int b1, int r2, int g2, int b2, int woolMeta, ChatFormatting textColor) {
        this.mainColor = new Vec3i(r1, g1, b1);
        this.secondaryColor = new Vec3i(r2, g2, b2);
        this.woolMeta = woolMeta;
        this.textColor = textColor;
        // Pre-calculamos el estilo para no generar objetos nuevos en cada frame de renderizado
        this.textStyle = Style.EMPTY.withColor(TextColor.fromRgb((r1 << 16) | (g1 << 8) | b1));
    }

    public Vec3i getMainColor() { return mainColor; }
    public Vec3i getSecondaryColor() { return secondaryColor; }
    public int getWoolMeta() { return woolMeta; }
    public ChatFormatting getChatFormatting() { return textColor; }
    public Style getTextStyle() { return textStyle; }

    /**
     * Devuelve el color principal como un array de int {r, g, b}.
     * Útil para los renderizadores de GeckoLib.
     */
    public int[] getMainColorRGB() {
        return new int[]{ mainColor.getX(), mainColor.getY(), mainColor.getZ() };
    }

    // ── Métodos de Búsqueda y Seguridad ──────────────────────────────────────

    public static EyeAndKoboldColor safeValueOf(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return KoboldEntity.DEFAULT_COLOR; // Fallback seguro
        }
    }

    public static EyeAndKoboldColor getColorByWoolId(int id) {
        for (EyeAndKoboldColor color : values()) {
            if (color.woolMeta == id) return color;
        }
        return KoboldEntity.DEFAULT_COLOR;
    }

    public static EyeAndKoboldColor getByIndex(int index) {
        EyeAndKoboldColor[] vals = values();
        return vals[Math.max(0, Math.min(index, vals.length - 1))];
    }
}