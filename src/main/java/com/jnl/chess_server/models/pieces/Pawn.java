package com.jnl.chess_server.models.pieces;


import com.jnl.chess_server.models.BoardData;
import com.jnl.chess_server.models.GameState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Pawn extends ChessPiece {
    private boolean hasMoved;


    public Pawn(int owner, boolean hasMoved ) {
        super(owner, "pawn"); // Передаем тип в родительский класс
        this.hasMoved = hasMoved;
    }
    public Pawn(int owner) {
        super(owner, "pawn"); // Передаем тип в родительский класс
        this.hasMoved = false;
    }
    public Pawn copyWith(boolean hasMoved){
        return new Pawn(getOwner(), hasMoved);
    }

    @Override
    public List<Integer> getPossibleMoves(int currentIndex, GameState game) {
        List<Integer> moves = new ArrayList<>();
        int boardSize = BoardData.boardSize;

        int direction;
        int[] attackOffsets;
        switch(getOwner()){
            case 0 -> {
                direction = - boardSize;
                attackOffsets = new int[]{direction - 1, direction + 1};
            }
            case 1 -> {
                direction = 1;
                attackOffsets = new int[]{direction - 14, direction + 14};
            }
            case 2 -> {
                direction = boardSize;
                attackOffsets = new int[]{direction - 1, direction + 1};
            }
            case 3 -> {
                direction = -1;
                attackOffsets = new int[]{direction - 14, direction + 14};
            }
            default -> {
                direction = 0;
                attackOffsets = new int[]{0};
            }
        }

        final int oneStepIndex = currentIndex + direction;
        if(BoardData.onBoard(oneStepIndex)
                && game.getBoard().get(oneStepIndex) == null){
            moves.add(oneStepIndex);

            int twoStepIndex = currentIndex + 2 * direction;
            if(!hasMoved && BoardData.onBoard(twoStepIndex)
                    && game.getBoard().get(twoStepIndex) == null){
                moves.add(twoStepIndex);
            }
        }


        for (int offset : attackOffsets) {
            final int attackIndex = currentIndex + offset;

            //Чтобы пешка не прыгала через доску
            final int currentRow = currentIndex / boardSize;
            final int newRow = attackIndex / boardSize;

            if (BoardData.onBoard(attackIndex) //если клетка существует
                    && Math.abs(currentRow - newRow) == 1){
                ChessPiece targetPiece = game.getBoard().get(attackIndex);
                if(targetPiece == null){ //если клетка пустая проверка на enPassant
                    for(Integer playerKey : game.getEnPassant().keySet()){
                        List<Integer> epData = game.getEnPassant().get(playerKey);
                        if (epData.get(0) == attackIndex && game.isEnemies(getOwner(), playerKey)) {
                            moves.add(attackIndex);
                        }
                    }
                }
                else if(game.isEnemies(targetPiece.getOwner(), getOwner())) {//если клетка вражеская
                    moves.add(attackIndex);
                }

            }
        }
        return moves;
    }


    @Override
    public ChessPiece kill() {
        return new Pawn(-1);
    }

    @Override
    public ChessPiece copy() {
        Pawn copy = new Pawn(this.getOwner(), this.hasMoved);
        copy.setHasMoved(this.isHasMoved());
        return copy;
    }

    public int checkEnPassant(int fromIndex, int toIndex){ //возвращает -1 если был обычный ход и индекс enPassed если был enPassed
        int direction;
        switch(getOwner()){
            case 0 ->
                direction = - BoardData.boardSize;
            case 1 ->
                direction = 1;
            case 2 ->
                direction = BoardData.boardSize;
            case 3 ->
                direction = -1;
            default ->
                direction = 0;
        }
        if(fromIndex + 2 * direction == toIndex){
            return fromIndex + direction;
        }
        return -1;
    }

    public boolean isFinished(int index, int promotionCondition){
        if(promotionCondition == 0){
            return switch (getOwner()) {
                case 0 -> List.of(42, 43, 44, 55, 54, 53).contains(index) || index < 14;
                case 1 -> List.of(10, 24, 38, 192, 178, 164).contains(index) || index % 14 == 13;
                case 2 -> List.of(140, 141, 142, 153, 152, 151).contains(index) || index > 182;
                case 3 -> List.of(3, 17, 31, 157, 171, 185).contains(index) || index % 14 == 0;
                default -> false;
            };
        }
        return switch(getOwner()){
            case 0 -> index / BoardData.boardSize <= BoardData.boardSize - promotionCondition;
            case 1 -> index % BoardData.boardSize >= promotionCondition - 1;
            case 2 -> index / BoardData.boardSize >= promotionCondition - 1;
            case 3 -> index % BoardData.boardSize <= BoardData.boardSize - promotionCondition;
            default -> false;
        };
    }

}