package com.solwazi.slidemahjong;

import java.awt.Color;

/**
 * One mahjong tile: a symbol glyph plus its tile color.
 * Immutable; use {@link #copy()} for board simulations.
 */
public class Tile {
    public final String symbol;
    public final Color color;

    public Tile(String symbol, Color color) {
        this.symbol = symbol;
        this.color = color;
    }

    public Tile copy() {
        return new Tile(symbol, color);
    }
}
