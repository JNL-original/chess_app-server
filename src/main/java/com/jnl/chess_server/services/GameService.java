package com.jnl.chess_server.services;

import com.jnl.chess_server.models.*;
import com.jnl.chess_server.models.enums.Command;
import com.jnl.chess_server.models.enums.GameStatus;
import com.jnl.chess_server.models.enums.Promotion;
import com.jnl.chess_server.models.enums.Stalemate;
import com.jnl.chess_server.models.pieces.*;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.jnl.chess_server.models.enums.Stalemate.SKIP_MOVE;

@Service
public class GameService {
    //TODO Возвращаем null как ошибку
    public GameState handleMove(GameState currentState, int fromIndex, int toIndex){
        GameState checkValid = makeMove(currentState, fromIndex, toIndex);
        if(checkValid == null) return null;
        return nextPlayer(currentState);
    }
    //TODO Меняем состояние, возвращаем ссылку на полученное состояние через метод nextPlayer. Возвращаем null как ошибку
    public GameState continueGameAfterPromotion(ChessPiece piece, GameState state){
        ArrayList<ChessPiece> board = new ArrayList<>(state.getBoard());
        if(state.getPromotionPawn() == -1) return null;
        if(piece.getType().equals("pawn"))
            board.set(state.getPromotionPawn(), new Pawn(state.getCurrentPlayer(), true));
        else if(piece.getType().equals("rook"))
            board.set(state.getPromotionPawn(), new Rook(state.getCurrentPlayer(), true));
        else board.set(state.getPromotionPawn(), piece);
        //TODO Меняем состояние
        state.setBoard(board);
        state.setStatus(GameStatus.ACTIVE);
        state.setPromotionPawn(-1);
        return nextPlayer(state);
    }

    //TODO Меняем состояние, возвращаем не ссылку на предыдущий, а полноценное новое состояние. Возвращаем null как ошибку
    private GameState makeMove(GameState currentState, int fromIndex, int toIndex) {
        List<ChessPiece> board = new ArrayList<>(currentState.getBoard());
        ChessPiece piece = board.get(fromIndex);
        if (piece == null) return null;
        //TODO Здесь должна быть проверка на валидность
        if(!getTruePossibleMoves(fromIndex, currentState).contains(toIndex)) return null;

        if (board.get(toIndex) != null &&
                board.get(toIndex).getOwner() == currentState.getCurrentPlayer() &&
                "rook".equals(board.get(toIndex).getType())) {
            return makeCastling(currentState, toIndex);
        }

        if (board.get(toIndex) != null && "king".equals(board.get(toIndex).getType())) {
            int killPlayer = board.get(toIndex).getOwner();
            for (int i = 0; i < board.size(); i++) {
                if (board.get(i) != null && board.get(i).getOwner() == killPlayer) {
                    board.set(i, board.get(i).kill());
                }
            }
            // Обновляем состояние через copyWith (в Java это builder)
            ArrayList<Boolean> alive = new ArrayList<>(currentState.getAlive());
            alive.set(killPlayer, false);

            currentState.setBoard(board);
            currentState.setAlive(alive);
            //TODO Обновили состояние
        }

        board.set(fromIndex, null);

        // 3. Логика по типам фигур
        switch (piece.getType()) {
            case "pawn" -> {
                Pawn pawn = (Pawn) piece;
                board.set(toIndex, pawn.copyWith(true));

                Map<Integer, List<Integer>> newEnPassant = new HashMap<>(currentState.getEnPassant());
                int mayEnPassant = pawn.checkEnPassant(fromIndex, toIndex);
                newEnPassant.put(currentState.getCurrentPlayer(), List.of(mayEnPassant, (mayEnPassant == -1) ? -1 : toIndex));

                // Проверка взятия на проходе
                for (Integer key : currentState.getEnPassant().keySet()) {
                    List<Integer> epData = currentState.getEnPassant().get(key);
                    if (epData.get(0) == toIndex && board.get(epData.get(1)).getOwner() == key) {
                        board.set(epData.get(1), null);
                        newEnPassant.put(key, List.of(-1, -1));
                    }
                }

                // Превращение пешки
                if (currentState.getConfig().getPawnPromotion() != Promotion.NONE &&
                        pawn.isFinished(toIndex, currentState.getConfig().getPromotionCondition())) {

                    switch (currentState.getConfig().getPawnPromotion()) {
                        case QUEEN -> board.set(toIndex, new Queen(currentState.getCurrentPlayer()));
                        case CHOICE -> {
                            board.set(toIndex, new Pawn(currentState.getCurrentPlayer(), true));
                            //TODO Меняем состояние
                            return currentState.toBuilder()
                                    .board(board)
                                    .enPassant(newEnPassant)
                                    .status(GameStatus.WAITING_FOR_PROMOTION)
                                    .promotionPawn(toIndex).build();
                        }
                        case RANDOM -> board.set(toIndex, getRandomPiece(currentState.getCurrentPlayer(), false));
                        case RANDOM_WITH_PAWN -> board.set(toIndex, getRandomPiece(currentState.getCurrentPlayer(), true));
                        default -> board.set(toIndex, new Pawn(currentState.getCurrentPlayer(), true));
                    }
                }
                //TODO Меняем состояние
                return currentState.toBuilder()
                        .board(board)
                        .enPassant(newEnPassant).build();
            }
            case "king" -> {
                King king = (King) piece;
                board.set(toIndex, king.copyWith(true));
                ArrayList<Integer> kings = new ArrayList<>(currentState.getKings());
                kings.set(currentState.getCurrentPlayer(), toIndex);
                //TODO Меняем состояние
                return currentState.toBuilder()
                        .board(board)
                        .kings(kings).build();
            }
            case "rook" -> {
                Rook rook = (Rook) piece;
                board.set(toIndex, rook.copyWith(true));
                //TODO Меняем состояние
                return currentState.toBuilder().board(board).build();
            }
            default -> {
                board.set(toIndex, piece);
                //TODO Меняем состояние
                return currentState.toBuilder().board(board).build();
            }
        }
    }//
    //TODO Меняем состояние, возвращаем checkNextTurn, которому отдали ссылку на полученное состояние
    private GameState nextPlayer(GameState state) {
        int next = (state.getCurrentPlayer() + 1) % 4;
        //TODO Меняем состояние
        state.setCurrentPlayer(next);
        return checkNextTurn(state);
    }
    //Не меняем состояние
    private boolean onFire(int index, List<ChessPiece> board, GameState state) {
        ChessPiece piece = board.get(index);
        if (piece == null) return false;

        for (int i = 0; i < board.size(); i++) {
            ChessPiece enemyPiece = board.get(i);
            if (enemyPiece != null &&
                    state.isEnemies(enemyPiece.getOwner(), piece.getOwner())) {
                GameState tempState = state.toBuilder().board(board).build();
                if (enemyPiece.getPossibleMoves(i, tempState).contains(index)) {
                    return true;
                }
            }
        }
        return false;
    }//Идентичное по функционалу
    //Не меняем состояние
    private List<Integer> getTruePossibleMoves(int index, GameState state) {
        ChessPiece piece = state.getBoard().get(index);
        if (piece == null) return new ArrayList<>();

        List<Integer> moves = piece.getPossibleMoves(index, state);

        if (state.getConfig().isOnlyRegicide()) {
            if ("king".equals(piece.getType())) {
                moves.addAll(possibleCastling(state));
            }
            return moves;
        }

        List<ChessPiece> draw = new ArrayList<>(state.getBoard());
        List<Integer> kings = new ArrayList<>(state.getKings());
        List<Integer> validMoves = new ArrayList<>();

        for (int move : moves) {
            ChessPiece temp = draw.get(move);
            draw.set(index, null);
            draw.set(move, piece);

            int currentKingPos = kings.get(state.getCurrentPlayer());
            if ("king".equals(piece.getType())) {
                currentKingPos = move;
            }

            if (!onFire(currentKingPos, draw, state)) {
                validMoves.add(move);
            }//мелкое изменение логики, здесь мы смотрим от обратного

            draw.set(move, temp);
            draw.set(index, piece);
        }

        if ("king".equals(piece.getType())) {
            validMoves.addAll(possibleCastling(state));
        }

        return validMoves;
    }//Идентичное по функционалу
    //Не меняем состояние
    private List<Integer> possibleCastling(GameState state) {
        List<Integer> possible = new ArrayList<>();
        List<ChessPiece> board = state.getBoard();
        int kingIdx = state.getKings().get(state.getCurrentPlayer());

        ChessPiece kingPiece = board.get(kingIdx);
        if (!(kingPiece instanceof King) || ((King) kingPiece).isHasMoved()) {
            return possible;
        }

        List<Integer> rooks = new ArrayList<>();
        for (int i = 0; i < board.size(); i++) {
            ChessPiece p = board.get(i);
            if (p instanceof Rook rook &&
                    p.getOwner() == state.getCurrentPlayer() &&
                    !rook.isHasMoved()) {
                rooks.add(i);
            }
        }

        if (rooks.isEmpty()) return possible;

        for (int rookIndex : rooks) {
            if (cleanWayForCastling(rookIndex, state)) {
                possible.add(rookIndex);
            }
        }
        return possible;
    }//Идентичное по функционалу
    //Не меняем состояние
    private boolean cleanWayForCastling(int rookIndex, GameState state) {
        int kingIndex = state.getKings().get(state.getCurrentPlayer());
        int boardSize = BoardData.boardSize;

        if (rookIndex < kingIndex) {
            int step = (state.getCurrentPlayer() == 1 || state.getCurrentPlayer() == 3) ? boardSize : 1;
            for (int x = rookIndex + step; x < kingIndex; x += step) {
                if (state.getBoard().get(x) != null) return false;
            }
        } else {
            int step = (state.getCurrentPlayer() == 1 || state.getCurrentPlayer() == 3) ? boardSize : 1;
            for (int x = rookIndex - step; x > kingIndex; x -= step) {
                if (state.getBoard().get(x) != null) return false;
            }
        }

        return checkKingPathNotOnFire(kingIndex, rookIndex, state);
    } //Идентичное по функционалу, оптимизирован
    //Не меняем состояние
    private boolean checkKingPathNotOnFire(int kingIndex, int rookIndex, GameState state) {
        int boardSize = BoardData.boardSize;
        int player = state.getCurrentPlayer();

        if (rookIndex < kingIndex) {
            switch (player) {
                case 0 -> { for (int i = kingIndex; i > kingIndex - 3; i--) if (onFire(i, state.getBoard(), state)) return false; }
                case 1 -> { for (int i = kingIndex; i > kingIndex - 3 * boardSize; i -= boardSize) if (onFire(i, state.getBoard(), state)) return false; }
                case 2 -> { for (int i = kingIndex; i > kingIndex - 3; i--) if (onFire(i, state.getBoard(), state)) return false; }
                case 3 -> { for (int i = kingIndex; i > kingIndex - 3 * boardSize; i -= boardSize) if (onFire(i, state.getBoard(), state)) return false; }
            }
        } else {
            switch (player) {
                case 0 -> { for (int i = kingIndex; i < kingIndex + 3; i++) if (onFire(i, state.getBoard(), state)) return false; }
                case 1 -> { for (int i = kingIndex; i < kingIndex + 3 * boardSize; i += boardSize) if (onFire(i, state.getBoard(), state)) return false; }
                case 2 -> { for (int i = kingIndex; i < kingIndex + 3; i++) if (onFire(i, state.getBoard(), state)) return false; }
                case 3 -> { for (int i = kingIndex; i < kingIndex + 3 * boardSize; i += boardSize) if (onFire(i, state.getBoard(), state)) return false; }
            }
        }
        return true;
    }//вспомогательный метод, лишь разбивает функционал
    //Не меняем состояние
    private Stalemate checkmate(GameState state) {
        if (state.getConfig().isOnlyRegicide()) {
            for (int i = 0; i < state.getBoard().size(); i++) {
                ChessPiece p = state.getBoard().get(i);
                if (p != null && p.getOwner() == state.getCurrentPlayer() && !getTruePossibleMoves(i, state).isEmpty()) {
                    return null;
                }
            }
            return Stalemate.SKIP_MOVE;
        }

        List<ChessPiece> board = new ArrayList<>(state.getBoard());
        List<Integer> kings = new ArrayList<>(state.getKings());

        for (int i = 0; i < board.size(); i++) {
            ChessPiece piece = board.get(i);
            if (piece != null && piece.getOwner() == state.getCurrentPlayer()) {
                for (int move : piece.getPossibleMoves(i, state)) {
                    ChessPiece temp = board.get(move);
                    board.set(i, null);
                    board.set(move, piece);
                    int kingPos = "king".equals(piece.getType()) ? move : kings.get(state.getCurrentPlayer());

                    boolean safe = !onFire(kingPos, board, state);

                    // Откат
                    board.set(move, temp);
                    board.set(i, piece);

                    if (safe) return null;
                }
            }
        }

        if (onFire(kings.get(state.getCurrentPlayer()), board, state)) {
            return Stalemate.CHECKMATE;
        } else {
            return state.ifStalemate();
        }
    }//Идентичное по функционалу
    //TODO Меняем состояние, возвращаю ссылку на полученное состояние
    private GameState checkNextTurn(GameState state) {
        boolean wasChange = false;

        if (state.getAlive().get(state.getCurrentPlayer())) {
            Map<Integer, List<Integer>> resetEP = new HashMap<>();
            resetEP.put(state.getCurrentPlayer(), List.of(-1, -1));
            //TODO Меняем состояние
            state.getEnPassant().put(state.getCurrentPlayer(), List.of(-1, -1));

            Stalemate result = checkmate(state);
            if (result != null) {
                switch (result) {
                    case CHECKMATE -> {
                        List<ChessPiece> board = new ArrayList<>(state.getBoard());
                        for (int i = 0; i < board.size(); i++) {
                            if (board.get(i) != null && board.get(i).getOwner() == state.getCurrentPlayer()) {
                                board.set(i, board.get(i).kill());
                            }
                        }
                        //TODO Меняем состояние
                        state.setBoard(board);
                        ArrayList<Boolean> alive = new ArrayList<>(state.getAlive());
                        alive.set(state.getCurrentPlayer(), false);
                        state.setAlive(alive);
                        state.setCurrentPlayer((state.getCurrentPlayer() + 1) % 4);

                        wasChange = true;
                    }
                    case Stalemate.DRAW -> state.setStatus(GameStatus.DRAW); //TODO Меняем состояние
                    case SKIP_MOVE -> {
                        //TODO Меняем состояние
                        state.setCurrentPlayer((state.getCurrentPlayer()+ 1) % 4);
                        wasChange = true;
                    }
                }
            }
        }
        //TODO Меняем состояние
        if(!gameIsActive(state)) state.setStatus(GameStatus.OVER);

        if (state.getStatus() == GameStatus.ACTIVE) {
            while (!state.getAlive().get(state.getCurrentPlayer())) {
                //TODO Меняем состояние
                state.setCurrentPlayer((state.getCurrentPlayer()+ 1) % 4);
                wasChange = true;
            }
            if (wasChange) return checkNextTurn(state);
        }
        return state;
    }//Идентичное по функционалу
    //Не меняем состояние
    private ChessPiece getRandomPiece(int owner, boolean includePawn) {
        int range = includePawn ? 5 : 4;
        int val = new Random().nextInt(range);
        return switch (val) {
            case 0 -> new Queen(owner);
            case 1 -> new Rook(owner, true);
            case 2 -> new Knight(owner);
            case 3 -> new Bishop(owner);
            default -> new Pawn(owner, true);
        };
    }//Вспомогательный метод
    //TODO Меняем состояние, возвращаем ссылку на полученное состояние
    private GameState makeCastling(GameState state, int rookIndex) {
        int kingIndex = state.getKings().get(state.getCurrentPlayer());
        int newKingIndex = kingIndex;
        int newRookIndex = rookIndex;
        int boardSize = BoardData.boardSize;
        int player = state.getCurrentPlayer();

        if (rookIndex < kingIndex) {
            switch (player) {
                case 0 -> { newRookIndex = rookIndex + 3; newKingIndex = kingIndex - 2; }
                case 1 -> { newRookIndex = rookIndex + 3 * boardSize; newKingIndex = kingIndex - 2 * boardSize; }
                case 2 -> { newRookIndex = rookIndex + 2; newKingIndex = kingIndex - 2; }
                case 3 -> { newRookIndex = rookIndex + 2 * boardSize; newKingIndex = kingIndex - 2 * boardSize; }
            }
        } else {
            switch (player) {
                case 0 -> { newRookIndex = rookIndex - 2; newKingIndex = kingIndex + 2; }
                case 1 -> { newRookIndex = rookIndex - 2 * boardSize; newKingIndex = kingIndex + 2 * boardSize; }
                case 2 -> { newRookIndex = rookIndex - 3; newKingIndex = kingIndex + 2; }
                case 3 -> { newRookIndex = rookIndex - 3 * boardSize; newKingIndex = kingIndex + 2 * boardSize; }
            }
        }

        List<ChessPiece> board = new ArrayList<>(state.getBoard());

        board.set(newRookIndex, ((Rook) board.get(rookIndex)).copyWith(true));
        board.set(newKingIndex, ((King) board.get(kingIndex)).copyWith(true));

        board.set(rookIndex, null);
        board.set(kingIndex, null);

        ArrayList<Integer> kings = new ArrayList<>(state.getKings());
        kings.set(state.getCurrentPlayer(), newKingIndex);
        //TODO Меняем состояние
        state.setKings(kings);
        return state;
    }//Идентичное по функционалу
    //Не меняем состояние
    private boolean gameIsActive(GameState state){
        int activePlayer = state.getAlive().indexOf(true);
        if(activePlayer == 3 || activePlayer == -1) return false;
        return switch (state.getCommands()) {
            case Command.NONE ->
                    state.getAlive().get((activePlayer + 1) % 4) || state.getAlive().get((activePlayer + 2) % 4) || state.getAlive().get((activePlayer + 3) % 4);
            case Command.OPPOSITE_SIDES ->
                    state.getAlive().get((activePlayer + 1) % 4) || state.getAlive().get((activePlayer + 3) % 4);
            case Command.ADJACENT_SIDES -> {
                if (activePlayer > 1) yield false;
                yield state.getAlive().get(2) || state.getAlive().get(3);
            }
            default -> false;
        };
    }//Идентичное по функционалу
}
