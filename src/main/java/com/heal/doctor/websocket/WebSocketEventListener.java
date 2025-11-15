package com.heal.doctor.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        if (sessionId == null) {
            return;
        }

        String doctorId = headers.getFirstNativeHeader("doctorId");
        
        if (doctorId == null) {
            Map<String, Object> sessionAttributes = headers.getSessionAttributes();
            if (sessionAttributes != null) {
                doctorId = (String) sessionAttributes.get("doctorId");
            }
        }

        if (doctorId != null && sessionId != null) {
            sessionRegistry.registerSession(doctorId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        if (sessionId != null) {
            sessionRegistry.removeSession(sessionId);
        }
    }
}