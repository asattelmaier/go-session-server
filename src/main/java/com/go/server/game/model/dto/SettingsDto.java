package com.go.server.game.model.dto;

public class SettingsDto {
    public final int boardSize;
    public final boolean isSuicideAllowed;

    public SettingsDto(final int boardSize, final boolean isSuicideAllowed) {
        this.boardSize = boardSize;
        this.isSuicideAllowed = isSuicideAllowed;
    }
    
    public static SettingsDto empty() {
        return new SettingsDto(0, false);
    }
}
