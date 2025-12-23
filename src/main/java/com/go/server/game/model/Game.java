package com.go.server.game.model;

import com.go.server.game.model.dto.GameDto;
import com.go.server.game.model.dto.IntersectionDto;
import com.go.server.game.model.dto.SettingsDto;
import com.go.server.game.session.model.Player;
import java.util.List;

public class Game {
    private final int boardSize;
    private final Player activePlayer;
    private final Player passivePlayer;
    private final List<List<List<Intersection>>> positions;
    private final boolean isGameEnded;

    public Game(int boardSize, Player activePlayer, Player passivePlayer, List<List<List<Intersection>>> positions, boolean isGameEnded) {
        this.boardSize = boardSize;
        this.activePlayer = activePlayer;
        this.passivePlayer = passivePlayer;
        this.positions = positions;
        this.isGameEnded = isGameEnded;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public Player getActivePlayer() {
        return activePlayer;
    }

    public Player getPassivePlayer() {
        return passivePlayer;
    }

    public List<List<List<Intersection>>> getPositions() {
        return positions;
    }

    public boolean isGameEnded() {
        return isGameEnded;
    }

    public GameDto toDto() {
        List<List<List<IntersectionDto>>> positionDtos = positions.stream()
            .map(board -> board.stream()
                .map(row -> row.stream()
                    .map(Intersection::toDto)
                    .toList())
                .toList())
            .toList();

        return new GameDto(
                new SettingsDto(boardSize),
                activePlayer.toDto(),
                passivePlayer.toDto(),
                positionDtos,
                isGameEnded
        );
    }
}
