package com.go.server.game.bot;

import com.go.server.game.model.DeviceMove;
import com.go.server.game.session.model.BotDifficulty;

public interface BotService {
    DeviceMove getNextMove(byte[] gameState, BotDifficulty difficulty);
}
