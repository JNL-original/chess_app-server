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
public class Bishop extends ChessPiece {

    public Bishop(int owner) {
        super(owner, "bishop");
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;

        int[] directions = {boardSize + 1, boardSize - 1, -boardSize + 1, -boardSize - 1};

        for (int direction : directions) {
            int index = currentIndex + direction;

            while (BoardData.onBoard(index)) {
                int prevCol = (index - direction) % boardSize;
                int currentCol = index % boardSize;
                if (Math.abs(prevCol - currentCol) != 1) {
                    break;
                }

                ChessPiece pieceOnTarget = game.getBoard().get(index);

                if (pieceOnTarget == null) {
                    moves.add(index);
                } else {
                    if (game.isEnemies(pieceOnTarget.getOwner(), getOwner())) {
                        moves.add(index);
                    }
                    break;
                }

                index += direction;
            }
        }

        return moves;
    }

    @Override
    public ChessPiece kill() {
        return new Bishop(-1);
    }

    @Override
    public ChessPiece copy() {
        return new Bishop(this.getOwner());
    }
}