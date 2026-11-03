package com.futurevet.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aviso exibido ao tutor ou à clínica ({@code usuarioId == null}), gerado a partir dos eventos que
 * passam pela fila de mensagens.
 */
public record Notificacao(long id, Long usuarioId, TipoEvento tipo, String animal, String detalhe,
        LocalDate dataEvento, LocalDateTime criadaEm, boolean lida) {
}
