package com.heal.doctor.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private UserHandshakeInterceptor doctorHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Existing SockJS endpoint — kept for backward compatibility / web clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(doctorHandshakeInterceptor)
                .withSockJS();

        // Native WebSocket endpoint for React Native mobile app (no SockJS overhead)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(doctorHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
