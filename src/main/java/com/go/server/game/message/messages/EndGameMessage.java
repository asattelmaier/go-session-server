package com.go.server.game.message.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.go.server.game.model.dto.EndGameDto;

public class EndGameMessage extends Message {
    private final EndGameDto payload;

    public EndGameMessage(String sessionId, EndGameDto payload) {
        super(sessionId, "/endgame");
        this.payload = payload;
    }

    @Override
    public EndGameDto getPayload() {
        return payload;
    }
}
