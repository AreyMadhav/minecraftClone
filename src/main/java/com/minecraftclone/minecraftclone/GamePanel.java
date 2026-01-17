package com.minecraftclone.minecraftclone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel implements Runnable {
    private Block[][] world;
    private final int blockSize = 32;
    private final int worldWidth = 25; // tiles
    private final int worldHeight = 18; // tiles

    private Thread gameThread;
    private volatile boolean running = false;

    private Player player;

    // Input state
    private boolean left, right, up;

    // Hotbar / selected block
    private BlockType selectedBlock = BlockType.GRASS;

    public GamePanel() {
        setPreferredSize(new Dimension(worldWidth * blockSize, worldHeight * blockSize));
        setFocusable(true);

        initWorld();
        initPlayer();
        setupInput();
    }

    private void initWorld() {
        world = new Block[worldWidth][worldHeight];

        int groundY = worldHeight / 2; // simple ground level
        for (int x = 0; x < worldWidth; x++) {
            for (int y = 0; y < worldHeight; y++) {
                if (y < groundY) {
                    world[x][y] = new Block(BlockType.AIR, x * blockSize, y * blockSize);
                } else if (y == groundY) {
                    world[x][y] = new Block(BlockType.GRASS, x * blockSize, y * blockSize);
                } else if (y <= groundY + 2) {
                    world[x][y] = new Block(BlockType.DIRT, x * blockSize, y * blockSize);
                } else {
                    world[x][y] = new Block(BlockType.STONE, x * blockSize, y * blockSize);
                }
            }
        }
    }

    private void initPlayer() {
        int spawnX = (worldWidth / 2) * blockSize;
        int spawnY = (worldHeight / 2 - 3) * blockSize;
        player = new Player(spawnX, spawnY);
    }

    private void setupInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) left = true;
                if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) right = true;
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_W || k == KeyEvent.VK_UP) up = true;
                if (k == KeyEvent.VK_1) selectedBlock = BlockType.GRASS;
                if (k == KeyEvent.VK_2) selectedBlock = BlockType.DIRT;
                if (k == KeyEvent.VK_3) selectedBlock = BlockType.STONE;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) left = false;
                if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) right = false;
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_W || k == KeyEvent.VK_UP) up = false;
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                int tx = mx / blockSize;
                int ty = my / blockSize;
                if (tx < 0 || tx >= worldWidth || ty < 0 || ty >= worldHeight) return;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    // break block
                    setBlock(tx, ty, BlockType.AIR);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // place block if empty
                    if (getBlock(tx, ty).getType() == BlockType.AIR) {
                        setBlock(tx, ty, selectedBlock);
                    }
                }
            }
        });
    }

    private Block getBlock(int tx, int ty) {
        return world[tx][ty];
    }

    private void setBlock(int tx, int ty, BlockType type) {
        world[tx][ty] = new Block(type, tx * blockSize, ty * blockSize);
    }

    public void startGame() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    public void stopGame() {
        running = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        final double fps = 60.0;
        final double nsPerTick = 1000000000.0 / fps;
        long lastTime = System.nanoTime();
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void update() {
        // simple player physics & movement
        double moveSpeed = 3.0;
        if (left) player.vx = -moveSpeed;
        else if (right) player.vx = moveSpeed;
        else player.vx = 0;

        if (up && player.onGround) {
            player.vy = -10;
            player.onGround = false;
        }

        // gravity
        player.vy += 0.6;
        if (player.vy > 12) player.vy = 12;

        // horizontal movement and collision
        player.x += player.vx;
        handleHorizontalCollisions();

        // vertical movement and collision
        player.y += player.vy;
        handleVerticalCollisions();
    }

    private void handleHorizontalCollisions() {
        int leftTile = (int)Math.floor(player.x / blockSize);
        int rightTile = (int)Math.floor((player.x + player.width - 1) / blockSize);
        int topTile = (int)Math.floor(player.y / blockSize);
        int bottomTile = (int)Math.floor((player.y + player.height - 1) / blockSize);

        if (player.vx > 0) {
            // moving right, check right side
            int tx = rightTile;
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (tx >= 0 && tx < worldWidth && ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty).getType() != BlockType.AIR) {
                        player.x = tx * blockSize - player.width;
                        player.vx = 0;
                        break;
                    }
                }
            }
        } else if (player.vx < 0) {
            int tx = leftTile;
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (tx >= 0 && tx < worldWidth && ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty).getType() != BlockType.AIR) {
                        player.x = (tx + 1) * blockSize;
                        player.vx = 0;
                        break;
                    }
                }
            }
        }
    }

    private void handleVerticalCollisions() {
        int leftTile = (int)Math.floor(player.x / blockSize);
        int rightTile = (int)Math.floor((player.x + player.width - 1) / blockSize);
        int topTile = (int)Math.floor(player.y / blockSize);
        int bottomTile = (int)Math.floor((player.y + player.height - 1) / blockSize);

        player.onGround = false;

        if (player.vy > 0) {
            // falling, check bottom
            int ty = bottomTile;
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (tx >= 0 && tx < worldWidth && ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty).getType() != BlockType.AIR) {
                        player.y = ty * blockSize - player.height;
                        player.vy = 0;
                        player.onGround = true;
                        break;
                    }
                }
            }
        } else if (player.vy < 0) {
            int ty = topTile;
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (tx >= 0 && tx < worldWidth && ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty).getType() != BlockType.AIR) {
                        player.y = (ty + 1) * blockSize;
                        player.vy = 0;
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw blocks
        for (int x = 0; x < worldWidth; x++) {
            for (int y = 0; y < worldHeight; y++) {
                Block b = world[x][y];
                BlockType t = b.getType();
                if (t == BlockType.AIR) continue;

                switch (t) {
                    case GRASS:
                        g.setColor(Color.GREEN);
                        break;
                    case DIRT:
                        g.setColor(new Color(139, 69, 19));
                        break;
                    case STONE:
                        g.setColor(Color.GRAY);
                        break;
                    case WOOD:
                        g.setColor(new Color(160, 82, 45));
                        break;
                    case LEAVES:
                        g.setColor(Color.GREEN.darker());
                        break;
                    default:
                        g.setColor(Color.MAGENTA);
                        break;
                }
                g.fillRect(b.getX(), b.getY(), blockSize, blockSize);
                g.setColor(Color.BLACK);
                g.drawRect(b.getX(), b.getY(), blockSize, blockSize);
            }
        }

        // Draw player
        g.setColor(Color.BLUE);
        g.fillRect((int)player.x, (int)player.y, player.width, player.height);

        // Draw hotbar
        int hotbarY = getHeight() - 48;
        int slotSize = 40;
        for (int i = 0; i < 3; i++) {
            int sx = 10 + i * (slotSize + 6);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(sx, hotbarY, slotSize, slotSize);
            g.setColor(Color.BLACK);
            g.drawRect(sx, hotbarY, slotSize, slotSize);

            BlockType t = (i == 0) ? BlockType.GRASS : (i == 1) ? BlockType.DIRT : BlockType.STONE;
            switch (t) {
                case GRASS -> g.setColor(Color.GREEN);
                case DIRT -> g.setColor(new Color(139, 69, 19));
                case STONE -> g.setColor(Color.GRAY);
                default -> g.setColor(Color.MAGENTA);
            }
            g.fillRect(sx + 6, hotbarY + 6, slotSize - 12, slotSize - 12);

            if (selectedBlock == t) {
                g.setColor(Color.YELLOW);
                g.drawRect(sx - 2, hotbarY - 2, slotSize + 4, slotSize + 4);
            }
        }
    }
}
