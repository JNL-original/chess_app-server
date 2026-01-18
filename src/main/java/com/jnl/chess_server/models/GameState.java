package com.jnl.chess_server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jnl.chess_server.models.enums.Command;
import com.jnl.chess_server.models.enums.GameStatus;
import com.jnl.chess_server.models.enums.Stalemate;
import com.jnl.chess_server.models.pieces.ChessPiece;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    @Builder.ObtainVia(method = "getBoardCopy")
    private List<ChessPiece> board;
    @Builder.ObtainVia(method = "getKingsCopy")
    private List<Integer> kings;
    @Builder.ObtainVia(method = "getAliveCopy")
    private List<Boolean> alive;
    @Builder.ObtainVia(method = "getEnPassantCopy")
    private Map<Integer, List<Integer>> enPassant;
    private int selectedIndex;
    private List<Integer> availableMoves;
    private int currentPlayer;
    private Command commands;
    private GameStatus status;
    private int promotionPawn;
    private Integer myPlayerIndex; // Integer может быть null
    private int turn;
    private OnlineConfig config;

    // Инициализация начального состояния (аналог GameState.initial)
    public static GameState initial(OnlineConfig config) {
        List<ChessPiece> board = BoardData.initializePieces();

        // Рандомный выбор команд (упрощенно)
        Command commands = new Random().nextBoolean() ?
                Command.OPPOSITE_SIDES : Command.ADJACENT_SIDES;

        return GameState.builder()
                .board(board)
                .selectedIndex(-1)
                .availableMoves(new ArrayList<>())
                .currentPlayer(0)
                .kings(initializeKings(board))
                .alive(List.of(true, true, true, true))
                .enPassant(createEmptyEnPassant())
                .commands(commands)
                .status(GameStatus.LOBBY)
                .promotionPawn(-1)
                .myPlayerIndex(-1)
                .turn(0)
                .config(config)
                .build();
    }

    private static Map<Integer, List<Integer>> createEmptyEnPassant() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            map.put(i, List.of(-1, -1));
        }
        return map;
    }

    private static List<Integer> initializeKings(List<ChessPiece> board) {
        List<Integer> kings = new ArrayList<>(List.of(-1, -1, -1, -1));
        for (int i = 0; i < board.size(); i++) {
            ChessPiece piece = board.get(i);
            if (piece != null && "king".equals(piece.getType()) && piece.getOwner() >= 0) {
                kings.set(piece.getOwner(), i);
            }
        }
        return kings;
    }

    // Логика врагов
    public boolean isEnemies(int firstPlayer, int secondPlayer) {
        if (firstPlayer == secondPlayer || firstPlayer < 0 || secondPlayer < 0) return false;
        return switch (commands) {
            case NONE -> true;
            case OPPOSITE_SIDES -> firstPlayer % 2 != secondPlayer % 2;
            case ADJACENT_SIDES -> firstPlayer % 2 == secondPlayer % 2;
            default -> false;
        };
    }

    public Stalemate ifStalemate() {
        // Подсчет живых (аналог alive.fold в Dart)
        long aliveCount = alive.stream().filter(Boolean::booleanValue).count();

        if (aliveCount == 2) {
            return config.getOneOnOneStalemate();
        }

        if (commands == Command.NONE) {
            return config.getAloneAmongAloneStalemate();
        }

        // Проверка наличия живых союзников
        for (int i = 1; i <= 3; i++) {
            int target = (currentPlayer + i) % 4;
            if (alive.get(target) && !isEnemies(currentPlayer, target)) {
                return config.getCommandOnCommandStalemate();
            }
        }

        return config.getCommandOnOneStalemate();
    }
    @JsonIgnore
    public List<ChessPiece> getBoardCopy() {
        if (this.board == null) return null;
        List<ChessPiece> copy = new ArrayList<>(this.board.size());
        for (ChessPiece piece : this.board) {
            // Если клетка пустая, кладем null, если нет — вызываем метод копирования
            copy.add(piece == null ? null : piece.copy());
        }
        return copy;
    }
    @JsonIgnore
    public List<Integer> getKingsCopy() {
        return this.kings == null ? null : new ArrayList<>(this.kings);
    }
    @JsonIgnore
    public List<Boolean> getAliveCopy() {
        return this.alive == null ? null : new ArrayList<>(this.alive);
    }
    @JsonIgnore
    public Map<Integer, List<Integer>> getEnPassantCopy() {
        if (this.enPassant == null) return null;
        Map<Integer, List<Integer>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : this.enPassant.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}
