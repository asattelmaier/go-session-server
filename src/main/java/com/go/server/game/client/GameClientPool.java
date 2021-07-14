package com.go.server.game.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GameClientPool {
    // TODO: Set max pool size
    final List<GameClient> availableGameClients = new CopyOnWriteArrayList<>();
    final List<GameClient> gameClientsInUse = new CopyOnWriteArrayList<>();
    final GameClientFactory gameClientFactory;

    public GameClientPool(final GameClientFactory gameClientFactory) {
        this.gameClientFactory = gameClientFactory;
    }

    public GameClient acquire() {
        final var gameClient = availableGameClients.stream().findFirst();
        gameClient.ifPresent(availableGameClients::remove);
        gameClient.ifPresent(gameClientsInUse::add);

        return gameClient.orElseGet(gameClientFactory::createGameClient);
    }

    public void release(final GameClient gameClient) {
        gameClientsInUse.remove(gameClient);
        availableGameClients.add(gameClient);
    }
}
