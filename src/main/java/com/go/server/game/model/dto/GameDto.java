package com.go.server.game.model.dto;

import java.util.List;

public class GameDto {
    public final SettingsDto settings;
    public final String activePlayer;
    public final String passivePlayer;
    public final List<List<List<IntersectionDto>>> positions;

    public GameDto(final SettingsDto settings, final String activePlayer, final String passivePlayer, final List<List<List<IntersectionDto>>> positions) {
        this.settings = settings;
        this.activePlayer = activePlayer;
        this.passivePlayer = passivePlayer;
        this.positions = positions;
    }
}
