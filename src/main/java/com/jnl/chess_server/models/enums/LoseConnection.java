package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LoseConnection {
    @JsonProperty("checkmate") CHECKMATE,
    @JsonProperty("wait") WAIT,
    @JsonProperty("randomMoves") RANDOM_MOVES
}
