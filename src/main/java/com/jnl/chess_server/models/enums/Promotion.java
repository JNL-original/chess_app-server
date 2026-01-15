package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Promotion {
    @JsonProperty("queen") QUEEN,
    @JsonProperty("choice") CHOICE,
    @JsonProperty("random") RANDOM,
    @JsonProperty("randomWithPawn") RANDOM_WITH_PAWN,
    @JsonProperty("none") NONE
}
