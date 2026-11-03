package com.futurevet.web.api.dto;

public record TokenResponse(String token, String tipo, long usuarioId, String nome, String email, String role) {
    public TokenResponse(String token, long usuarioId, String nome, String email, String role) {
        this(token, "Bearer", usuarioId, nome, email, role);
    }
}