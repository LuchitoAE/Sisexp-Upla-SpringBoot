package com.upla.sisexp.notificacion.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_EXPEDIENTE_EVENTOS = "expediente.eventos";

    @Bean
    public Queue expedienteEventosQueue() { return new Queue(QUEUE_EXPEDIENTE_EVENTOS, true); }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
