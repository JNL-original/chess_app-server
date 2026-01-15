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
public class King extends ChessPiece {
    private boolean hasMoved;

    public King(int owner, boolean hasMoved) {
        super(owner, "king");
        this.hasMoved = hasMoved;
    }

    public King(int owner) {
        super(owner, "king");
        this.hasMoved = false;
    }

    // Аналог copyWith для рокировки или обновления состояния
    public King copyWith(Boolean hasMoved) {
        return new King(this.getOwner(), hasMoved != null ? hasMoved : this.hasMoved);
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;

        // Список всех соседних клеток
        int[] checkList = {
                currentIndex + boardSize,
                currentIndex - boardSize,
                currentIndex + 1,
                currentIndex - 1,
                currentIndex + boardSize + 1,
                currentIndex + boardSize - 1,
                currentIndex - boardSize + 1,
                currentIndex - boardSize - 1
        };

        for (int index : checkList) {
            // 1. Проверка на нахождение на доске
            if (!BoardData.onBoard(index)) {
                continue;
            }

            // 2. Проверка: клетка должна быть либо пустой, либо с врагом
            ChessPiece targetPiece = game.getBoard().get(index);
            if (targetPiece != null && !game.isEnemies(targetPiece.getOwner(), getOwner())) {
                continue;
            }

            // 3. Защита от "прыжка" через край доски (проверка колонок)
            int currentCol = currentIndex % boardSize;
            int newCol = index % boardSize;

            if (Math.abs(currentCol - newCol) <= 1) {
                moves.add(index);
            }
        }

        return moves;
    }

    @Override
    public ChessPiece kill() {
        return new King(-1, false);
    }

    @Override
    public ChessPiece copy() {
        King copy = new King(this.getOwner(), this.hasMoved);
        copy.setHasMoved(this.isHasMoved());
        return copy;
    }
}