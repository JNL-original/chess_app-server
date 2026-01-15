package com.jnl.chess_server.models.pieces;

import com.jnl.chess_server.models.BoardData;
import com.jnl.chess_server.models.GameState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Queen extends ChessPiece {

    public Queen(int owner) {
        super(owner, "queen");
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;
        int currentRow = currentIndex / boardSize;

        // --- 1. ДИАГОНАЛЬНЫЕ ХОДЫ (как у Слона) ---
        int[] diagDirections = {boardSize + 1, boardSize - 1, -boardSize + 1, -boardSize - 1};
        for (int direction : diagDirections) {
            for (int index = currentIndex + direction;
                 BoardData.onBoard(index) && Math.abs((index - direction) % boardSize - index % boardSize) == 1;
                 index += direction) {

                if (processMove(index, game, moves)) break;
            }
        }

        // --- 2. ВЕРТИКАЛЬНЫЕ ХОДЫ ---
        // Вниз
        for (int index = currentIndex + boardSize; BoardData.onBoard(index); index += boardSize) {
            if (processMove(index, game, moves)) break;
        }
        // Вверх
        for (int index = currentIndex - boardSize; BoardData.onBoard(index); index -= boardSize) {
            if (processMove(index, game, moves)) break;
        }

        // --- 3. ГОРИЗОНТАЛЬНЫЕ ХОДЫ ---
        // Вправо
        for (int index = currentIndex + 1; BoardData.onBoard(index) && (index / boardSize == currentRow); index++) {
            if (processMove(index, game, moves)) break;
        }
        // Влево
        for (int index = currentIndex - 1; BoardData.onBoard(index) && (index / boardSize == currentRow); index--) {
            if (processMove(index, game, moves)) break;
        }

        return moves;
    }

    /**
     * Вспомогательный метод, чтобы не дублировать логику проверки клетки
     * Возвращает true, если нужно прервать луч (встретили фигуру)
     */
    private boolean processMove(int index, GameState game, List<Integer> moves) {
        ChessPiece target = game.getBoard().get(index);
        if (target == null) {
            moves.add(index);
            return false;
        } else {
            if (game.isEnemies(target.getOwner(), this.getOwner())) {
                moves.add(index);
            }
            return true; // Прерываем луч
        }
    }

    @Override
    public ChessPiece kill() {
        return new Queen(-1);
    }

    @Override
    public ChessPiece copy() {
        return new Queen(this.getOwner());
    }
}