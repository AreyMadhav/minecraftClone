package com.minecraftclone.minecraftclone;

import com.minecraftclone.minecraftclone.GamePanel;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create a new JFrame for the game window
            JFrame frame = new JFrame("Minecraft Clone");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600); // Set your desired window size
            frame.setLocationRelativeTo(null); // Center the window on the screen
            frame.setResizable(false); // Prevent window resizing

            // Add the main game panel to the JFrame
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);

            // Make the JFrame visible
            frame.setVisible(true);

            // Start the game loop
            gamePanel.startGame();
        });
    }
}
