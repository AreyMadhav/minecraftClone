package com.minecraftclone.minecraftclone;

public class Player {
    public double x, y;
    public double vx, vy;
    public int width = 24;
    public int height = 48;
    public boolean onGround = false;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
    }
}
