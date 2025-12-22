package com.go.server.game.session.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.go.server.game.session.model.BotDifficulty;

public class CreateSessionDto {

    @JsonProperty("playerId")
    private String playerId;
    
    @JsonProperty("difficulty")
    private BotDifficulty difficulty;

    public CreateSessionDto() {
    }
    
    public CreateSessionDto(String playerId, BotDifficulty difficulty) {
        this.playerId = playerId;
        this.difficulty = difficulty;
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

    @Override
    public String toString() {
        return "CreateSessionDto{playerId='" + playerId + "', difficulty='" + difficulty + "'}";
    }
}
