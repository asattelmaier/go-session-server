package com.go.server.game.session.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.go.server.game.session.model.BotDifficulty;

public class CreateSessionDto {

    @JsonProperty("playerId")
    private String playerId;
    
    @JsonProperty("difficulty")
    private BotDifficulty difficulty;

    @JsonProperty("boardSize")
    private Integer boardSize;

    public CreateSessionDto() {
    }
    
    public CreateSessionDto(String playerId, BotDifficulty difficulty, Integer boardSize) {
        this.playerId = playerId;
        this.difficulty = difficulty;
        this.boardSize = boardSize;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public BotDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(Integer boardSize) {
        this.boardSize = boardSize;
    }

    @Override
    public String toString() {
        return "CreateSessionDto{playerId='" + playerId + "', difficulty='" + difficulty + "', boardSize=" + boardSize + "}";
    }
}
