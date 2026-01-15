package com.jnl.chess_server.models.pieces;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jnl.chess_server.models.GameState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data // Генерирует геттеры, сеттеры, toString, equals и hashCode
@NoArgsConstructor // Пустой конструктор для Jackson
@AllArgsConstructor // Конструктор для создания объектов
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", // Определяем тип по этому полю
        visible = true // Оставляем поле 'type' в объекте после десериализации
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pawn.class, name = "pawn"),
        @JsonSubTypes.Type(value = King.class, name = "king"),
        @JsonSubTypes.Type(value = Rook.class, name = "rook"),
        @JsonSubTypes.Type(value = Knight.class, name = "knight"),
        @JsonSubTypes.Type(value = Bishop.class, name = "bishop"),
        @JsonSubTypes.Type(value = Queen.class, name = "queen")
})
public abstract class ChessPiece {
    private int owner;
    private String type;


    public abstract List<Integer> getPossibleMoves(int currentIndex, GameState game);
    public abstract ChessPiece kill();
    public abstract ChessPiece copy();

    @Override
    public String toString() {
        return type + " (player " + (owner + 1) + ")";
    }
}
