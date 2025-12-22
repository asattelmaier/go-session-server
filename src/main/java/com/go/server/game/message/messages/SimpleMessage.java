package com.go.server.game.message.messages;

public class SimpleMessage extends Message {
    private final Object payload;

    public SimpleMessage(final String destination, final Object payload) {
        super("", destination);
        this.payload = payload;
    }
    
    public SimpleMessage(final String sessionId, final String destination, final Object payload) {
        super(sessionId, destination);
        this.payload = payload;
    }

    @Override
    public Object getPayload() {
        return payload;
    }
}
