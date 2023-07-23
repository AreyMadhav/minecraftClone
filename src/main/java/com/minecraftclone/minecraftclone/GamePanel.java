package com.minecraftclone.minecraftclone; // Assuming GamePanel.java is in the same package as Block.java and BlockType.java

import static com.minecraftclone.minecraftclone.BlockType.AIR;
import static com.minecraftclone.minecraftclone.BlockType.DIRT;
import static com.minecraftclone.minecraftclone.BlockType.GRASS;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {
    private Block[][] world;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600)); // Set your desired panel size
        setFocusable(true);
        // Add necessary event listeners here (e.g., for keyboard input)
        // Initialize game objects and resources

        // Create a simple game world with blocks (10x10 grid)
        world = new Block[10][10];
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (y == 0) {
                    world[x][y] = new Block(BlockType.GRASS, x * 32, y * 32); // 32 is the block size
                } else {
                    world[x][y] = new Block(BlockType.DIRT, x * 32, y * 32);
                }
            }
        }
    }

    public void startGame() {
        // Your game initialization code, if any, goes here
    }

    public void stopGame() {
        // Your game shutdown code, if any, goes here
    }

    @Override
    public void run() {
        // Main game loop
        while (true) {
            // Update game state

            // Render the game world
            repaint();

            // Control game frame rate (frame limiting) using sleep or timing mechanisms
            try {
                Thread.sleep(16); // Targeting approximately 60 frames per second (1000ms / 60fps ≈ 16ms)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Render the game world with blocks
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                Block block = world[x][y];
                switch (block.getType()) {
                    case AIR:
                        break;
                    case GRASS:
                        g.setColor(Color.GREEN);
                        g.fillRect(block.getX(), block.getY(), 32, 32);
                        break;
                    case DIRT:
                        g.setColor(Color.BROWN);
                        g.fillRect(block.getX(), block.getY(), 32, 32);
                        break;
                    // Add rendering for other block types as needed
                }
            }
        }
    }
}
