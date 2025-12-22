package com.go.server.game.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.model.dto.*;
import com.go.server.game.session.model.BotDifficulty;
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
    public GameDto processMove(Session session, DeviceMove move) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);

            // Play the new move
            String color = determineNextColor(session.getMoves());
            String moveCoord = move.getType() == DeviceMove.MoveType.PASS ? "PASS" : toGtpCoord(move.getX(), move.getY(), session.getBoardSize());
            
            String response = sendGtp(writer, reader, GTP_PLAY + color + " " + moveCoord);
            if (!response.startsWith("=")) {
                throw new RuntimeException("Illegal move: " + response);
            }

            // Move legitimate, update session
            session.addMove(moveCoord); // Store as GTP for simplicity, or DeviceMove json? keeping simple

            return buildGameDto(writer, reader, session, color.equalsIgnoreCase("black") ? "white" : "black");

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
            
            // Do not add to session history here. caller will invoke processMove.
            
            return move;

        } catch (Exception e) {
            logger.error("GnuGo GenMove failed", e);
            return Optional.empty();
        }
    }

    @Override
    public GameDto getGameState(Session session) {
        try (Socket socket = new Socket(gnuGoHost, gnuGoPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            setupGame(writer, reader, session);
            String nextColor = determineNextColor(session.getMoves());
            return buildGameDto(writer, reader, session, nextColor);

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
        String color = "black";
        for (String move : moves) {
            String resp = sendGtp(writer, reader, GTP_PLAY + color + " " + move);
            logger.debug("Replay move {}: {} -> {}", color, move, resp);
            color = color.equals("black") ? "white" : "black";
        }
    }

    private GameDto buildGameDto(BufferedWriter writer, BufferedReader reader, Session session, String nextColor) throws IOException {
        // Fetch stones
        String blackStones = sendGtp(writer, reader, GTP_LIST_STONES_BLACK);
        String whiteStones = sendGtp(writer, reader, GTP_LIST_STONES_WHITE);

        Set<String> blackSet = parseStoneList(blackStones);
        Set<String> whiteSet = parseStoneList(whiteStones);
        logger.info("Stones found - Black: {}, White: {}", blackSet.size(), whiteSet.size());

        int size = session.getBoardSize();
        List<List<IntersectionDto>> board = new ArrayList<>();

        for (int y = 0; y < size; y++) {
            List<IntersectionDto> row = new ArrayList<>();
            for (int x = 0; x < size; x++) {
                String coord = toGtpCoord(x, y, size);
                StateDto state = StateDto.Empty;
                if (blackSet.contains(coord)) state = StateDto.Black;
                else if (whiteSet.contains(coord)) state = StateDto.White;
                
                row.add(new IntersectionDto(new LocationDto(x, y), state));
            }
            board.add(row);
        }
        
        // Wrap in single history list
        List<List<List<IntersectionDto>>> positions = Collections.singletonList(board);
        
        return new GameDto(
                new SettingsDto(session.getBoardSize(), true), // Suicidallowed default true for GnuGo Chinese rules?
                nextColor.substring(0, 1).toUpperCase() + nextColor.substring(1).toLowerCase(), // Capitalized "Black"/"White"
                nextColor.equalsIgnoreCase("black") ? "White" : "Black", // Passive
                positions
        );
    }
    
    private Set<String> parseStoneList(String response) {
        if (!response.startsWith("=")) return Collections.emptySet();
        String list = response.substring(1).trim();
        if (list.isEmpty()) return Collections.emptySet();
        return Arrays.stream(list.split("\\s+"))
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private String determineNextColor(List<String> moves) {
        return (moves.size() % 2 == 0) ? "black" : "white";
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
        if (!response.startsWith("=")) return Optional.empty();
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
