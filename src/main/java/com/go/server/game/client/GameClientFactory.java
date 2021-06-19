package com.go.server.game.client;

import com.go.server.configuration.SessionConfig;
import org.springframework.stereotype.Component;

@Component
public class GameClientFactory {
    final SessionConfig sessionConfig;

    GameClientFactory(final SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public GameClient createGameClient() {
        return GameClient.connect(sessionConfig);
    }
}
