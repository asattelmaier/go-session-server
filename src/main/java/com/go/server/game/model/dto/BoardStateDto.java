package com.go.server.game.model.dto;

import java.util.List;

public class BoardStateDto {
    public final List<IntersectionRowDto> rows;

    public BoardStateDto(List<IntersectionRowDto> rows) {
        this.rows = rows;
    }
}
