package com.jnl.chess_server.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GameStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("over") OVER,
    @JsonProperty("waitingForPromotion") WAITING_FOR_PROMOTION,
    @JsonProperty("draw") DRAW,
    @JsonProperty("lobby") LOBBY,
    @JsonProperty("connecting") CONNECTING,
    @JsonProperty("waitResponse") WAIT_RESPONSE
}