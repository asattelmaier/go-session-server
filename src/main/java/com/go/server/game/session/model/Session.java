package com.go.server.game.session.model;

import com.go.server.game.session.model.output.SessionDto;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Session {
    private final static int PLAYERS_PER_SESSION = 2;
    private final static int MINUTES_UNTIL_UNUSED = 2;
    // TODO: Use UUID instead of string
    private final String id = UUID.randomUUID().toString();
    private final List<Player> players = new CopyOnWriteArrayList<>();
    private boolean hasError = false;
    private String errorMessage = "";
    private LocalTime updated;
    private boolean isEmpty = false;

    public Session(final LocalTime updated) {
        this.updated = updated;
    }

    public static Session empty() {
        final Session session = new Session(LocalTime.now());

        session.isEmpty = true;

        return session;
    }

    public static Session notFound(final String sessionId) {
        return Session.error("Session with id \"" + sessionId + "\" not found");
    }

    public static Session invalidPlayerId(final String playerId) {
        return Session.error("Invalid player id \"" + playerId + "\" provided");
    }

    public String getId() {
        return id;
    }

    public boolean isPlayerPlaying(UUID playerId) {
        return this.players.stream().anyMatch(player -> player.isPlayer(playerId));
    }

    public boolean isPresent() {
        return !isEmpty;
    }

    public boolean isPending() {
        return players.size() < PLAYERS_PER_SESSION;
    }

    public void addPlayer(final Player player) {
        players.add(player);
    }

    public boolean has(final String id) {
        return this.id.equals(id);
    }

    public void terminate() {
        players.clear();
        update();
    }

    public void update() {
        updated = LocalTime.now();
    }

    public boolean isInUse() {
        final var duration = Duration.between(updated, LocalTime.now());

        return duration.getSeconds() < TimeUnit.MINUTES.toSeconds(MINUTES_UNTIL_UNUSED);
    }

    public SessionDto toDto() {
        final var playersDto = players
                .stream()
                .map(Player::toDto)
                .collect(Collectors.toList());

        return new SessionDto(id, playersDto, hasError, errorMessage);
    }

    private static Session error(final String errorMessage) {
        final var session = new Session(LocalTime.now());

        session.hasError = true;
        session.errorMessage = errorMessage;

        return session;
    }
}
