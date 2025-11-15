package com.heal.doctor.websocket;

import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.utils.CurrentUserName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class DoctorHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DoctorHandshakeInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            String token = servletRequest.getServletRequest().getParameter("token");

            if (token == null || token.trim().isEmpty()) {
                String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    token = authHeader.substring(BEARER_PREFIX_LENGTH);
                }
            }

            if (token == null || token.trim().isEmpty()) {
                logger.warn("WebSocket handshake failed - no token provided");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            try {
                String username = jwtUtil.extractUsername(token);

                if (!jwtUtil.validateToken(token, username)) {
                    logger.warn("WebSocket handshake failed - invalid token: username: {}", username);
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                String doctorId = jwtUtil.extractDoctorId(token);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, doctorId, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                String extractedDoctorId = CurrentUserName.getCurrentDoctorId();

                attributes.put("doctorId", extractedDoctorId);
                attributes.put("username", username);
                attributes.put("token", token);

                logger.info("WebSocket handshake successful: doctorId: {}, username: {}", extractedDoctorId, username);
                return true;
            } catch (Exception e) {
                logger.warn("WebSocket handshake failed - authentication error: {}", e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        logger.warn("WebSocket handshake failed - invalid request type");
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}