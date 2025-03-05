package com.heal.doctor.websocket;
import com.heal.doctor.utils.CurrentUserName;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Extract doctorId from JWT
        String doctorId = CurrentUserName.getCurrentDoctorId();

        if (doctorId == null) {
            throw new RuntimeException("Unauthorized: Doctor must be logged in to subscribe.");
        }

        attributes.put("doctorId", doctorId); // Store doctorId in session attributes
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}
