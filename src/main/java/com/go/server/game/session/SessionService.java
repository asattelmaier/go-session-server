package com.go.server.game.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.session.exception.InvalidMoveException;

import com.go.server.game.engine.GameEngine;
import com.go.server.game.message.handler.MessageHandler;
import com.go.server.game.message.messages.JoinedMessage;
import com.go.server.game.message.messages.SimpleMessage;
import com.go.server.game.message.messages.TerminatedMessage;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.model.*;
import com.go.server.game.model.dto.*;
import com.go.server.game.session.model.BotDifficulty;
import com.go.server.game.session.model.Colors;
import com.go.server.game.session.model.Player;
import com.go.server.game.session.model.Session;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.game.session.repository.SessionRepository;
import com.go.server.user.model.User;
import com.go.server.user.UserService;
import com.go.server.user.exception.InvalidUserIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private final GameEngine gameEngine;
    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);
    public SessionService(final SessionRepository repository, 
                          final MessageHandler messageHandler, 
                          final GameEngine gameEngine,
                          final UserService userService) {
        this.repository = repository;
        this.messageHandler = messageHandler;
        this.gameEngine = gameEngine;
        this.userService = userService;
    }

    public SessionDto createSession(final User player, final BotDifficulty difficulty, final Integer boardSize) {
        
        repository.getSessionByPlayerId(player.getId()).ifPresent(openSession -> {
            logger.info("Found existing session {} for player {}. Terminating it.", openSession.getId(), player.getId());
            terminateSession(openSession.getId());
        });

        return createNewSession(player.getId(), difficulty, boardSize).toDto();
    }

    public void terminateSession(final String sessionId) {
        final var session = repository.getSession(sessionId);
        session.terminate();
        repository.removeSession(session);
        messageHandler.send(new TerminatedMessage(session.toDto()));
    }

    public void updateSession(final String sessionId, final User player, final DeviceMove move) {
        final var session = repository.getSession(sessionId);
        
        Game game = gameEngine.getGameState(session);
        validateTurn(game, player.getId());
        
        processHumanMove(session, move);
        session.update();
        repository.updateSession(session);
    }
    
    private void validateTurn(Game game, UUID requestorId) {
        UUID activePlayerId = game.getActivePlayer().getId();
        
        if (!activePlayerId.equals(requestorId)) {
             throw new InvalidMoveException("It is not your turn!");
        }
    }

    public void initializeGame(final String sessionId) {
        final var session = repository.getSession(sessionId);
        Game game = gameEngine.getGameState(session);
        broadcastGameState(sessionId, game.toDto());
    }
    
    private void processHumanMove(Session session, DeviceMove move) {
        Game game = gameEngine.processMove(session, move);
        
        GameDto gameDto = game.toDto();
        broadcastGameState(session.getId(), gameDto);
        
        if (game.isGameEnded()) {
            handleGameEnd(session);
        } else {
            checkForBotMove(session, game);
        }
    }

    private void broadcastGameState(String sessionId, GameDto gameDto) {
        messageHandler.send(new SimpleMessage(sessionId, TOPIC_UPDATED, gameDto));
    }
    
    private void handleGameEnd(Session session) {
        try {
            EndGame endGame = gameEngine.getScore(session);
            messageHandler.send(new com.go.server.game.message.messages.EndGameMessage(session.getId(), endGame.toDto()));
        } catch (Exception e) {
            logger.error("Failed to calculate score or send end game message", e);
        }
    }

    private void checkForBotMove(Session session, Game game) {
        String activeColor = game.getActivePlayer().getColor().name();
        
        Optional<Player> botPlayer = session.getPlayers().stream()
                .filter(p -> p.isBot() && p.getColor().name().equalsIgnoreCase(activeColor))
                .findFirst();
                
        botPlayer.ifPresent(bot -> {
            logger.debug("It is Bot's turn ({})", bot.getColor());
            gameEngine.generateMove(session).ifPresent(move -> {
                Game newGame = gameEngine.processMove(session, move);
                GameDto newGameDto = newGame.toDto();
                broadcastGameState(session.getId(), newGameDto);
                
                if (newGame.isGameEnded()) {
                    handleGameEnd(session);
                } else {
                    checkForBotMove(session, newGame);
                }
            });
        });
    }

    public SessionDto joinSession(final User player, final String sessionId) {
        final var gamePlayer = Player.human(player.getId(), Colors.WHITE);
        final var sessionDto = addPlayer(gamePlayer, sessionId).toDto();
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

    private Session createNewSession(final UUID playerId, final BotDifficulty difficulty, final Integer boardSize) {
        logger.info("Creating new session for player: {} with difficulty: {} size: {}", playerId, difficulty, boardSize);
        final var session = new Session(Instant.now(), difficulty, boardSize);
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

    private Session addPlayer(final Player player, final String sessionId) {
        final var session = repository.getSession(sessionId);
        logger.info("Adding player to session {}. Players before: {}", sessionId, session.getPlayers().size());
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
