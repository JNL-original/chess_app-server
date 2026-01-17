package com.jnl.chess_server.repositories;

import com.jnl.chess_server.models.GameRoom;
import com.jnl.chess_server.models.OnlineConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GameRepository {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();
    public synchronized String createRoom(OnlineConfig config){
        String roomId = getRandomId();
        while (rooms.containsKey(roomId)) roomId = getRandomId();
        GameRoom newRoom = new GameRoom(config);
        rooms.put(roomId, newRoom);
        return  roomId;
    }
    public GameRoom getRoom(String roomId){
        return rooms.get(roomId);
    }
    public synchronized boolean removeRoom(String roomId){
        return rooms.remove(roomId) != null;
    }
    public Map<String, GameRoom> getRooms(){
        return Collections.unmodifiableMap(rooms);
    }

    @Scheduled(fixedRate = 300000)
    public synchronized void removeEmptyRooms(){
        rooms.entrySet().removeIf(entry -> entry.getValue().isEmpty()
                && (System.currentTimeMillis() > entry.getValue().getLastActivityTimestamp() + 5 * 60 * 1000)
        );
    }


    private String getRandomId(){
        int length = 6;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
}
