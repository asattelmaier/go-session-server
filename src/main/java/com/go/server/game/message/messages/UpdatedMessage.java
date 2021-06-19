package com.go.server.game.message.messages;

import com.go.server.game.client.GameClient;

public class UpdatedMessage extends Message {
    private final static String DESTINATION = "/updated";
    private final GameClient gameClient;
    private final byte[] message;

    public UpdatedMessage(final GameClient gameClient, final String gameSessionId, final byte[] message) {
        super(gameSessionId, DESTINATION);
        this.gameClient = gameClient;
        this.message = message;
    }

    @Override
    public Object getPayload() {
        return gameClient.send(message);
    }
}
