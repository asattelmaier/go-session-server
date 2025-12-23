package com.go.server.game.model;

import com.go.server.game.model.dto.EndGameDto;
import com.go.server.game.session.model.Player;
import java.util.List;

public class EndGame {
    private final double score;
    private final List<Player> winners;

    public EndGame(double score, List<Player> winners) {
        this.score = score;
        this.winners = winners;
    }

    public double getScore() {
        return score;
    }

    public List<Player> getWinners() {
        return winners;
    }

    public EndGameDto toDto() {
        return new EndGameDto(
            score,
            winners.stream().map(Player::toDto).toList()
        );
    }
}
