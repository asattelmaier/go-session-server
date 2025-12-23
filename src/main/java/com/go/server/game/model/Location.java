package com.go.server.game.model;

import com.go.server.game.model.dto.LocationDto;

public class Location {
    private final int x;
    private final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public LocationDto toDto() {
        return new LocationDto(x, y);
    }
}
