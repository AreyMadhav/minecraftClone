package com.minecraftclone.minecraftclone;

public class Block {

    Block(com.minecraftclone.minecraftclone.BlockType blockType, int i, int i0) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    public enum BlockType {
    AIR, GRASS, DIRT, STONE, WOOD, LEAVES
}
    private BlockType type;
    private int x;
    private int y;

    public Block(BlockType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    // Add getters and setters as needed

    public int getType() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
