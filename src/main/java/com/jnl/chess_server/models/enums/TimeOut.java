package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TimeOut {
    @JsonProperty("checkmate") CHECKMATE,
    @JsonProperty("randomMoves") RANDOM_MOVES
}
