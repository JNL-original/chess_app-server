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
public class Rook extends ChessPiece {
    private boolean hasMoved;

    public Rook(int owner, boolean hasMoved) {
        super(owner, "rook");
        this.hasMoved = hasMoved;
    }

    public Rook(int owner) {
        super(owner, "rook");
        this.hasMoved = false;
    }

    public Rook copyWith(Boolean hasMoved) {
        return new Rook(this.getOwner(), hasMoved != null ? hasMoved : this.hasMoved);
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;
        int currentRow = currentIndex / boardSize;

        // Вниз
        for (int index = currentIndex + boardSize; BoardData.onBoard(index); index += boardSize) {
            if (processMove(index, game, moves)) break;
        }
        // Вверх
        for (int index = currentIndex - boardSize; BoardData.onBoard(index); index -= boardSize) {
            if (processMove(index, game, moves)) break;
        }
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

    private boolean processMove(int index, GameState game, List<Integer> moves) {
        ChessPiece target = game.getBoard().get(index);
        if (target == null) {
            moves.add(index);
            return false;
        } else {
            if (game.isEnemies(target.getOwner(), this.getOwner())) {
                moves.add(index);
            }
            return true;
        }
    }

    @Override
    public ChessPiece kill() {
        return new Rook(-1, false);
    }

    @Override
    public ChessPiece copy() {
        Rook copy = new Rook(this.getOwner(), this.hasMoved);
        copy.setHasMoved(this.isHasMoved());
        return copy;
    }
}