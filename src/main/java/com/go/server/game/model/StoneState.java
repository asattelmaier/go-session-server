package com.go.server.game.model;

import com.go.server.game.model.dto.StateDto;

public enum StoneState {
    Black,
    White,
    Empty;

    public StateDto toDto() {
        switch (this) {
            case Black: return StateDto.Black;
            case White: return StateDto.White;
            case Empty: return StateDto.Empty;
            default: return StateDto.Empty;
        }
    }
}
