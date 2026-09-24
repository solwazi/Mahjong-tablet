package com.solwazi.slidemahjong;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Paints the 6x6 board, tiles, and hint highlights, and handles
 * mouse press/drag/release as the desktop equivalent of touch swipes.
 */
public class BoardPanel extends JPanel implements MouseListener {

    private static final int PAD = 10;
    private static final int GAP = 6;
    private static final int DRAG_THRESHOLD = 30; // px, mirrors the touch threshold
    private static final int CORNER = 12;

    private static final Color BOARD_BG = new Color(0x0f172a);
    private static final Color TILE_TEXT = new Color(0x0f172a);
    private static final Color AMBER = new Color(0xfbbf24);

    private final Board board;
    private final IntConsumer countListener;

    private int pressX;
    private int pressY;
    private int pressIndex = -1;

    private final Set<Integer> hintSources = new HashSet<>();
    private final Set<Integer> hintTargets = new HashSet<>();
    private Timer hintTimer;

    public BoardPanel(Board board, IntConsumer countListener) {
        this.board = board;
        this.countListener = countListener;
        setOpaque(false);
        addMouseListener(this);
    }

    /** Starts a brand-new board (also used for the very first board). */
    public void newBoard() {
        clearHint();
        board.initGame();
        notifyCount();
        repaint();
    }

    /** Finds a legal move and highlights it for 1.5 seconds. */
    public void showHint() {
        Board.Hint hint = board.findHint();
        if (hint == null) {
            // Safety net: reshuffle and try again, like the original.
            board.shuffleTilesInPlace();
            board.ensureSolvable();
            notifyCount();
            repaint();
            return;
        }

        hintSources.clear();
        hintSources.addAll(hint.blockIndices);
        hintTargets.clear();
        hintTargets.addAll(hint.destinationIndices());
        repaint();

        clearHintTimer();
        hintTimer = new Timer(1500, e -> clearHint());
        hintTimer.setRepeats(false);
        hintTimer.start();
    }

    private void clearHint() {
        clearHintTimer();
        hintSources.clear();
        hintTargets.clear();
        repaint();
    }

    private void clearHintTimer() {
        if (hintTimer != null && hintTimer.isRunning()) {
            hintTimer.stop();
        }
        hintTimer = null;
    }

    private void notifyCount() {
        countListener.accept(board.tilesRemaining());
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    private int cellSize() {
        int side = Math.min(getWidth(), getHeight());
        return (side - 2 * PAD - (Board.GRID_SIZE - 1) * GAP) / Board.GRID_SIZE;
    }

    private int cellPitch() {
        return cellSize() + GAP;
    }

    private int cellX(int col) {
        return PAD + col * cellPitch();
    }

    private int cellY(int row) {
        return PAD + row * cellPitch();
    }

    /** Board cell index under the point, or -1 (gap/outside). */
    private int indexAt(int x, int y) {
        int cell = cellSize();
        int pitch = cellPitch();
        int col = (x - PAD) / pitch;
        int row = (y - PAD) / pitch;
        if (row < 0 || row >= Board.GRID_SIZE || col < 0 || col >= Board.GRID_SIZE) {
            return -1;
        }
        int localX = (x - PAD) - col * pitch;
        int localY = (y - PAD) - row * pitch;
        if (localX < 0 || localY < 0 || localX > cell || localY > cell) {
            return -1;
        }
        return row * Board.GRID_SIZE + col;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int side = Math.min(getWidth(), getHeight());
        g2.setColor(BOARD_BG);
        g2.fill(new RoundRectangle2D.Double(0, 0, side, side, CORNER * 2, CORNER * 2));

        Tile[] grid = board.getGrid();
        int cell = cellSize();
        Font tileFont = new Font("Segoe UI Symbol", Font.BOLD, Math.max(12, (int) (cell * 0.55)));

        for (int idx = 0; idx < Board.CELL_COUNT; idx++) {
            int row = idx / Board.GRID_SIZE;
            int col = idx % Board.GRID_SIZE;
            int x = cellX(col);
            int y = cellY(row);

            Tile tile = grid[idx];
            if (tile == null) {
                paintEmptyCell(g2, x, y, cell, idx);
            } else {
                paintTile(g2, x, y, cell, tile, tileFont, idx);
            }

            if (hintSources.contains(idx)) {
                g2.setColor(AMBER);
                g2.setStroke(new BasicStroke(3));
                g2.draw(new RoundRectangle2D.Double(x + 1.5, y + 1.5,
                        cell - 3, cell - 3, CORNER, CORNER));
            } else if (hintTargets.contains(idx)) {
                g2.setColor(new Color(251, 191, 36, 64));
                g2.fill(new RoundRectangle2D.Double(x, y, cell, cell, CORNER, CORNER));
                g2.setColor(AMBER);
                Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0);
                g2.setStroke(dashed);
                g2.draw(new RoundRectangle2D.Double(x + 1, y + 1,
                        cell - 2, cell - 2, CORNER, CORNER));
            }
        }

        g2.dispose();
    }

    private void paintTile(Graphics2D g2, int x, int y, int cell,
                           Tile tile, Font font, int idx) {
        // Drop shadow, like the CSS box-shadow.
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Double(x, y + 4, cell, cell, CORNER, CORNER));

        g2.setColor(tile.color);
        g2.fill(new RoundRectangle2D.Double(x, y, cell, cell, CORNER, CORNER));

        g2.setFont(font);
        g2.setColor(TILE_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (cell - fm.stringWidth(tile.symbol)) / 2;
        int ty = y + (cell - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(tile.symbol, tx, ty);
    }

    private void paintEmptyCell(Graphics2D g2, int x, int y, int cell, int idx) {
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fill(new RoundRectangle2D.Double(x, y, cell, cell, CORNER, CORNER));
        g2.setColor(new Color(255, 255, 255, 26));
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10, new float[]{5, 4}, 0);
        g2.setStroke(dashed);
        g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5,
                cell - 1, cell - 1, CORNER, CORNER));
    }

    // ------------------------------------------------------------------
    // Mouse: press/drag/release acts like a touch swipe.
    // ------------------------------------------------------------------

    @Override
    public void mousePressed(MouseEvent e) {
        pressIndex = indexAt(e.getX(), e.getY());
        pressX = e.getX();
        pressY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int startIndex = pressIndex;
        pressIndex = -1;
        if (startIndex < 0 || board.getGrid()[startIndex] == null) {
            return;
        }

        int deltaX = e.getX() - pressX;
        int deltaY = e.getY() - pressY;
        if (Math.abs(deltaX) < DRAG_THRESHOLD && Math.abs(deltaY) < DRAG_THRESHOLD) {
            return; // treated as a tap, not a drag
        }

        int step;
        Board.Axis axis;
        int magnitude;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            axis = Board.Axis.HORIZONTAL;
            step = deltaX > 0 ? 1 : -1;
            magnitude = Math.abs(deltaX);
        } else {
            axis = Board.Axis.VERTICAL;
            step = deltaY > 0 ? Board.GRID_SIZE : -Board.GRID_SIZE;
            magnitude = Math.abs(deltaY);
        }

        // Longer drags move the block further, like the touch version.
        int requestedDistance = Math.max(1, Math.round((float) magnitude / cellPitch()));

        boolean committed = board.attemptSlide(startIndex, step, axis, requestedDistance);
        if (committed) {
            notifyCount();
            repaint();
            Timer settle = new Timer(100, ev -> resolveMatches());
            settle.setRepeats(false);
            settle.start();
        }
        // If not committed, the tile simply stays put (no flicker).
    }

    /**
     * Clears matches, then re-checks on a short delay for chain reactions.
     * When the board settles with tiles left but no legal moves, reshuffles.
     */
    private void resolveMatches() {
        Set<Integer> matches = board.collectMatches();
        if (!matches.isEmpty()) {
            board.removeTiles(matches);
            notifyCount();
            repaint();
            Timer again = new Timer(150, ev -> resolveMatches());
            again.setRepeats(false);
            again.start();
            return;
        }

        if (board.tilesRemaining() == 0) {
            JOptionPane.showMessageDialog(this,
                    "🎉 Board Cleared! Nice job!",
                    "Shift Mahjong Daily",
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (!board.hasLineOfSightMatch() && board.findHint() == null) {
            board.ensureSolvable();
            notifyCount();
            repaint();
        }
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(420, 420);
    }
}
