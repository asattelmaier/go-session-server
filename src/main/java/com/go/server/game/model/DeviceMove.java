package com.go.server.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeviceMove {
    public static final String PASS_ID = "PASS";

    public enum MoveType {
        PLAY,
        PASS
    }

    private int x;
    private int y;
    private MoveType type;

    /**
     * Default constructor for Jackson/Serialization.
     */
    public DeviceMove() {
        this.type = MoveType.PLAY; // Default to PLAY
    }

    private DeviceMove(int x, int y, MoveType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public static DeviceMove at(int x, int y) {
        return new DeviceMove(x, y, MoveType.PLAY);
    }

    public static DeviceMove pass() {
        return new DeviceMove(0, 0, MoveType.PASS);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public MoveType getType() {
        return type;
    }

    public void setType(MoveType type) {
        this.type = type;
    }
    
    public void setPass(boolean isPass) {
        if (isPass) {
            this.type = MoveType.PASS;
        }
    }

    @JsonIgnore
    public boolean isPass() {
        return type == MoveType.PASS;
    }

    @Override
    public String toString() {
        return type == MoveType.PASS ? PASS_ID : "(" + x + "," + y + ")";
    }
}
