package com.solwazi.slidemahjong;

/**
 * A single mahjong tile: a symbol glyph plus the tile's face color (ARGB int).
 * Immutable.
 */
public class Tile {

    /** The mahjong glyph, e.g. "🀄". */
    public final String symbol;

    /** Tile face color as an ARGB int (alpha always 0xFF). */
    public final int color;

    public Tile(String symbol, int color) {
        this.symbol = symbol;
        this.color = color;
    }

    public Tile copy() {
        return new Tile(symbol, color);
    }

    /**
     * Parses a "#rrggbb" (or "#aarrggbb") hex color into an ARGB int.
     * Pure Java so the game logic stays free of Android dependencies.
     */
    public static int parseColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        long value = Long.parseLong(h, 16);
        if (h.length() == 6) {
            value |= 0xFF000000L;
        }
        return (int) value;
    }
}
