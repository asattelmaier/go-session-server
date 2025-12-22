package com.go.server.game.model.dto;

public class IntersectionDto {
    public final LocationDto location;
    public final StateDto state;

    public IntersectionDto(final LocationDto location, final StateDto state) {
        this.location = location;
        this.state = state;
    }
}
