package com.futurevet.mensageria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.futurevet.model.TipoEvento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ProdutorEventosTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ConsumidorNotificacoes consumidor = mock(ConsumidorNotificacoes.class);
    private final EventoFuturevet evento = new EventoFuturevet(TipoEvento.CONSULTA_CONFIRMADA, 2L, "Bento",
            "10:00", LocalDate.of(2026, 11, 4), LocalDateTime.of(2026, 10, 26, 9, 0));

    @Test
    void comRabbitAtivoPublicaNoRoteadorComAChaveDeRota() {
        new ProdutorEventos(rabbit, consumidor, true).publicar(evento);
        verify(rabbit).convertAndSend(RabbitMQConfig.ROTEADOR, RabbitMQConfig.CHAVE_ROTA, evento);
        verify(consumidor, never()).processar(any(), any());
    }

    @Test
    void semBrokerEntregaDiretoAoConsumidor() {
        new ProdutorEventos(rabbit, consumidor, false).publicar(evento);
        verify(consumidor).processar(evento, CanalEntrega.LOCAL);
        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void brokerForaDoArNaoPerdeOAviso() {
        doThrow(new AmqpConnectException(new RuntimeException("conexão recusada"))).when(rabbit)
                .convertAndSend(eq(RabbitMQConfig.ROTEADOR), eq(RabbitMQConfig.CHAVE_ROTA), any(Object.class));
        new ProdutorEventos(rabbit, consumidor, true).publicar(evento);
        verify(consumidor).processar(evento, CanalEntrega.LOCAL);
    }
}
