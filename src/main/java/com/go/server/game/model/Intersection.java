package com.go.server.game.model;

import com.go.server.game.model.dto.IntersectionDto;

public class Intersection {
    private final Location location;
    private final StoneState state;

    public Intersection(Location location, StoneState state) {
        this.location = location;
        this.state = state;
    }

    public Location getLocation() {
        return location;
    }

    public StoneState getState() {
        return state;
    }

    public IntersectionDto toDto() {
        return new IntersectionDto(location.toDto(), state.toDto());
    }
}
