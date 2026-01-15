package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Command {
    @JsonProperty("none") NONE,
    @JsonProperty("oppositeSides") OPPOSITE_SIDES,
    @JsonProperty("adjacentSides") ADJACENT_SIDES,
    @JsonProperty("random") RANDOM
}