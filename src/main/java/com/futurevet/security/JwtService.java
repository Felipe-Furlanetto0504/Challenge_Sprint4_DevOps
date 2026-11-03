package com.futurevet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtService {
    private static final long MILISSEGUNDOS_POR_MINUTO = 60_000L;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes:720}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String gerar(String email, String role) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + expirationMinutes * MILISSEGUNDOS_POR_MINUTO);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(agora)
                .expiration(expira)
                .signWith(key)
                .compact();
    }

    public String email(String token) {
        return claims(token, Claims::getSubject);
    }

    public boolean valido(String token, String emailEsperado) {
        try {
            return email(token).equalsIgnoreCase(emailEsperado) && !expirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean expirado(String token) {
        return claims(token, Claims::getExpiration).before(new Date());
    }

    private <T> T claims(String token, Function<Claims, T> resolver) {
        return resolver.apply(claims(token));
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}