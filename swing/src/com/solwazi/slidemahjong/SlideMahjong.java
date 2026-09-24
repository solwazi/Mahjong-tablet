package com.solwazi.slidemahjong;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

/**
 * Desktop (Swing) entry point for Shift Mahjong Daily.
 */
public class SlideMahjong {

    private static final Color APP_BG = new Color(0x1e293b);
    private static final Color BUTTON_BG = new Color(0x3b82f6);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SlideMahjong::createAndShowGui);
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("Shift Mahjong Daily");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(APP_BG);
        frame.setLayout(new BorderLayout(0, 10));

        JLabel titleLabel = new JLabel("Shift Mahjong Daily", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));

        JLabel countLabel = new JLabel("Tiles Remaining: 24", SwingConstants.CENTER);
        countLabel.setForeground(new Color(0xcbd5e1));
        countLabel.setFont(countLabel.getFont().deriveFont(14f));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(countLabel, BorderLayout.SOUTH);

        Board board = new Board();
        BoardPanel boardPanel = new BoardPanel(board,
                n -> countLabel.setText("Tiles Remaining: " + n));

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(boardPanel);

        JButton newButton = new JButton("New Board");
        JButton hintButton = new JButton("Hint");
        styleButton(newButton);
        styleButton(hintButton);
        newButton.addActionListener(e -> boardPanel.newBoard());
        hintButton.addActionListener(e -> boardPanel.showHint());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        footer.add(newButton);
        footer.add(hintButton);

        frame.add(header, BorderLayout.NORTH);
        frame.add(centerWrap, BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);

        frame.pack();
        frame.setMinimumSize(new Dimension(440, 600));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        boardPanel.newBoard();
    }

    private static void styleButton(JButton button) {
        button.setBackground(BUTTON_BG);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
    }
}
