package com.go.server.game.model.dto;

import com.go.server.game.session.model.output.PlayerDto;
import java.util.List;
import com.go.server.game.model.dto.BoardStateDto;

public class GameDto {
    public final SettingsDto settings;
    public final PlayerDto activePlayer;
    public final PlayerDto passivePlayer;
    public final List<BoardStateDto> positions;

    public final boolean isGameEnded;

    public GameDto(final SettingsDto settings, final PlayerDto activePlayer, final PlayerDto passivePlayer, final List<BoardStateDto> positions, final boolean isGameEnded) {
        this.settings = settings;
        this.activePlayer = activePlayer;
        this.passivePlayer = passivePlayer;
        this.positions = positions;
        this.isGameEnded = isGameEnded;
    }
}
