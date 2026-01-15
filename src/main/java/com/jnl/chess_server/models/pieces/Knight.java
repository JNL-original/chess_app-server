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
public class Knight extends ChessPiece {

    public Knight(int owner) {
        super(owner, "knight");
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;
        int currentCol = currentIndex % boardSize;

        // 8 направлений прыжка коня
        int[] directions = {
                boardSize * 2 - 1,
                boardSize * 2 + 1,
                -boardSize * 2 - 1,
                -boardSize * 2 + 1,
                boardSize - 2,
                boardSize + 2,
                -boardSize - 2,
                -boardSize + 2
        };

        for (int direction : directions) {
            int index = currentIndex + direction;

            // 1. Проверка: клетка на доске
            if (!BoardData.onBoard(index)) {
                continue;
            }

            // 2. Проверка: не перепрыгнули ли мы через край доски
            // (разница колонок у коня не может быть больше 2)
            if (Math.abs(index % boardSize - currentCol) > 2) {
                continue;
            }

            // 3. Проверка: клетка пуста или там враг
            ChessPiece targetPiece = game.getBoard().get(index);
            if (targetPiece == null || game.isEnemies(targetPiece.getOwner(), getOwner())) {
                moves.add(index);
            }
        }

        return moves;
    }

    @Override
    public ChessPiece kill() {
        return new Knight(-1);
    }

    @Override
    public ChessPiece copy() {
        return new Knight(this.getOwner());
    }
}