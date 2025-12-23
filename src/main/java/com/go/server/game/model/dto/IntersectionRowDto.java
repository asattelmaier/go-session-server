package com.go.server.game.model.dto;

import java.util.List;

public class IntersectionRowDto {
    public final List<IntersectionDto> cols;

    public IntersectionRowDto(List<IntersectionDto> cols) {
        this.cols = cols;
    }
}
