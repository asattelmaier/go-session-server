package com.go.server.game.model;

public class DeviceMove {
    public static final String PASS_ID = "PASS";

    private int x;
    private int y;
    private boolean isPass;

    /**
     * Default constructor for Jackson/Serialization.
     */
    public DeviceMove() {}

    private DeviceMove(int x, int y, boolean isPass) {
        this.x = x;
        this.y = y;
        this.isPass = isPass;
    }

    public static DeviceMove at(int x, int y) {
        return new DeviceMove(x, y, false);
    }

    public static DeviceMove pass() {
        return new DeviceMove(0, 0, true);
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

    public boolean isPass() {
        return isPass;
    }

    public void setPass(boolean pass) {
        isPass = pass;
    }

    @Override
    public String toString() {
        return isPass ? PASS_ID : "(" + x + "," + y + ")";
    }
}
