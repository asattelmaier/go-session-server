package com.go.server.game.model.dto;

public class SettingsDto {
    public final int boardSize;

    public SettingsDto(final int boardSize) {
        this.boardSize = boardSize;
    }
    
    public static SettingsDto empty() {
        return new SettingsDto(0);
    }
}
