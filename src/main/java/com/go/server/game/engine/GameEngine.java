package com.go.server.game.engine;

import com.go.server.game.model.EndGame;
import com.go.server.game.model.Game;
import com.go.server.game.session.model.Session;
import com.go.server.game.model.DeviceMove;

import java.util.Optional;

public interface GameEngine {
    Game processMove(Session session, DeviceMove move);
    Optional<DeviceMove> generateMove(Session session);
    Game getGameState(Session session);
    EndGame getScore(Session session);
}
