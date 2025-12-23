package com.go.server.game.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.go.server.game.session.model.output.PlayerDto;

import java.util.List;

public class EndGameDto {
    public final double score;
    public final List<PlayerDto> winner;

    @JsonCreator
    public EndGameDto(
            @JsonProperty("score") final double score,
            @JsonProperty("winner") final List<PlayerDto> winner
    ) {
        this.score = score;
        this.winner = winner;
    }
}
