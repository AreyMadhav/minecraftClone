package com.minecraftclone.minecraftclone;

import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class GamePanel extends JPanel implements Runnable {
    // No large full-world array: columns are generated per-chunk on demand
    private final int blockSize = 32;
    private final int worldHeight = 18; // tiles

    private Thread gameThread;
    private volatile boolean running = false;

    private Player player;

    // Input state
    private boolean left, right, up;

    // Hotbar / selected block
    private BlockType selectedBlock = BlockType.GRASS;
    // Chunking for infinite horizontal world
    private final int CHUNK_WIDTH = 16; // tiles per chunk
    private final Map<Integer, Block[]> columns = new HashMap<>(); // key: tileX -> column array
    private final Set<Integer> generatedChunks = new HashSet<>();
    private final Map<Long, BlockType> modifications = new HashMap<>(); // player changes
    private final long worldSeed = 0x5f3759dfL; // deterministic seed

    // Camera (top-left in pixels)
    private double cameraX = 0;
    private double cameraY = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);

        // Pre-generate a few chunks around spawn
        initPlayer();
        ensureChunkGenerated(0);
        ensureChunkGenerated(1);
        ensureChunkGenerated(-1);
        setupInput();
    }

    // world is generated per-chunk on demand; this method kept for compatibility (not used)
    private void initWorld() {
        // no-op: generation happens lazily via ensureChunkGenerated
    }

    private void initPlayer() {
        int spawnX = 0 * blockSize; // spawn near x=0
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
                requestFocusInWindow();
                double worldMX = cameraX + e.getX();
                double worldMY = cameraY + e.getY();
                int tx = (int)Math.floor(worldMX / blockSize);
                int ty = (int)Math.floor(worldMY / blockSize);
                if (ty < 0 || ty >= worldHeight) return;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    // break block
                    setBlock(tx, ty, BlockType.AIR);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // place block if empty
                    if (getBlock(tx, ty) == BlockType.AIR) {
                        setBlock(tx, ty, selectedBlock);
                    }
                }
            }
        });
    }

    private long keyFor(int tx, int ty) {
        return (((long)tx) << 32) | (ty & 0xffffffffL);
    }

    private BlockType getBlock(int tx, int ty) {
        if (ty < 0 || ty >= worldHeight) return BlockType.AIR;
        long k = keyFor(tx, ty);
        if (modifications.containsKey(k)) return modifications.get(k);

        Block[] col = getColumn(tx);
        if (col == null) return BlockType.AIR;
        return col[ty].getType();
    }

    private void setBlock(int tx, int ty, BlockType type) {
        if (ty < 0 || ty >= worldHeight) return;
        long k = keyFor(tx, ty);
        modifications.put(k, type);
    }

    private int chunkOf(int tx) {
        return Math.floorDiv(tx, CHUNK_WIDTH);
    }

    private void ensureChunkGenerated(int cx) {
        if (generatedChunks.contains(cx)) return;
        generateChunk(cx);
        // also mark generated so we don't repeat
        generatedChunks.add(cx);
    }

    private Block[] getColumn(int tx) {
        if (!columns.containsKey(tx)) {
            int cx = chunkOf(tx);
            // generate neighboring chunks to allow trees to span
            ensureChunkGenerated(cx - 1);
            ensureChunkGenerated(cx);
            ensureChunkGenerated(cx + 1);
        }
        return columns.get(tx);
    }

    private void generateChunk(int cx) {
        int startX = cx * CHUNK_WIDTH;
        int endX = startX + CHUNK_WIDTH - 1;
        // deterministic random for chunk
        Random chunkRand = new Random(worldSeed ^ (cx * 341873128712L));

        // precompute base phases for smooth surface
        double phase1 = (chunkRand.nextDouble() - 0.5) * 2 * Math.PI;
        double phase2 = (chunkRand.nextDouble() - 0.5) * 2 * Math.PI;

        for (int tx = startX; tx <= endX; tx++) {
            Block[] col = new Block[worldHeight];

            // height function using combined sin waves for smooth terrain
            double base = worldHeight / 2.0;
            double val = Math.sin(tx * 0.12 + phase1) * 2.2 + Math.sin(tx * 0.05 + phase2) * 3.5;
            int surf = (int)Math.round(base + val);
            if (surf < 2) surf = 2;
            if (surf > worldHeight - 6) surf = worldHeight - 6;

            for (int y = 0; y < worldHeight; y++) {
                if (y < surf) {
                    col[y] = new Block(BlockType.AIR, tx * blockSize, y * blockSize);
                } else if (y == surf) {
                    col[y] = new Block(BlockType.GRASS, tx * blockSize, y * blockSize);
                } else if (y <= surf + 2) {
                    col[y] = new Block(BlockType.DIRT, tx * blockSize, y * blockSize);
                } else {
                    col[y] = new Block(BlockType.STONE, tx * blockSize, y * blockSize);
                }
            }

            // simple caves
            for (int y = surf + 2; y < worldHeight - 1; y++) {
                Random r = new Random(worldSeed ^ (tx * 9301L) ^ (y * 49297L));
                double depthFactor = (double)(y - surf) / (worldHeight - surf);
                double caveChance = 0.02 + depthFactor * 0.08;
                if (r.nextDouble() < caveChance) {
                    col[y] = new Block(BlockType.AIR, tx * blockSize, y * blockSize);
                }
            }

            columns.put(tx, col);
        }

        // After columns are generated, place trees deterministically; avoid calling getColumn
        // to prevent recursive chunk generation. Only write into already-generated columns.
        for (int tx = startX; tx <= endX; tx++) {
            Random r = new Random(worldSeed ^ (tx * 341873128712L));
            if (r.nextDouble() < 0.08) {
                double base = worldHeight / 2.0;
                double val = Math.sin(tx * 0.12 + ((worldSeed >>> 4) & 0xffff)) * 2.2 + Math.sin(tx * 0.05 + ((worldSeed >>> 8) & 0xffff)) * 3.5;
                int surf = (int)Math.round(base + val);
                if (surf < 4 || surf >= worldHeight) continue;
                int trunk = 3 + r.nextInt(3);
                // place trunk into this chunk's column if present
                Block[] colTx = columns.get(tx);
                for (int t = 1; t <= trunk; t++) {
                    int ty = surf - t;
                    if (ty >= 0 && ty < worldHeight && colTx != null) colTx[ty] = new Block(BlockType.WOOD, tx * blockSize, ty * blockSize);
                }
                int leafTop = surf - trunk - 1;
                for (int lx = tx - 2; lx <= tx + 2; lx++) {
                    Block[] colLx = columns.get(lx); // do not generate neighbor chunks here
                    if (colLx == null) continue;
                    for (int ly = leafTop; ly <= leafTop + 2; ly++) {
                        if (ly >= 0 && ly < worldHeight) {
                            if (colLx[ly].getType() == BlockType.AIR) colLx[ly] = new Block(BlockType.LEAVES, lx * blockSize, ly * blockSize);
                        }
                    }
                }
            }
        }
    }

    public void startGame() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
        requestFocusInWindow();
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

        // update camera to center on player
        int viewW = getWidth() > 0 ? getWidth() : 800;
        int viewH = getHeight() > 0 ? getHeight() : 600;
        cameraX = player.x - viewW / 2.0;
        cameraY = player.y - viewH / 2.0;

        // clamp vertical camera so it doesn't show outside world
        double maxCamY = worldHeight * blockSize - viewH;
        if (cameraY < 0) cameraY = 0;
        if (cameraY > maxCamY) cameraY = maxCamY;
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
                if (ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty) != BlockType.AIR) {
                        player.x = tx * blockSize - player.width;
                        player.vx = 0;
                        break;
                    }
                }
            }
        } else if (player.vx < 0) {
            int tx = leftTile;
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty) != BlockType.AIR) {
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
                if (ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty) != BlockType.AIR) {
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
                if (ty >= 0 && ty < worldHeight) {
                    if (getBlock(tx, ty) != BlockType.AIR) {
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
        // Determine visible tile range based on camera
        int viewW = getWidth() > 0 ? getWidth() : 800;
        int viewH = getHeight() > 0 ? getHeight() : 600;

        int tileStartX = (int)Math.floor(cameraX / blockSize) - 2;
        int tileEndX = (int)Math.ceil((cameraX + viewW) / blockSize) + 2;
        int tileStartY = Math.max(0, (int)Math.floor(cameraY / blockSize) - 1);
        int tileEndY = Math.min(worldHeight - 1, (int)Math.ceil((cameraY + viewH) / blockSize) + 1);

        for (int tx = tileStartX; tx <= tileEndX; tx++) {
            for (int ty = tileStartY; ty <= tileEndY; ty++) {
                BlockType t = getBlock(tx, ty);
                if (t == BlockType.AIR) continue;

                int px = tx * blockSize - (int)cameraX;
                int py = ty * blockSize - (int)cameraY;

                switch (t) {
                    case GRASS -> g.setColor(Color.GREEN);
                    case DIRT -> g.setColor(new Color(139, 69, 19));
                    case STONE -> g.setColor(Color.GRAY);
                    case WOOD -> g.setColor(new Color(160, 82, 45));
                    case LEAVES -> g.setColor(Color.GREEN.darker());
                    default -> g.setColor(Color.MAGENTA);
                }
                g.fillRect(px, py, blockSize, blockSize);
                g.setColor(Color.BLACK);
                g.drawRect(px, py, blockSize, blockSize);
            }
        }

        // Draw player (screen-relative)
        g.setColor(Color.BLUE);
        int playerScreenX = (int)Math.round(player.x - cameraX);
        int playerScreenY = (int)Math.round(player.y - cameraY);
        g.fillRect(playerScreenX, playerScreenY, player.width, player.height);

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
