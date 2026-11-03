package com.futurevet;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class FutureVetApplication {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public static void main(String[] args) {
        SpringApplication.run(FutureVetApplication.class, args);
    }

    /** Relógio único da aplicação: regras de data usam o horário de Brasília e ficam testáveis. */
    @Bean
    Clock clock() {
        return Clock.system(FUSO);
    }
}
