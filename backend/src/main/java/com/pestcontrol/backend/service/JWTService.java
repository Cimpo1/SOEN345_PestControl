package com.pestcontrol.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import com.pestcontrol.backend.domain.User;

public class JWTService {

    private static final String passPhrase = "ILovePotatooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo";
    private static final Key key = Keys.hmacShaKeyFor(passPhrase.getBytes());

    private static final long EXPIRATION_TIME = 3600000;

    public static String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUserId().toString())
                .claim("fullName", user.getFullName())
                .claim("email", user.getEmail())
                .claim("role", user.getUserRole())
                .claim("phoneNumber", user.getPhoneNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public static Long getUserId(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public static String getRole(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("role");
    }

    public static String getFullName(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("fullName");
    }

    public static String getEmail(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("email");
    }

    public static String getPhoneNumber(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("phoneNumber");
    }
}