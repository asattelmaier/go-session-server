package com.go.server.game.engine;

import com.go.server.game.model.dto.GameDto;
import com.go.server.game.session.model.Session;
import com.go.server.game.model.DeviceMove;

import java.util.Optional;

public interface GameEngine {
    GameDto processMove(Session session, DeviceMove move);
    Optional<DeviceMove> generateMove(Session session);
    GameDto getGameState(Session session);
}
