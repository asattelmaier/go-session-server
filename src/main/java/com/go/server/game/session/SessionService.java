package com.go.server.game.session;

import com.go.server.game.client.GameClientPool;
import com.go.server.game.message.handler.MessageHandler;
import com.go.server.game.message.messages.JoinedMessage;
import com.go.server.game.message.messages.TerminatedMessage;
import com.go.server.game.message.messages.UpdatedMessage;
import com.go.server.game.session.model.Colors;
import com.go.server.game.session.model.Player;
import com.go.server.game.session.model.Session;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionService {
    private final static int REMOVE_UNUSED_SESSIONS_INTERVAL = 120000;
    private final SessionRepository repository;
    private final MessageHandler messageHandler;
    private final GameClientPool gameClientPool;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);

    public SessionService(final SessionRepository repository, final MessageHandler messageHandler, final GameClientPool gameClientPool) {
        this.repository = repository;
        this.messageHandler = messageHandler;
        this.gameClientPool = gameClientPool;
    }

    public SessionDto createSession(final CreateSessionDto createSessionDto) {
        final var playerId = UserService.userIdFromString(createSessionDto.playerId);
        final var openSession = repository.getSessionByPlayerId(playerId);

        if (openSession.isPresent()) {
            return openSession.get().toDto();
        }

        return createNewSession(playerId).toDto();
    }

    public void terminateSession(final String sessionId) {
        final var session = repository.getSession(sessionId);

        session.terminate();
        repository.removeSession(session);

        messageHandler.send(new TerminatedMessage(session.toDto()));
    }

    public void updateSession(final String sessionId, final byte[] message) {
        final var session = repository.getSession(sessionId);

        sendMessage(sessionId, message);
        session.update();

        repository.updateSession(session);
    }

    public SessionDto joinSession(final String playerId, final String sessionId) {
        final var player = new Player(UserService.userIdFromString(playerId), Colors.WHITE);
        final var sessionDto = addPlayer(player, sessionId).toDto();

        messageHandler.send(new JoinedMessage(sessionDto));

        return sessionDto;
    }

    public List<SessionDto> getPendingSessions() {
        return this.repository.getAllSessions()
                .stream()
                .filter(Session::isPending)
                .map(Session::toDto)
                .toList();
    }

    private Session createNewSession(final UUID playerId) {
        final var session = new Session(LocalTime.now());

        session.addPlayer(new Player(playerId, Colors.BLACK));
        repository.addSession(session);

        return session;
    }

    private void sendMessage(final String sessionId, final byte[] message) {
        final var gameClient = gameClientPool.acquire();

        messageHandler.send(new UpdatedMessage(gameClient, sessionId, message));

        gameClientPool.release(gameClient);
    }

    private Session addPlayer(final Player player, final String sessionId) {
        final var session = repository.getSession(sessionId);

        session.addPlayer(player);

        return repository.updateSession(session);
    }

    @Scheduled(fixedDelay = REMOVE_UNUSED_SESSIONS_INTERVAL)
    private void removeUnusedSessions() {
        final var sessions = repository.getAllSessions();
        final var unusedSessions = sessions
                .stream()
                .filter(session -> !session.isInUse())
                .collect(Collectors.toList());

        repository.removeSessions(unusedSessions);

        logger.info("Number of unused Sessions removed: " + unusedSessions.size());
    }
}
