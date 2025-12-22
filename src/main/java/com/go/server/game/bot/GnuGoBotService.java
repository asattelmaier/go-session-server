package com.go.server.game.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.session.model.BotDifficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class GnuGoBotService implements BotService {
    private static final String FIELD_ACTIVE_PLAYER = "activePlayer";
    private static final String FIELD_POSITIONS = "positions";
    private static final String FIELD_STATE = "state";
    private static final String STATE_EMPTY = "Empty";

    private static final String GTP_BOARDSIZE = "boardsize ";
    private static final String GTP_CLEAR_BOARD = "clear_board";
    private static final String GTP_KOMI = "komi ";
    private static final String GTP_PLAY = "play ";
    private static final String GTP_LEVEL = "level ";
    private static final String GTP_GENMOVE = "genmove ";

    private static final String KOMI_VALUE = "5.5";
    private static final String DEFAULT_ACTIVE_COLOR = "White";

    private static final int DEFAULT_BOARD_SIZE = 9;
    private static final int DEFAULT_LEVEL = 10;

    private static final int LEVEL_EASY = 1;
    private static final int LEVEL_MEDIUM = 10;
    private static final int LEVEL_HARD = 20;

    private final Logger logger = LoggerFactory.getLogger(GnuGoBotService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gnugo.host:localhost}")
    private String gnuGoHost;

    @Value("${gnugo.port:8001}")
    private int gnuGoPort;

    @Override
    public DeviceMove getNextMove(byte[] gameState, BotDifficulty difficulty) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JsonNode root = objectMapper.readTree(gameState);
            Optional<JsonNode> boardOpt = getBoard(root);

            int height = boardOpt.map(JsonNode::size).orElse(DEFAULT_BOARD_SIZE);
            int width = boardOpt.map(board -> board.get(0).size()).orElse(DEFAULT_BOARD_SIZE);
            String activeColor = root.path(FIELD_ACTIVE_PLAYER).asText(DEFAULT_ACTIVE_COLOR);

            sendGtp(writer, reader, GTP_BOARDSIZE + width);
            sendGtp(writer, reader, GTP_CLEAR_BOARD);
            sendGtp(writer, reader, GTP_KOMI + KOMI_VALUE);

            boardOpt.ifPresent(board -> setupBoard(writer, reader, board, width, height));

            sendGtp(writer, reader, GTP_LEVEL + mapDifficultyToLevel(difficulty));

            String response = sendGtp(writer, reader, GTP_GENMOVE + activeColor);
            logger.debug("GnuGo Sidecar Response: {}", response);

            return parseGnuGoResponse(response, width, height)
                    .orElseThrow(() -> new RuntimeException("Unexpected GnuGo response: " + response));

        } catch (Exception e) {
            logger.error("GnuGo Bot failed", e);
            throw new RuntimeException("GnuGo generation failed", e);
        }
    }

    private void setupBoard(BufferedWriter writer, BufferedReader reader, JsonNode board, int width, int height) {
        IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x -> {
                    String state = board.get(y).get(x).path(FIELD_STATE).asText();
                    if (!STATE_EMPTY.equalsIgnoreCase(state)) {
                        try {
                            String coord = toGtpCoord(x, y, height);
                            sendGtp(writer, reader, GTP_PLAY + state.toLowerCase() + " " + coord);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                })
        );
    }

    Optional<DeviceMove> parseGnuGoResponse(String response, int width, int height) {
        if (!response.startsWith("=")) {
            return Optional.empty();
        }

        String moveStr = response.substring(1).trim();
        logger.debug("GnuGo Parsed Move: {}", moveStr);

        return moveStr.equalsIgnoreCase(DeviceMove.PASS_ID)
                ? Optional.of(DeviceMove.pass())
                : Optional.of(parseGtpCoord(moveStr, width, height));
    }

    /**
     * Sends a GTP command and reads the response.
     * Note: This method uses imperative style to handle socket stream reading
     * safely until the double-newline protocol terminator is reached.
     */
    private String sendGtp(BufferedWriter writer, BufferedReader reader, String command) throws IOException {
        logger.debug("Sending GTP: {}", command);
        writer.write(command + "\n");
        writer.flush();

        StringBuilder sb = new StringBuilder();
        String line;
        boolean firstLine = true;
        // GTP responses end with an empty line.
        // We read until we hit that empty line after the actual response content.
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() && !firstLine) break;
            sb.append(line).append("\n");
            firstLine = false;
        }
        return sb.toString().trim();
    }

    private Optional<JsonNode> getBoard(JsonNode root) {
        JsonNode positions = root.path(FIELD_POSITIONS);
        return (positions.isArray() && positions.size() > 0)
                ? Optional.of(positions.get(0))
                : Optional.empty();
    }

    String toGtpCoord(int x, int y, int height) {
        char initialCol = (char) ('A' + x);
        char col = initialCol >= 'I' ? (char) (initialCol + 1) : initialCol;
        int row = height - y;
        return String.valueOf(col) + row;
    }

    DeviceMove parseGtpCoord(String coord, int width, int height) {
        String upperCoord = coord.toUpperCase();
        if (upperCoord.length() < 2) throw new RuntimeException("Invalid coord: " + upperCoord);

        char colChar = upperCoord.charAt(0);
        int rowNumber = Integer.parseInt(upperCoord.substring(1));

        int xShift = colChar >= 'J' ? 1 : 0;
        int x = (colChar - 'A') - xShift;
        int y = height - rowNumber;

        return DeviceMove.at(x, y);
    }

    int mapDifficultyToLevel(BotDifficulty difficulty) {
        return Optional.ofNullable(difficulty)
                .map(d -> switch (d) {
                    case EASY -> LEVEL_EASY;
                    case MEDIUM -> LEVEL_MEDIUM;
                    case HARD -> LEVEL_HARD;
                })
                .orElse(DEFAULT_LEVEL);
    }
}
