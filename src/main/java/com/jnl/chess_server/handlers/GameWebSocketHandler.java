package com.jnl.chess_server.handlers;

import com.jnl.chess_server.models.BoardData;
import com.jnl.chess_server.models.GameRoom;
import com.jnl.chess_server.models.GameState;
import com.jnl.chess_server.models.OnlineConfig;
import com.jnl.chess_server.models.enums.GameStatus;
import com.jnl.chess_server.models.pieces.ChessPiece;
import com.jnl.chess_server.repositories.GameRepository;
import com.jnl.chess_server.services.GameService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@AllArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {
    private GameService gameService;
    private GameRepository gameRepository;
    private ObjectMapper objectMapper;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        //Обработка токена
        String query = uri.getQuery();
        String token = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("token", token);

        //Обработка id комнаты
        String path = session.getUri().getPath();
        String roomId = extractRoomId(path);

        if (roomId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put("roomId", roomId);
    }

    private String extractRoomId(String path) {
        String[] parts = path.split("/");

        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                return parts[i]; // Возвращаем последний непустой сегмент
            }
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println(message);
        // Здесь ты будешь получать ходы от Flutter
        String roomId = (String) session.getAttributes().get("roomId");
        String token = (String) session.getAttributes().get("token");

        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText(null);
        if(type == null) return;

        if(roomId.equals("new")){
            if(type.equals("create")) {
                OnlineConfig config = objectMapper.treeToValue(json.get("config"), OnlineConfig.class);
                if(config == null) return;
                roomId = gameRepository.createRoom(config);
                Map<String, String> response = Map.of(
                        "type", "new_room",
                        "roomId", roomId
                );
                if(session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
            return;
        }

        // 2. Достаем контекст из атрибутов
        GameRoom room = gameRepository.getRoom(roomId);
        if(room == null){
            Map<String, Object> response = new HashMap<>();
            response.put("type", "notExist");
            if(session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }

        synchronized (room){
            if(type.equals("connect")){
                room.addPlayer(session);
                Map<String, Object> response = new HashMap<>();

                response.put("type", "sync");
                response.put("data", room.getState().toBuilder().myPlayerIndex(room.getPlayerIndex(token)));//-1 если зритель
                if(room.getState().getStatus().equals(GameStatus.LOBBY)) response.put("ready", room.getReadyIndexes());

                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                try {
                    if (session.isOpen()) session.sendMessage(jsonMessage);
                } catch (IOException e) {
                    System.err.println("Не удалось отправить сообщение сессии " + session.getId());
                }
            }
            if(type.equals("move")){
                if(!room.getPlayerTokens().containsValue(token)
                        || !room.getState().getStatus().equals(GameStatus.ACTIVE)
                        || room.getState().getCurrentPlayer() != room.getPlayerIndex(token)) return;

                Integer fromIndex = objectMapper.treeToValue(json.get("from"), Integer.class);
                Integer toIndex = objectMapper.treeToValue(json.get("to"), Integer.class);
                if(fromIndex == null || toIndex == null) return;

                room.getState().setTurn(room.getState().getTurn()+1);//увеличиваю контрольно число
                GameState prevState = room.getState().toBuilder().build();
                try{
                    GameState newState = gameService.handleMove(room.getState(), fromIndex, toIndex);
                    if(newState == null) return;
                    room.setState(newState);
                } catch(Exception ex){
                    System.err.println("Ошибка в превращении, комната " + roomId);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("type", "update");
                response.put("turn", room.getState().getTurn());
                response.put("data", changeSeeker(prevState, room.getState()));
                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                room.getSessions().forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(jsonMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Не удалось отправить сообщение сессии " + s.getId());
                    }
                });
            }
            if(type.equals("promotion")){
                if(!room.getPlayerTokens().containsValue(token)
                        || !room.getState().getStatus().equals(GameStatus.WAITING_FOR_PROMOTION)
                        || room.getState().getCurrentPlayer() != room.getPlayerIndex(token)) return;
                ChessPiece piece = objectMapper.treeToValue(json.get("who"), ChessPiece.class);
                if(piece == null) return;
                room.getState().setTurn(room.getState().getTurn()+1);//увеличиваю контрольно число
                GameState prevState = room.getState().toBuilder().build();
                try{
                    GameState newState = gameService.continueGameAfterPromotion(piece, room.getState());
                    if(newState == null) return;
                    room.setState(newState);
                } catch(Exception ex){
                    System.err.println("Ошибка в превращении, комната " + roomId);
                }
                Map<String, Object> response = new HashMap<>();
                response.put("type", "update");
                response.put("turn", room.getState().getTurn());
                response.put("data", changeSeeker(prevState, room.getState()));
                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                room.getSessions().forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(jsonMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Не удалось отправить сообщение сессии " + s.getId());
                    }
                });
            }
            if(type.equals("ready")){
                if(!room.getPlayerTokens().containsValue(token)
                        || !room.getState().getStatus().equals(GameStatus.LOBBY)) return;

                Integer color = objectMapper.treeToValue(json.get("color"), Integer.class);//Может быть null
                String name = objectMapper.treeToValue(json.get("name"), String.class);//Может быть null

                int playerIndex = room.getPlayerIndex(token);
                OnlineConfig config = room.getState().getConfig();

                Map<String, Object> response = new HashMap<>();
                response.put("type", "lobby");

                room.getState().setTurn(room.getState().getTurn()+1);
                response.put("turn", room.getState().getTurn());

                room.setReady(session, true);
                response.put("ready", room.getReadyIndexes());

                if(color != null) {
                    config.getPlayerColors().put(playerIndex, color);
                    response.put("colors", config.getPlayerColors());
                }
                if(name != null)  {
                    config.getPlayerNames().put(playerIndex, name);
                    response.put("names", config.getPlayerNames());
                }

                if(room.roomIsReady()) {
                    room.getState().setStatus(GameStatus.ACTIVE);
                    response.put("status", GameStatus.ACTIVE);
                }

                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                room.getSessions().forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(jsonMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Не удалось отправить сообщение сессии " + s.getId());
                    }
                });
            }

            if(type.equals("cancelReady")){
                if(!room.getPlayerTokens().containsValue(token)
                        || !room.getState().getStatus().equals(GameStatus.LOBBY)) return;
                room.setReady(session, false);
                Map<String, Object> response = new HashMap<>();
                response.put("type", "lobby");
                room.getState().setTurn(room.getState().getTurn()+1);
                response.put("turn", room.getState().getTurn());
                response.put("ready", room.getReadyIndexes());

                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                room.getSessions().forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(jsonMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Не удалось отправить сообщение сессии " + s.getId());
                    }
                });
            }
            if(type.equals("change")){
                if(!room.getPlayerTokens().containsValue(token)
                        || !room.getState().getStatus().equals(GameStatus.LOBBY)) return;

                Integer color = objectMapper.treeToValue(json.get("color"), Integer.class);//Может быть null
                String name = objectMapper.treeToValue(json.get("name"), String.class);//Может быть null

                int playerIndex = room.getPlayerIndex(token);
                OnlineConfig config = room.getState().getConfig();

                Map<String, Object> response = new HashMap<>();
                response.put("type", "lobby");

                room.getState().setTurn(room.getState().getTurn()+1);

                response.put("turn", room.getState().getTurn());
                response.put("ready", room.getReadyIndexes());

                if(color != null) {
                    boolean canChange = true;
                    Map<Integer, String> playerTokens = room.getPlayerTokens();
                    for(int i = 0; i < 4; i ++){
                        if(!color.equals(config.getPlayerColors().get(i))) continue;
                        if(playerTokens.get(i) != null){
                            canChange = false;
                        }
                        else{
                            if(!config.getPlayerColors().containsValue(0xFFFFEB3B)){
                                config.getPlayerColors().put(i, 0xFFFFEB3B);
                                continue;
                            }
                            if(!config.getPlayerColors().containsValue(0xFF2196F3)){
                                config.getPlayerColors().put(i, 0xFF2196F3);
                                continue;
                            }
                            if(!config.getPlayerColors().containsValue(0xFFF44336)){
                                config.getPlayerColors().put(i, 0xFFF44336);
                                continue;
                            }
                            if(!config.getPlayerColors().containsValue(0xFF4CAF50)){
                                config.getPlayerColors().put(i, 0xFF4CAF50);
                            }
                        }
                    }

                    if(canChange){
                        config.getPlayerColors().put(playerIndex, color);
                        response.put("colors", config.getPlayerColors());
                    }
                }
                if(name != null) {
                    boolean canChange = true;
                    Map<Integer, String> playerTokens = room.getPlayerTokens();
                    for(int i = 0; i < 4; i ++){
                        if(!name.equals(config.getPlayerNames().get(i))) continue;
                        if(playerTokens.get(i) != null){
                            canChange = false;
                        }
                        else{
                            if(!config.getPlayerNames().containsValue("игрок 1")){
                                config.getPlayerNames().put(i, "игрок 1");
                                continue;
                            }
                            if(!config.getPlayerNames().containsValue("игрок 2")){
                                config.getPlayerNames().put(i, "игрок 2");
                                continue;
                            }
                            if(!config.getPlayerNames().containsValue("игрок 3")){
                                config.getPlayerNames().put(i, "игрок 3");
                                continue;
                            }
                            if(!config.getPlayerNames().containsValue("игрок 4")){
                                config.getPlayerNames().put(i, "игрок 4");
                            }
                        }
                    }

                    if(canChange){
                        config.getPlayerNames().put(playerIndex, name);
                        response.put("names", config.getPlayerNames());
                    }
                }

                TextMessage jsonMessage =  new TextMessage(objectMapper.writeValueAsString(response));
                room.getSessions().forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(jsonMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Не удалось отправить сообщение сессии " + s.getId());
                    }
                });
            }
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null && gameRepository.getRoom(roomId) != null) {
            GameRoom room = gameRepository.getRoom(roomId);
            if(room.getState().getStatus().equals(GameStatus.LOBBY)) room.removePlayer(session);
            else room.removeSession(session);
        }
    }



    private Map<String, Object> changeSeeker(GameState prev, GameState cur){

        Map<String, Object> changes = new HashMap<>();

        if(Objects.equals(prev, cur) || prev == null || cur == null) return changes;
        if(!Objects.equals(prev.getConfig(), cur.getConfig()))changes.put("config", cur.getConfig());
        if(prev.getCurrentPlayer() != cur.getCurrentPlayer()) changes.put("currentPlayer", cur.getCurrentPlayer());
        if(prev.getPromotionPawn() != cur.getPromotionPawn()) changes.put("promotionPawn", cur.getPromotionPawn());
        if(!prev.getStatus().equals(cur.getStatus())) changes.put("status", cur.getStatus());
        if(!prev.getCommands().equals(cur.getCommands())) changes.put("commands", cur.getCommands());
        if(!Objects.equals(prev.getEnPassant(), cur.getEnPassant())) changes.put("enPassant", cur.getEnPassant());
        if(!Objects.equals(prev.getAlive(), cur.getAlive())) changes.put("alive", cur.getAlive());
        if(!Objects.equals(prev.getKings(), cur.getKings())) changes.put("kings", cur.getKings());
        if(!Objects.equals(prev.getBoard(), cur.getBoard())) {
            Map<Integer, Object> tiles = new HashMap<>();
            for(int i = 0; i < BoardData.totalTiles; i++){
                if(!Objects.equals(prev.getBoard().get(i), cur.getBoard().get(i))){
                    tiles.put(i, cur.getBoard().get(i));
                }
            }
            changes.put("tiles", tiles);
        }
        return changes;
    }
}
