package com.minecraftclone.minecraftclone;

public class Block {
    private BlockType type;
    private int x;
    private int y;

    public Block(BlockType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public BlockType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setType(BlockType type) {
        this.type = type;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
