package com.heal.doctor.websocket;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Set<String>> doctorSessions = new ConcurrentHashMap<>();

    public void registerSession(String doctorId, String sessionId) {
        doctorSessions.computeIfAbsent(doctorId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void removeSession(String sessionId) {
        doctorSessions.values().forEach(sessions -> sessions.remove(sessionId));
    }

    public Set<String> getSessionsByDoctorId(String doctorId) {
        return doctorSessions.getOrDefault(doctorId, Collections.emptySet());
    }
}