package com.jnl.chess_server.models;

import com.jnl.chess_server.models.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineConfig {
    // Пользовательский интерфейс (цвета храним как HEX-инты)
    private Map<Integer, Long> playerColors;
    private Map<Integer, String> playerNames;

    // Логика программы
    private Command commands;
    private Promotion pawnPromotion;
    private int promotionCondition;
    private boolean onlyRegicide;

    // Настройки пата
    private Stalemate oneOnOneStalemate;
    private Stalemate aloneAmongAloneStalemate;
    private Stalemate commandOnOneStalemate;
    private Stalemate commandOnCommandStalemate;

    // Таймеры
    private TimerType timerType;
    private Double timerTime;
    private TimeOut timeOut;

    // Специфичные для онлайна
    private boolean publicAccess;
    private LoseConnection ifConnectionIsLost;

    // Значения по умолчанию (аналог конструктора в Dart)
    public static OnlineConfig createDefault() {
        return OnlineConfig.builder()
                .playerColors(Map.of(-1, 0xFF9E9E9EL, 0, 0xFFFFEB3BL, 1, 0xFF2196F3L, 2, 0xFFF44336L, 3, 0xFF4CAF50L))
                .playerNames(Map.of(0, "игрок 1", 1, "игрок 2", 2, "игрок 3", 3, "игрок 4"))
                .commands(Command.NONE)
                .pawnPromotion(Promotion.QUEEN)
                .promotionCondition(9)
                .onlyRegicide(false)
                .oneOnOneStalemate(Stalemate.DRAW)
                .aloneAmongAloneStalemate(Stalemate.CHECKMATE)
                .commandOnOneStalemate(Stalemate.DRAW)
                .commandOnCommandStalemate(Stalemate.CHECKMATE)
                .timerType(TimerType.NONE)
                .timerTime(Double.POSITIVE_INFINITY)
                .timeOut(TimeOut.RANDOM_MOVES)
                .publicAccess(true)
                .ifConnectionIsLost(LoseConnection.WAIT)
                .build();
    }
}