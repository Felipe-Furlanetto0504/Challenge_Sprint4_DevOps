package com.futurevet.service;

import com.futurevet.mensageria.EventoFuturevet;
import com.futurevet.model.TipoEvento;
import com.futurevet.security.Acesso;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Dispara os eventos do domínio. O destinatário é o outro lado da relação: ação da clínica avisa o
 * tutor do animal; ação do tutor avisa a clínica.
 */
@Component
public class PublicadorEventos {
    private final ApplicationEventPublisher eventos;
    private final Acesso acesso;
    private final Clock clock;

    public PublicadorEventos(ApplicationEventPublisher eventos, Acesso acesso, Clock clock) {
        this.eventos = eventos;
        this.acesso = acesso;
        this.clock = clock;
    }

    public void publicar(TipoEvento tipo, long tutorDoAnimal, String animal, String detalhe, LocalDate data) {
        Long destinatario = acesso.atual().admin() ? tutorDoAnimal : null;
        eventos.publishEvent(
                new EventoFuturevet(tipo, destinatario, animal, detalhe, data, LocalDateTime.now(clock)));
    }
}
