package com.heal.doctor.websocket;

import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.utils.CurrentUserName;
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
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            if (token == null || token.trim().isEmpty()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            try {
                String username = jwtUtil.extractUsername(token);
                
                if (!jwtUtil.validateToken(token, username)) {
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
                
                return true;
            } catch (Exception e) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
        
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}