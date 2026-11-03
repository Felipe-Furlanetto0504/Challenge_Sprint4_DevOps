package com.futurevet.monitoramento;

import com.futurevet.mensageria.CanalEntrega;
import com.futurevet.mensageria.ConsumidorNotificacoes;
import com.futurevet.mensageria.ProdutorEventos;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Lê as métricas expostas pelo Spring Boot Actuator (Micrometer) e a saúde dos componentes:
 * banco de dados, RabbitMQ e o microsserviço de IA (consultado com RestClient, pelo endereço fixo ou
 * pelo endereço registrado no Eureka).
 */
@Service
public class MonitoramentoService {
    public static final String SERVICO_IA = "futurevet-ia";
    static final String INDISPONIVEL = "DOWN";
    static final String NAO_REGISTRADO = "UNKNOWN";
    private static final double BYTES_POR_MB = 1024d * 1024d;
    private static final Duration TEMPO_LIMITE_IA = Duration.ofSeconds(3);

    private final MeterRegistry registry;
    private final HealthEndpoint saude;
    private final DiscoveryClient discovery;
    private final ProdutorEventos produtor;
    private final String iaUrl;
    private final RestClient restClient;

    public MonitoramentoService(MeterRegistry registry, HealthEndpoint saude, DiscoveryClient discovery,
            ProdutorEventos produtor, @Value("${futurevet.ia.url:}") String iaUrl) {
        this.registry = registry;
        this.saude = saude;
        this.discovery = discovery;
        this.produtor = produtor;
        this.iaUrl = iaUrl;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TEMPO_LIMITE_IA);
        fabrica.setReadTimeout(TEMPO_LIMITE_IA);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
    }

    public PainelMonitoramento painel() {
        return new PainelMonitoramento(soma("jvm.memory.used") / BYTES_POR_MB,
                soma("jvm.memory.max") / BYTES_POR_MB, gauge("process.cpu.usage") * 100,
                gauge("system.cpu.usage") * 100, gauge("process.uptime") / 60, requisicoesHttp(),
                status(saude.health()), status(saude.healthForPath("db")), statusIa(), produtor.rabbitAtivo(),
                status(saude.healthForPath("rabbit")), notificacoes(CanalEntrega.RABBITMQ),
                notificacoes(CanalEntrega.LOCAL));
    }

    /** Memória é medida por área da JVM (heap, metaspace...); somamos as áreas com valor conhecido. */
    private double soma(String metrica) {
        return registry.find(metrica).gauges().stream().mapToDouble(Gauge::value).filter(v -> v > 0).sum();
    }

    private double gauge(String metrica) {
        Gauge g = registry.find(metrica).gauge();
        return g == null || Double.isNaN(g.value()) ? 0 : g.value();
    }

    private long requisicoesHttp() {
        return registry.find("http.server.requests").timers().stream().mapToLong(Timer::count).sum();
    }

    private double notificacoes(CanalEntrega canal) {
        Counter c = registry.find(ConsumidorNotificacoes.METRICA).tag("canal", canal.name().toLowerCase())
                .counter();
        return c == null ? 0 : c.count();
    }

    private static String status(HealthComponent componente) {
        return componente == null ? NAO_REGISTRADO : componente.getStatus().getCode();
    }

    String statusIa() {
        return enderecoIa().map(this::consultarSaudeIa).orElse(NAO_REGISTRADO);
    }

    private Optional<URI> enderecoIa() {
        if (!iaUrl.isBlank())
            return Optional.of(URI.create(iaUrl));
        return discovery.getInstances(SERVICO_IA).stream().findFirst().map(ServiceInstance::getUri);
    }

    private String consultarSaudeIa(URI base) {
        try {
            Map<?, ?> corpo = restClient.get().uri(base.resolve("/actuator/health")).retrieve().body(Map.class);
            return corpo == null ? INDISPONIVEL : String.valueOf(corpo.get("status"));
        } catch (RuntimeException e) {
            return INDISPONIVEL;
        }
    }
}
