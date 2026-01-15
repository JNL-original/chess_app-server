package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TimerType {
    @JsonProperty("none") NONE,
    @JsonProperty("perPlayer") PER_PLAYER,
    @JsonProperty("perMove") PER_MOVE
}
