package com.heal.doctor.websocket;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void removeSession(String sessionId) {
        userSessions.values().forEach(sessions -> sessions.remove(sessionId));
    }

    public Set<String> getSessionsByUserId(String userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }
}
