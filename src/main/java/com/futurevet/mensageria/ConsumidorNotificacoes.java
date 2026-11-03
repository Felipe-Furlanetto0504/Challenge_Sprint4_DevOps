package com.futurevet.mensageria;

import com.futurevet.service.NotificacaoService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorNotificacoes {
    public static final String METRICA = "futurevet.notificacoes.processadas";
    private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificacoes.class);

    private final NotificacaoService notificacoes;
    private final MeterRegistry metricas;

    public ConsumidorNotificacoes(NotificacaoService notificacoes, MeterRegistry metricas) {
        this.notificacoes = notificacoes;
        this.metricas = metricas;
    }

    @RabbitListener(queues = RabbitMQConfig.FILA, autoStartup = "${futurevet.mensageria.rabbitmq:false}")
    public void receber(EventoFuturevet evento) {
        processar(evento, CanalEntrega.RABBITMQ);
    }

    public void processar(EventoFuturevet evento, CanalEntrega canal) {
        notificacoes.registrar(evento);
        metricas.counter(METRICA, "canal", canal.name().toLowerCase()).increment();
        log.info("Notificação {} registrada (canal {})", evento.tipo(), canal);
    }
}
