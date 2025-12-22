package com.go.server.game.session.model;

import com.go.server.game.session.model.output.SessionDto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Session {
    private final static int PLAYERS_PER_SESSION = 2;
    private final static int MINUTES_UNTIL_UNUSED = 2;
    // TODO: Use UUID instead of string
    private final String id;
    private final List<Player> players = new CopyOnWriteArrayList<>();
    private boolean hasError = false;
    private String errorMessage = "";
    private Instant updated;
    private boolean isEmpty = false;
    private BotDifficulty difficulty;

    public Session(final Instant updated) {
        this.updated = updated;
        this.id = UUID.randomUUID().toString();
    }
    
    public Session(final Instant updated, final BotDifficulty difficulty) {
        this.updated = updated;
        this.id = UUID.randomUUID().toString();
        this.difficulty = difficulty;
    }

    public BotDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Session(final String id, final Instant updated, final List<Player> players) {
        this.id = id;
        this.updated = updated;
        this.players.addAll(players);
    }

    public static Session empty() {
        final Session session = new Session(Instant.now());

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

    public List<Player> getPlayers() {
        return players;
    }

    public Instant getUpdated() {
        return updated;
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
        updated = Instant.now();
    }

    public boolean isInUse() {
        final var duration = Duration.between(updated, Instant.now());

        return duration.getSeconds() < TimeUnit.MINUTES.toSeconds(MINUTES_UNTIL_UNUSED);
    }

    public SessionDto toDto() {
        final var playersDto = players
                .stream()
                .map(Player::toDto)
                .collect(Collectors.toList());

        return new SessionDto(id, playersDto, difficulty != null ? difficulty.name() : null, hasError, errorMessage);
    }

    private static Session error(final String errorMessage) {
        final var session = new Session(Instant.now());

        session.hasError = true;
        session.errorMessage = errorMessage;

        return session;
    }
}
