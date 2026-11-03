package com.futurevet.mensageria;

import com.futurevet.model.TipoEvento;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mensagem trafegada pela fila. {@code destinatarioId} nulo significa "equipe da clínica".
 *
 * @param detalhe    horário da consulta ou nome da vacina
 * @param dataEvento data da consulta ou da aplicação
 */
public record EventoFuturevet(TipoEvento tipo, Long destinatarioId, String animal, String detalhe,
        LocalDate dataEvento, LocalDateTime ocorridoEm) {
}
