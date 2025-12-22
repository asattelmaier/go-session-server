package com.go.server.game.session.model;

import java.util.Arrays;

public enum GameCommandType {
    CREATE("Create"),
    PLAY("Play"),
    PASS("Pass"),
    UNKNOWN("Unknown");

    private final String commandName;

    GameCommandType(String commandName) {
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }

    public static GameCommandType fromString(String text) {
        return Arrays.stream(GameCommandType.values())
                .filter(b -> b.commandName.equalsIgnoreCase(text))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
