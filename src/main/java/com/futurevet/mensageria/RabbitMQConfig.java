package com.futurevet.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILA = "futurevet.notificacoes";
    public static final String ROTEADOR = "futurevet.eventos";
    public static final String CHAVE_ROTA = "notificacao";

    @Bean
    public Queue filaNotificacoes() {
        return new Queue(FILA, true);
    }

    @Bean
    public DirectExchange roteadorEventos() {
        return new DirectExchange(ROTEADOR);
    }

    @Bean
    public Binding ligacaoNotificacoes(Queue filaNotificacoes, DirectExchange roteadorEventos) {
        return BindingBuilder.bind(filaNotificacoes).to(roteadorEventos).with(CHAVE_ROTA);
    }

    /** Eventos trafegam como JSON; só classes deste pacote podem ser reconstruídas a partir da mensagem. */
    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper tipos = new DefaultJackson2JavaTypeMapper();
        tipos.setTrustedPackages(EventoFuturevet.class.getPackageName());
        conversor.setJavaTypeMapper(tipos);
        return conversor;
    }
}
