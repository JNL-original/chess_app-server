package com.jnl.chess_server.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class GameRoom {
    private GameState state;
    private Map<Integer, String> playerTokens = new ConcurrentHashMap<>();//резервируем места за игроками
    private List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();//Не ограничиваем 4 игроками, чтобы могли подключаться зрители
    private List<String> readyList = new CopyOnWriteArrayList<>();
    private long lastActivityTimestamp = System.currentTimeMillis();//TODO реализовать в будущем, чтобы комната удалялась только если прошедшее время больше этого

    public GameRoom(OnlineConfig config) {
        this.state = GameState.initial(config);
    }

    public boolean isFull(){
        return playerTokens.size() == 4;
    }
    public boolean isEmpty(){
        return sessions.isEmpty();
    }
    public boolean roomIsReady(){
        return readyList.size() == 4;
    }
    public boolean playerIsReady(WebSocketSession session){
        String token = (String) session.getAttributes().get("token");
        return readyList.contains(token);
    }
    public synchronized void setReady(WebSocketSession session, boolean isReady){
        String token = (String) session.getAttributes().get("token");
        if(isReady){
           if(!readyList.contains(token)) readyList.add(token);
        }
        else readyList.remove(token);
    }
    public Map<Integer, Boolean> getReadyIndexes(){
        Map<Integer, Boolean> readyIndexes = new HashMap<>();
        for(Integer index : playerTokens.keySet()){
            if(readyList.contains(playerTokens.get(index))) readyIndexes.put(index, true);
            else readyIndexes.put(index, false);
        }
        return readyIndexes;
    }

    public synchronized int getPlayerIndex(String token){
        for(Integer key : playerTokens.keySet())
            if(playerTokens.get(key).equals(token))
                return key;
        return -1;
    }

    public synchronized void addPlayer(WebSocketSession session) {
        String token = (String) session.getAttributes().get("token");
        if(token == null) return;
        if(!sessions.contains(session)) sessions.add(session);
        if(playerTokens.containsValue(token)) return;
        for (int i = 0; i < 4; i++){
            if(!playerTokens.containsKey(i)) {
                playerTokens.put(i, token);
                return;
            }
        }
    }
    public synchronized void removePlayer(WebSocketSession session){
        sessions.remove(session);
        String token = (String) session.getAttributes().get("token");
        readyList.remove(token);
        if(token == null || !playerTokens.containsValue(token)) return;
        for (int i = 0; i < 4; i++){
            if(Objects.equals(playerTokens.get(i), token)) {
                playerTokens.remove(i);
            }
        }
    }
    public synchronized void removeSession(WebSocketSession session){
        sessions.remove(session);
    }
}
