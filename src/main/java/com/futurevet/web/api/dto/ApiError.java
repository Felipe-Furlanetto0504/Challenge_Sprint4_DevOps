package com.futurevet.web.api.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String mensagem, List<String> detalhes) {
    public ApiError(int status, String mensagem) {
        this(Instant.now(), status, mensagem, List.of());
    }
    public ApiError(int status, String mensagem, List<String> detalhes) {
        this(Instant.now(), status, mensagem, detalhes);
    }
}