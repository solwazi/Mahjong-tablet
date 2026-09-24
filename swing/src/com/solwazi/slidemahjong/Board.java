package com.solwazi.slidemahjong;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * UI-agnostic game logic for Shift Mahjong Daily.
 *
 * <p>The board is a flat {@code Tile[36]} array, index = row * 6 + col.
 * A move is always simulated on a copy of the board first and is only
 * committed when at least one of the moved tiles is part of a
 * line-of-sight match on the resulting board. Sliding a tile merely
 * out of the way so two untouched tiles match does not count.</p>
 */
public class Board {

    public static final int GRID_SIZE = 6;
    public static final int CELL_COUNT = GRID_SIZE * GRID_SIZE;

    /** Mahjong symbols, in palette order. */
    public static final String[] SYMBOLS = {
        "🀀", "🀁", "🀂", "🀃", "🀄", "🀅",
        "🀆", "🀇", "🀏", "🀕", "🀟", "🀤"
    };

    /** Per-symbol tile colors, matching SYMBOLS order. */
    public static final String[] COLOR_HEX = {
        "#f87171", "#fb923c", "#fbbf24", "#fde047",
        "#a3e635", "#4ade80", "#2dd4bf", "#38bdf8",
        "#60a5fa", "#818cf8", "#c084fc", "#f472b6"
    };

    public enum Axis { HORIZONTAL, VERTICAL }

    /** Result of analyzing how a tile block can slide. */
    public static final class SlideInfo {
        /** Contiguous block of tile indices that would move together. */
        public final List<Integer> blockIndices;
        /** Open squares ahead of the block before the edge/another tile. */
        public final int maxDistance;

        public SlideInfo(List<Integer> blockIndices, int maxDistance) {
            this.blockIndices = blockIndices;
            this.maxDistance = maxDistance;
        }
    }

    /** A legal slide move that produces a match. */
    public static final class Hint {
        public final List<Integer> blockIndices;
        public final int step;
        public final int distance;

        public Hint(List<Integer> blockIndices, int step, int distance) {
            this.blockIndices = blockIndices;
            this.step = step;
            this.distance = distance;
        }

        /** Cells the block would land on. */
        public List<Integer> destinationIndices() {
            List<Integer> dest = new ArrayList<>();
            for (int idx : blockIndices) {
                dest.add(idx + step * distance);
            }
            return dest;
        }
    }

    private Tile[] grid = new Tile[CELL_COUNT];
    private final Random random = new Random();

    /** Current board array (read-only use; the board swaps the reference on commit). */
    public Tile[] getGrid() {
        return grid;
    }

    public int tilesRemaining() {
        int n = 0;
        for (Tile t : grid) {
            if (t != null) {
                n++;
            }
        }
        return n;
    }

    /** Builds a fresh shuffled 24-tile board, guaranteed solvable. */
    public void initGame() {
        grid = new Tile[CELL_COUNT];

        List<Tile> deck = new ArrayList<>();
        for (int i = 0; i < SYMBOLS.length; i++) {
            Color color = Color.decode(COLOR_HEX[i]);
            deck.add(new Tile(SYMBOLS[i], color));
            deck.add(new Tile(SYMBOLS[i], color));
        }
        Collections.shuffle(deck, random);

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < CELL_COUNT; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions, random);

        for (int i = 0; i < deck.size(); i++) {
            grid[positions.get(i)] = deck.get(i);
        }

        ensureSolvable();
    }

    /**
     * For a tile at {@code startIndex}, returns the contiguous block of tiles
     * that would move together and how many open squares lie ahead of it.
     * Read-only: never mutates {@code targetGrid}.
     */
    public SlideInfo getSlideInfo(Tile[] targetGrid, int startIndex, int step, Axis axis) {
        int row = startIndex / GRID_SIZE;

        List<Integer> movingBlockIndices = new ArrayList<>();
        movingBlockIndices.add(startIndex);
        int current = startIndex;

        while (true) {
            current += step;
            if (current < 0 || current >= CELL_COUNT) {
                break;
            }
            if (axis == Axis.HORIZONTAL && current / GRID_SIZE != row) {
                break;
            }
            if (targetGrid[current] != null) {
                movingBlockIndices.add(current);
            } else {
                break;
            }
        }

        int lastBlockIdx = movingBlockIndices.get(movingBlockIndices.size() - 1);
        int maxDistance = 0;
        int cursor = lastBlockIdx + step;

        while (cursor >= 0 && cursor < CELL_COUNT
                && !(axis == Axis.HORIZONTAL && cursor / GRID_SIZE != row)
                && targetGrid[cursor] == null) {
            maxDistance++;
            cursor += step;
        }

        return new SlideInfo(movingBlockIndices, maxDistance);
    }

    /**
     * Slides the block on {@code targetGrid} by {@code distance} cells,
     * moving the block far-end-first so tiles don't overwrite each other.
     * The caller must ensure the distance fits the available open space.
     */
    public static void applySlide(Tile[] targetGrid, List<Integer> blockIndices,
                                  int step, int distance) {
        List<Integer> reverseBlock = new ArrayList<>(blockIndices);
        Collections.reverse(reverseBlock);
        for (int idx : reverseBlock) {
            targetGrid[idx + step * distance] = targetGrid[idx];
            targetGrid[idx] = null;
        }
    }

    /**
     * Slides the block on {@code targetGrid}, capped by open space.
     * Returns false when nothing can move.
     */
    public boolean processPartialSlide(Tile[] targetGrid, int startIndex, int step,
                                       Axis axis, int requestedDistance) {
        SlideInfo info = getSlideInfo(targetGrid, startIndex, step, axis);
        int distance = Math.min(requestedDistance, info.maxDistance);
        if (distance == 0) {
            return false;
        }
        applySlide(targetGrid, info.blockIndices, step, distance);
        return true;
    }

    /**
     * Attempts a slide on the real board. The move is simulated on a copy
     * and committed only if at least one moved tile is part of a
     * line-of-sight match on the resulting board.
     */
    public boolean attemptSlide(int startIndex, int step, Axis axis, int requestedDistance) {
        if (startIndex < 0 || startIndex >= CELL_COUNT || grid[startIndex] == null) {
            return false;
        }
        SlideInfo info = getSlideInfo(grid, startIndex, step, axis);
        int distance = Math.min(requestedDistance, info.maxDistance);
        if (distance == 0) {
            return false;
        }
        Tile[] testGrid = copyGrid(grid);
        applySlide(testGrid, info.blockIndices, step, distance);
        if (movedBlockCreatesMatch(testGrid, info.blockIndices, step, distance)) {
            grid = testGrid;
            return true;
        }
        return false;
    }

    /**
     * Read-only check: does the board contain a matching pair with
     * line-of-sight (same row/column, no other tiles between; gaps allowed)?
     */
    public static boolean hasLineOfSightMatch(Tile[] targetGrid) {
        for (int r = 0; r < GRID_SIZE; r++) {
            int lastTileIdx = -1;
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                if (targetGrid[idx] != null) {
                    if (lastTileIdx != -1
                            && targetGrid[idx].symbol.equals(targetGrid[lastTileIdx].symbol)) {
                        return true;
                    }
                    lastTileIdx = idx;
                }
            }
        }

        for (int c = 0; c < GRID_SIZE; c++) {
            int lastTileIdx = -1;
            for (int r = 0; r < GRID_SIZE; r++) {
                int idx = r * GRID_SIZE + c;
                if (targetGrid[idx] != null) {
                    if (lastTileIdx != -1
                            && targetGrid[idx].symbol.equals(targetGrid[lastTileIdx].symbol)) {
                        return true;
                    }
                    lastTileIdx = idx;
                }
            }
        }

        return false;
    }

    /** Line-of-sight check against the live board. */
    public boolean hasLineOfSightMatch() {
        return hasLineOfSightMatch(grid);
    }

    /**
     * Gathers every line-of-sight pair on the live board into a removal set.
     * Does not mutate the board.
     */
    public Set<Integer> collectMatches() {
        return collectMatches(grid);
    }

    /**
     * Gathers every line-of-sight pair on the given board into a removal
     * set. Does not mutate the board.
     */
    public static Set<Integer> collectMatches(Tile[] targetGrid) {
        Set<Integer> toRemove = new LinkedHashSet<>();

        for (int r = 0; r < GRID_SIZE; r++) {
            int lastTileIdx = -1;
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                if (targetGrid[idx] != null) {
                    if (lastTileIdx != -1
                            && targetGrid[idx].symbol.equals(targetGrid[lastTileIdx].symbol)) {
                        toRemove.add(idx);
                        toRemove.add(lastTileIdx);
                    }
                    lastTileIdx = idx;
                }
            }
        }

        for (int c = 0; c < GRID_SIZE; c++) {
            int lastTileIdx = -1;
            for (int r = 0; r < GRID_SIZE; r++) {
                int idx = r * GRID_SIZE + c;
                if (targetGrid[idx] != null) {
                    if (lastTileIdx != -1
                            && targetGrid[idx].symbol.equals(targetGrid[lastTileIdx].symbol)) {
                        toRemove.add(idx);
                        toRemove.add(lastTileIdx);
                    }
                    lastTileIdx = idx;
                }
            }
        }

        return toRemove;
    }

    /**
     * Strict legality check: after sliding {@code blockIndices} by
     * {@code distance} on {@code testGrid}, is at least one of the moved
     * tiles (at its new position) part of a line-of-sight match?
     * Merely uncovering a match between untouched tiles does not count.
     */
    public static boolean movedBlockCreatesMatch(Tile[] testGrid,
                                                 List<Integer> blockIndices,
                                                 int step, int distance) {
        Set<Integer> matches = collectMatches(testGrid);
        for (int idx : blockIndices) {
            if (matches.contains(idx + step * distance)) {
                return true;
            }
        }
        return false;
    }

    /** Removes the given cells from the live board. */
    public void removeTiles(Set<Integer> indices) {
        for (int idx : indices) {
            grid[idx] = null;
        }
    }

    /**
     * Finds one legal slide (block + direction + distance) that would produce
     * a match — exactly the kind of move a drag is allowed to make.
     * Returns null when no such move exists.
     */
    public Hint findHint() {
        int[][] directions = {
            {1, 0}, {-1, 0},                 // step, axis (0 = horizontal)
            {GRID_SIZE, 1}, {-GRID_SIZE, 1} // step, axis (1 = vertical)
        };

        for (int index = 0; index < CELL_COUNT; index++) {
            if (grid[index] == null) {
                continue;
            }
            for (int[] dir : directions) {
                int step = dir[0];
                Axis axis = dir[1] == 0 ? Axis.HORIZONTAL : Axis.VERTICAL;

                SlideInfo info = getSlideInfo(grid, index, step, axis);
                for (int distance = 1; distance <= info.maxDistance; distance++) {
                    Tile[] testGrid = copyGrid(grid);
                    applySlide(testGrid, info.blockIndices, step, distance);
                    if (movedBlockCreatesMatch(testGrid, info.blockIndices, step, distance)) {
                        return new Hint(info.blockIndices, step, distance);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Randomly redistributes the tiles currently on the board across all 36
     * cells. Values are kept; only positions change (Fisher-Yates).
     */
    public void shuffleTilesInPlace() {
        List<Tile> tileValues = new ArrayList<>();
        for (Tile t : grid) {
            if (t != null) {
                tileValues.add(t);
            }
        }

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < CELL_COUNT; i++) {
            positions.add(i);
        }
        for (int i = positions.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(positions, i, j);
        }

        Tile[] newGrid = new Tile[CELL_COUNT];
        for (int i = 0; i < tileValues.size(); i++) {
            newGrid[positions.get(i)] = tileValues.get(i);
        }
        grid = newGrid;
    }

    /**
     * Guarantees the board is never left with zero legal moves: reshuffles
     * until at least one slide can put a moved tile into a match.
     */
    public void ensureSolvable() {
        ensureSolvable(500);
    }

    public void ensureSolvable(int maxAttempts) {
        int attempts = 0;
        while (findHint() == null && attempts < maxAttempts) {
            shuffleTilesInPlace();
            attempts++;
        }
    }

    private Tile[] copyGrid(Tile[] source) {
        Tile[] copy = new Tile[CELL_COUNT];
        for (int i = 0; i < CELL_COUNT; i++) {
            copy[i] = source[i] == null ? null : source[i].copy();
        }
        return copy;
    }
}
