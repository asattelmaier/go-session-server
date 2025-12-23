package com.go.server.game.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.model.*;
import com.go.server.game.session.model.BotDifficulty;
import com.go.server.game.session.model.Player;
import com.go.server.game.session.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.go.server.game.session.exception.InvalidMoveException;

@Service
public class GnuGoGameEngine implements GameEngine {

    private static final String GTP_BOARDSIZE = "boardsize ";
    private static final String GTP_CLEAR_BOARD = "clear_board";
    private static final String GTP_KOMI = "komi ";
    private static final String GTP_PLAY = "play ";
    private static final String GTP_GENMOVE = "genmove ";
    private static final String GTP_LIST_STONES_BLACK = "list_stones black";
    private static final String GTP_LIST_STONES_WHITE = "list_stones white";
    private static final String GTP_LEVEL = "level ";
    private static final String GTP_FINAL_SCORE = "final_score";
    private static final String GTP_RESPONSE_PREFIX = "=";

    private static final String COLOR_BLACK = "black";
    private static final String COLOR_WHITE = "white";
    private static final String MOVE_PASS = "PASS";

    private static final String SCORE_BLACK_PREFIX = "B+";
    private static final String SCORE_WHITE_PREFIX = "W+";

    private static final String KOMI_VALUE = "5.5";

    private static final int LEVEL_EASY = 1;
    private static final int LEVEL_MEDIUM = 10;
    private static final int LEVEL_HARD = 20;
    private static final int DEFAULT_LEVEL = 10;

    private final Logger logger = LoggerFactory.getLogger(GnuGoGameEngine.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gnugo.host:localhost}")
    private String gnuGoHost;

    @Value("${gnugo.port:8001}")
    private int gnuGoPort;

    @Override
    public Game processMove(Session session, DeviceMove move) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);

            // Play the new move
            String color = determineNextColor(session.getMoves());
            String moveCoord = move.getType() == DeviceMove.MoveType.PASS ? MOVE_PASS : toGtpCoord(move.getX(), move.getY(), session.getBoardSize());
            
            String response = sendGtp(writer, reader, GTP_PLAY + color + " " + moveCoord);
            if (!response.startsWith(GTP_RESPONSE_PREFIX)) {
                throw new InvalidMoveException("Illegal move: " + response);
            }

            // Move legitimate, update session
            session.addMove(moveCoord);

            return buildGame(writer, reader, session, color.equalsIgnoreCase(COLOR_BLACK) ? COLOR_WHITE : COLOR_BLACK);

        } catch (InvalidMoveException e) {
            throw e;
        } catch (Exception e) {
            logger.error("GnuGo Game Engine failed", e);
            throw new RuntimeException("Game Engine Error", e);
        }
    }

    @Override
    public Optional<DeviceMove> generateMove(Session session) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);

            String color = determineNextColor(session.getMoves());
            
            // Set Difficulty
            sendGtp(writer, reader, GTP_LEVEL + mapDifficultyToLevel(session.getDifficulty().orElse(null)));

            String response = sendGtp(writer, reader, GTP_GENMOVE + color);
            Optional<DeviceMove> move = parseGnuGoResponse(response, session.getBoardSize());
            
            return move;

        } catch (Exception e) {
            logger.error("GnuGo GenMove failed", e);
            return Optional.empty();
        }
    }

    @Override
    public Game getGameState(Session session) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);
            String nextColor = determineNextColor(session.getMoves());
            return buildGame(writer, reader, session, nextColor);

        } catch (Exception e) {
            logger.error("GnuGo getGameState failed", e);
            throw new RuntimeException("Game Engine Error", e);
        }
    }

    private void setupGame(BufferedWriter writer, BufferedReader reader, Session session) throws IOException {
        int size = session.getBoardSize();
        logger.info("Setting up GnuGo board. size: {}", size);
        sendGtp(writer, reader, GTP_BOARDSIZE + size);
        sendGtp(writer, reader, GTP_CLEAR_BOARD);
        sendGtp(writer, reader, GTP_KOMI + KOMI_VALUE);

        // Replay history
        List<String> moves = session.getMoves();
        logger.info("Replaying {} moves for session {}", moves.size(), session.getId());
        String color = COLOR_BLACK;
        for (String move : moves) {
            String resp = sendGtp(writer, reader, GTP_PLAY + color + " " + move);
            logger.debug("Replay move {}: {} -> {}", color, move, resp);
            color = color.equals(COLOR_BLACK) ? COLOR_WHITE : COLOR_BLACK;
        }
    }

    private Game buildGame(BufferedWriter writer, BufferedReader reader, Session session, String nextColor) throws IOException {
        // Fetch stones
        String blackStones = sendGtp(writer, reader, GTP_LIST_STONES_BLACK);
        String whiteStones = sendGtp(writer, reader, GTP_LIST_STONES_WHITE);

        Set<String> blackSet = parseStoneList(blackStones);
        Set<String> whiteSet = parseStoneList(whiteStones);
        logger.info("Stones found - Black: {}, White: {}", blackSet.size(), whiteSet.size());

        int size = session.getBoardSize();
        List<List<List<Intersection>>> board = new ArrayList<>();
        List<List<Intersection>> singleBoard = new ArrayList<>();

        for (int y = 0; y < size; y++) {
            List<Intersection> row = new ArrayList<>();
            for (int x = 0; x < size; x++) {
                String coord = toGtpCoord(x, y, size);
                StoneState state = StoneState.Empty;
                if (blackSet.contains(coord)) state = StoneState.Black;
                else if (whiteSet.contains(coord)) state = StoneState.White;
                
                row.add(new Intersection(new Location(x, y), state));
            }
            singleBoard.add(row);
        }
        
        // Wrap in single history list
        board.add(singleBoard);
        
        // Check for double pass
        List<String> moves = session.getMoves();
        boolean isGameEnded = false;
        if (moves.size() >= 2) {
            String lastMove = moves.get(moves.size() - 1);
            String secondLastMove = moves.get(moves.size() - 2);
            if (MOVE_PASS.equalsIgnoreCase(lastMove) && MOVE_PASS.equalsIgnoreCase(secondLastMove)) {
                isGameEnded = true;
            }
        }

        String activeColorStr = nextColor;
        String passiveColorStr = nextColor.equalsIgnoreCase(COLOR_BLACK) ? COLOR_WHITE : COLOR_BLACK;

        Player activePlayer = session.getPlayers().stream()
                .filter(p -> p.getColor().name().equalsIgnoreCase(activeColorStr))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Active player not found for color: " + activeColorStr));

        Player passivePlayer = session.getPlayers().stream()
                .filter(p -> p.getColor().name().equalsIgnoreCase(passiveColorStr))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Passive player not found for color: " + passiveColorStr));

        return new Game(
                session.getBoardSize(),
                activePlayer,
                passivePlayer,
                board,
                isGameEnded
        );
    }
    
    private Set<String> parseStoneList(String response) {
        if (!response.startsWith(GTP_RESPONSE_PREFIX)) return Collections.emptySet();
        String list = response.substring(1).trim();
        if (list.isEmpty()) return Collections.emptySet();
        return Arrays.stream(list.split("\\s+"))
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private String determineNextColor(List<String> moves) {
        return (moves.size() % 2 == 0) ? COLOR_BLACK : COLOR_WHITE;
    }

    private String sendGtp(BufferedWriter writer, BufferedReader reader, String command) throws IOException {
        logger.debug("Sending GTP: {}", command);
        writer.write(command + "\n");
        writer.flush();

        StringBuilder sb = new StringBuilder();
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() && !firstLine) break;
            sb.append(line).append("\n");
            firstLine = false;
        }
        return sb.toString().trim();
    }
    
    // ... Helper methods (toGtpCoord, parseGnuGoResponse, mapDifficulty) ...
    // Reuse from previous implementation but make static or copy.
    
    private String toGtpCoord(int x, int y, int height) {
        char initialCol = (char) ('A' + x);
        char col = initialCol >= 'I' ? (char) (initialCol + 1) : initialCol;
        int row = height - y;
        return String.valueOf(col) + row;
    }

    private Optional<DeviceMove> parseGnuGoResponse(String response, int size) {
        if (!response.startsWith(GTP_RESPONSE_PREFIX)) return Optional.empty();
        String moveStr = response.substring(1).trim();
        if (moveStr.equalsIgnoreCase(DeviceMove.PASS_ID)) return Optional.of(DeviceMove.pass());
        
        String upperCoord = moveStr.toUpperCase();
        if (upperCoord.length() < 2) return Optional.empty(); 
        
        char colChar = upperCoord.charAt(0);
        int rowNumber = Integer.parseInt(upperCoord.substring(1));
        int xShift = colChar >= 'J' ? 1 : 0;
        int x = (colChar - 'A') - xShift;
        int y = size - rowNumber;
        return Optional.of(DeviceMove.at(x, y));
    }
    
    @Override
    public EndGame getScore(Session session) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);
            
            String response = sendGtp(writer, reader, GTP_FINAL_SCORE);
            return parseFinalScore(response, session);

        } catch (Exception e) {
            logger.error("GnuGo getScore failed", e);
            throw new RuntimeException("Game Engine Score Error", e);
        }
    }

    private EndGame parseFinalScore(String response, Session session) {
        if (!response.startsWith(GTP_RESPONSE_PREFIX)) {
            throw new RuntimeException("GnuGo failed to calculate score: " + response);
        }

        String scoreStr = response.substring(1).trim(); // e.g. "B+10.5" or "W+5.0"
        logger.info("GnuGo Final Score: {}", scoreStr);

        double score = 0.0;
        String winnerColor = "Draw";

        if (scoreStr.toUpperCase().startsWith(SCORE_BLACK_PREFIX)) {
            winnerColor = "BLACK";
            try {
                score = Double.parseDouble(scoreStr.substring(SCORE_BLACK_PREFIX.length()));
            } catch (NumberFormatException e) {
                // Handle "B+Resign" etc if needed, though final_score usually returns numbers
                logger.warn("Could not parse score: {}", scoreStr);
            }
        } else if (scoreStr.toUpperCase().startsWith(SCORE_WHITE_PREFIX)) {
            winnerColor = "WHITE";
            try {
                score = Double.parseDouble(scoreStr.substring(SCORE_WHITE_PREFIX.length()));
            } catch (NumberFormatException e) {
                 logger.warn("Could not parse score: {}", scoreStr);
            }
        }

        String finalWinnerColor = winnerColor;
        List<Player> winners = session.getPlayers().stream()
                .filter(p -> p.getColor().name().equalsIgnoreCase(finalWinnerColor))
                .toList();

        return new EndGame(score, winners);
    }

    private int mapDifficultyToLevel(BotDifficulty difficulty) {
         return Optional.ofNullable(difficulty)
                .map(d -> switch (d) {
                    case EASY -> LEVEL_EASY;
                    case MEDIUM -> LEVEL_MEDIUM;
                    case HARD -> LEVEL_HARD;
                })
                .orElse(DEFAULT_LEVEL);
    }
}
