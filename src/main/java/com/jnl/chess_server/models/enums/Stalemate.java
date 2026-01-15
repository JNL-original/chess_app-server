package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Stalemate {
    @JsonProperty("checkmate") CHECKMATE,
    @JsonProperty("draw") DRAW,
    @JsonProperty("skipMove") SKIP_MOVE
}
