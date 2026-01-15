package com.jnl.chess_server.models;


import com.jnl.chess_server.models.pieces.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BoardData{
    public static final int boardSize = 14;
    public static final int  totalTiles = boardSize*boardSize;
    public static final List<Integer> corners = List.of(
            0, 1, 2, 11, 12, 13,
            14, 15, 16, 25, 26, 27,
            28, 29, 30, 39, 40, 41,

            154, 155, 156, 165, 166, 167,
            168, 169, 170, 179, 180, 181,
            182, 183, 184, 193, 194, 195
            ); //если их перезаписывать надо еще поменять promotion conditions

    public static List<ChessPiece> initializePieces() {
        // Создаем список, заполненный null (аналог List.generate)
        List<ChessPiece> board = new ArrayList<>(Collections.nCopies(totalTiles, null));

        // --- ИГРОК 2 (СВЕРХУ) ---
        setRow(board, 0, 2);
        setPawnsRow(board, 1, 2, true);

        // --- ИГРОК 0 (ВНИЗУ) ---
        setRow(board, 13, 0);
        setPawnsRow(board, 12, 0, true);

        // --- ИГРОК 1 (СЛЕВА) ---
        setColumn(board, 0, 1);
        setPawnsRow(board, 1, 1, false);

        // --- ИГРОК 3 (СПРАВА) ---
        setColumn(board, 13, 3);
        setPawnsRow(board, 12, 3, false);

        return board;
    }

    public static boolean onBoard(int index){
        return index >= 0 && index < totalTiles && !corners.contains(index);
    }



    private static void setRow(List<ChessPiece> board, int row, int owner) {
        int start = row * 14 + 3;
        List<ChessPiece> pieces = new ArrayList<>(Arrays.asList(
                new Rook(owner), new Knight(owner), new Bishop(owner),
                new Queen(owner), new King(owner),
                new Bishop(owner), new Knight(owner), new Rook(owner)
        ));

        // Король и Ферзь меняются местами для симметрии
        if (row == 0) {
            Collections.swap(pieces, 3, 4);
        }

        for (int i = 0; i < pieces.size(); i++) {
            board.set(start + i, pieces.get(i));
        }
    }

    private static void setColumn(List<ChessPiece> board, int col, int owner) {
        int startOffset = 3 * 14 + col;
        List<ChessPiece> pieces = new ArrayList<>(Arrays.asList(
                new Rook(owner), new Knight(owner), new Bishop(owner),
                new Queen(owner), new King(owner),
                new Bishop(owner), new Knight(owner), new Rook(owner)
        ));

        if (col == 13) {
            Collections.swap(pieces, 3, 4);
        }

        for (int i = 0; i < pieces.size(); i++) {
            board.set(startOffset + (i * 14), pieces.get(i));
        }
    }

    private static void setPawnsRow(List<ChessPiece> board, int index, int owner, boolean isHorizontal) {
        if (isHorizontal) {
            int start = index * 14 + 3;
            for (int i = 0; i < 8; i++) {
                board.set(start + i, new Pawn(owner));
            }
        } else {
            int start = 3 * 14 + index;
            for (int i = 0; i < 8; i++) {
                board.set(start + (i * 14), new Pawn(owner));
            }
        }
    }
}
