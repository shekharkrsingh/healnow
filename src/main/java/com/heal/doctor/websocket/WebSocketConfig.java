package com.heal.doctor.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Enables a simple in-memory message broker
        config.setApplicationDestinationPrefixes("/app"); // Prefix for messages bound for methods
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws").setAllowedOrigins("*"); // WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:8080")
                .addInterceptors(new WebSocketAuthInterceptor()) // Add auth interceptor
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .withSockJS(); // Fallback for browsers that don’t support WebSocket
    }
}
