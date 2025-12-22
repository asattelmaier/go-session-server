package com.go.server.game.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.bot.BotManager;
import com.go.server.game.bot.BotService;
import com.go.server.game.client.GameClientPool;
import com.go.server.game.message.handler.MessageHandler;
import com.go.server.game.message.messages.JoinedMessage;
import com.go.server.game.message.messages.SimpleMessage;
import com.go.server.game.message.messages.TerminatedMessage;
import com.go.server.game.session.model.BotDifficulty;
import com.go.server.game.session.model.Colors;
import com.go.server.game.session.model.Player;
import com.go.server.game.session.model.Session;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.game.session.repository.SessionRepository;
import com.go.server.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {
    private static final int REMOVE_UNUSED_SESSIONS_INTERVAL = 120000;
    
    private static final String TOPIC_UPDATED = "/updated";
    
    private final SessionRepository repository;
    private final MessageHandler messageHandler;
    private final GameClientPool gameClientPool;
    private final BotManager botManager;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);

    public SessionService(final SessionRepository repository, final MessageHandler messageHandler, final GameClientPool gameClientPool, final BotManager botManager) {
        this.repository = repository;
        this.messageHandler = messageHandler;
        this.gameClientPool = gameClientPool;
        this.botManager = botManager;
    }

    public SessionDto createSession(final CreateSessionDto createSessionDto) {
        final var playerId = UserService.userIdFromString(createSessionDto.getPlayerId());
        
        repository.getSessionByPlayerId(playerId).ifPresent(openSession -> {
            logger.info("Found existing session {} for player {}. Terminating it.", openSession.getId(), playerId);
            terminateSession(openSession.getId());
        });

        return createNewSession(playerId, createSessionDto.getDifficulty()).toDto();
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
        final var player = Player.human(UserService.userIdFromString(playerId), Colors.WHITE);
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

    private Session createNewSession(final UUID playerId, final BotDifficulty difficulty) {
        logger.info("Creating new session for player: {} with difficulty: {}", playerId, difficulty);
        final var session = new Session(Instant.now(), difficulty);
        session.addPlayer(Player.human(playerId, Colors.BLACK));

        Optional.ofNullable(difficulty)
                .ifPresentOrElse(
                        d -> {
                            logger.info("Adding BOT player (WHITE) due to difficulty selection.");
                            session.addPlayer(Player.bot(UUID.randomUUID(), Colors.WHITE));
                        },
                        () -> logger.warn("No difficulty provided, waiting for second human player.")
                );

        repository.addSession(session);
        return session;
    }

    private void sendMessage(final String sessionId, final byte[] message) {
        final var gameClient = gameClientPool.acquire();
        try {
            byte[] response = gameClient.send(message);
            messageHandler.send(new SimpleMessage(sessionId, TOPIC_UPDATED, response));
            checkForBotMove(sessionId, response);
        } finally {
            gameClientPool.release(gameClient);
        }
    }

    private void checkForBotMove(String sessionId, byte[] gameState) {
        repository.findSession(sessionId)
                .ifPresentOrElse(
                        session -> botManager.checkForBotMove(session, gameState, this::sendMessage),
                        () -> logger.warn("Session not found during bot check: {}", sessionId)
                );
    }

    private Session addPlayer(final Player player, final String sessionId) {
        final var session = repository.getSession(sessionId);
        session.addPlayer(player);
        return repository.updateSession(session);
    }

    @Scheduled(fixedDelay = REMOVE_UNUSED_SESSIONS_INTERVAL)
    private void removeUnusedSessions() {
        List<Session> unusedSessions = repository.getAllSessions().stream()
                .filter(session -> !session.isInUse())
                .toList();

        if (unusedSessions.isEmpty()) {
            logger.debug("No unused Sessions removed.");
            return;
        }

        repository.removeSessions(unusedSessions);
        logger.info("Number of unused Sessions removed: {}", unusedSessions.size());
    }
}
