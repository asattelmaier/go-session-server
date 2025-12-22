package com.go.server.game.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.session.model.BotDifficulty;
import com.go.server.game.session.model.Player;
import com.go.server.game.session.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiConsumer;

@Component
public class BotManager {
    private static final String FIELD_ACTIVE_PLAYER = "activePlayer";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_COMMAND = "command";
    private static final String FIELD_GAME = "game";
    private static final String FIELD_LOCATION = "location";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_X = "x";
    private static final String FIELD_Y = "y";

    private static final String COMMAND_PASS = "Pass";
    private static final String COMMAND_PLAY = "Play";

    private static final BotDifficulty DEFAULT_DIFFICULTY = BotDifficulty.MEDIUM;

    private final Logger logger = LoggerFactory.getLogger(BotManager.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BotService botService;

    private record BotMoveContext(Session session, JsonNode root, BiConsumer<String, byte[]> messageSender) {
        public String getSessionId() {
            return session.getId();
        }
    }

    public BotManager(BotService botService) {
        this.botService = botService;
    }

    public void checkForBotMove(Session session, byte[] gameState, BiConsumer<String, byte[]> messageSender) {
        logger.debug("Checking for bot move for session: {}", session.getId());

        parseGameState(gameState)
                .map(root -> new BotMoveContext(session, root, messageSender))
                .ifPresent(this::processBotTurn);
    }

    private void processBotTurn(BotMoveContext context) {
        getActivePlayerColor(context.root())
                .flatMap(color -> findBotPlayer(context.session(), color))
                .flatMap(botPlayer -> generateBotMove(context, botPlayer))
                .ifPresent(playDto -> sendMove(context, playDto));
    }

    private Optional<JsonNode> parseGameState(byte[] gameState) {
        try {
            return Optional.of(objectMapper.readTree(gameState));
        } catch (Exception e) {
            logger.error("Failed to parse game state JSON", e);
            return Optional.empty();
        }
    }

    private Optional<String> getActivePlayerColor(JsonNode root) {
        return Optional.ofNullable(root.get(FIELD_ACTIVE_PLAYER))
                .map(node -> node.isTextual() ? node.asText() : node.path(FIELD_COLOR).asText())
                .filter(color -> !color.isEmpty());
    }

    private Optional<Player> findBotPlayer(Session session, String color) {
        return session.getPlayers().stream()
                .filter(p -> p.getColor().name().equalsIgnoreCase(color) && p.isBot())
                .findFirst();
    }

    private Optional<ObjectNode> generateBotMove(BotMoveContext context, Player botPlayer) {
        logger.debug("It is Bot's turn! Bot Player: {}", botPlayer.getId());
        BotDifficulty difficulty = Optional.ofNullable(context.session().getDifficulty()).orElse(DEFAULT_DIFFICULTY);

        try {
            byte[] stateBytes = objectMapper.writeValueAsBytes(context.root());
            DeviceMove botMove = botService.getNextMove(stateBytes, difficulty);
            logger.debug("Bot generated move: {}", botMove);
            return Optional.ofNullable(constructPlayDto(context.root(), botMove));
        } catch (Exception e) {
            logger.error("Bot move generation failed", e);
            return Optional.empty();
        }
    }

    private void sendMove(BotMoveContext context, ObjectNode playDto) {
        try {
            byte[] moveMsg = objectMapper.writeValueAsBytes(playDto);
            context.messageSender().accept(context.getSessionId(), moveMsg);
        } catch (Exception e) {
            logger.error("Failed to serialize or send bot move", e);
        }
    }

    private ObjectNode constructPlayDto(JsonNode root, DeviceMove botMove) {
        ObjectNode commandNode = objectMapper.createObjectNode();
        if (botMove.isPass()) {
            commandNode.put(FIELD_NAME, COMMAND_PASS);
        } else {
            commandNode.put(FIELD_NAME, COMMAND_PLAY);
            commandNode.set(FIELD_LOCATION, objectMapper.createObjectNode()
                    .put(FIELD_X, botMove.getX())
                    .put(FIELD_Y, botMove.getY()));
        }

        ObjectNode playDtoNode = objectMapper.createObjectNode();
        playDtoNode.set(FIELD_COMMAND, commandNode);
        playDtoNode.set(FIELD_GAME, root);
        return playDtoNode;
    }
}
