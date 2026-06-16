package com.heal.doctor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secretKey}")
    private String SECRET_KEY;

    @Value("${jwt.access.expiry.hours}")
    private int EXPIRY_HOUR;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String userId, String doctorId, String role) {
        long expirationTime = 1000L * 60 * 60 * EXPIRY_HOUR;

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId);
        claims.put("doctorId", doctorId);
        claims.put("role", role != null ? role : "DOCTOR");

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSecretKey())
                .compact();
    }

    // Overloaded method for backward compatibility (for DOCTOR role where userId = doctorId)
    public String generateToken(String username, String doctorId, String role) {
        return generateToken(username, doctorId, doctorId, role);
    }

    // Overloaded method for backward compatibility (defaults to DOCTOR role)
    public String generateToken(String username, String doctorId) {
        return generateToken(username, doctorId, "DOCTOR");
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractDoctorId(String token) {
        return extractClaim(token, claims -> claims.get("doctorId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> {
            String role = claims.get("role", String.class);
            // Default to DOCTOR if role is null (for backward compatibility with old tokens)
            return role != null ? role : "DOCTOR";
        });
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
