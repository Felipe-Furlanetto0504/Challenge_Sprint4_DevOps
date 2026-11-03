package com.futurevet.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica no RabbitMQ os eventos do domínio, somente depois que a transação que os gerou foi
 * confirmada no banco. Sem broker configurado (ou se ele cair), entrega direto ao consumidor, para
 * que nenhum aviso se perca.
 */
@Component
public class ProdutorEventos {
    private static final Logger log = LoggerFactory.getLogger(ProdutorEventos.class);

    private final RabbitTemplate rabbit;
    private final ConsumidorNotificacoes consumidor;
    private final boolean rabbitAtivo;

    public ProdutorEventos(RabbitTemplate rabbit, ConsumidorNotificacoes consumidor,
            @Value("${futurevet.mensageria.rabbitmq:false}") boolean rabbitAtivo) {
        this.rabbit = rabbit;
        this.consumidor = consumidor;
        this.rabbitAtivo = rabbitAtivo;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void publicar(EventoFuturevet evento) {
        if (rabbitAtivo) {
            try {
                rabbit.convertAndSend(RabbitMQConfig.ROTEADOR, RabbitMQConfig.CHAVE_ROTA, evento);
                log.info("Evento {} enviado ao RabbitMQ", evento.tipo());
                return;
            } catch (AmqpException e) {
                log.warn("RabbitMQ indisponível ({}). Entregando o evento {} localmente.", e.getMessage(),
                        evento.tipo());
            }
        }
        consumidor.processar(evento, CanalEntrega.LOCAL);
    }

    public boolean rabbitAtivo() {
        return rabbitAtivo;
    }
}
