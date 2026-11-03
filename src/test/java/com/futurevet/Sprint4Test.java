package com.futurevet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurevet.ia.AssistenteClient;
import com.futurevet.ia.PerguntaAssistente;
import com.futurevet.ia.RespostaAssistente;
import com.futurevet.mensageria.ConsumidorNotificacoes;
import com.futurevet.mensageria.EventoFuturevet;
import com.futurevet.model.TipoEvento;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Sprint4Test {
    private static final String TUTOR = "tutor@futurevet.local";
    private static final String CLINICA = "clinica@futurevet.local";
    private static final String LOCAL_TESTE = "Sala Sprint 4";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate db;
    @Autowired
    Clock clock;
    @Autowired
    ConsumidorNotificacoes consumidor;
    @Autowired
    ObjectMapper json;

    @MockitoBean
    AssistenteClient assistente;

    @BeforeEach
    @AfterEach
    void limpar() {
        db.update("DELETE FROM TB_FV_NOTIFICACAO");
        db.update("DELETE FROM TB_CONSULTA WHERE LOCAL_CONSULTA=?", LOCAL_TESTE);
    }

    private static RequestPostProcessor tutor() {
        return user(TUTOR).roles("TUTOR");
    }

    private static RequestPostProcessor clinica() {
        return user(CLINICA).roles("ADMIN");
    }

    private String dataUtil() {
        return LocalDate.now(clock).plusDays(10).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).toString();
    }

    private String html(String url, RequestPostProcessor quem) throws Exception {
        return mvc.perform(get(url).with(quem)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void loginTrocaDeIdiomaELembraEscolhaEmCookie() throws Exception {
        String pt = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(pt).contains("Entrar na minha conta").contains("lang=\"pt-BR\"");

        var resposta = mvc.perform(get("/login").param("lang", "en")).andExpect(status().isOk())
                .andExpect(cookie().value("futurevet-idioma", "en")).andReturn().getResponse();
        assertThat(resposta.getContentAsString(StandardCharsets.UTF_8)).contains("Sign in to my account")
                .contains("lang=\"en\"");
    }

    @Test
    void telasProtegidasRenderizamEmIngles() throws Exception {
        assertThat(html("/consultas?lang=en", tutor())).contains("Appointments").contains("Awaiting confirmation");
        assertThat(html("/vacinas?lang=en", tutor())).contains("Vaccination card").contains("Overdue");
        assertThat(html("/animais?lang=en", tutor())).contains("Dog").contains("Medium");
        assertThat(html("/admin/monitoramento?lang=en", clinica())).contains("Monitoring");
    }

    @Test
    void regraDeNegocioEhExibidaNoIdiomaEscolhido() throws Exception {
        String domingo = LocalDate.now(clock).plusDays(7).with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).toString();
        String corpo = mvc.perform(post("/consultas/salvar").with(tutor()).with(csrf()).param("lang", "en")
                .param("animalId", "1").param("tipo", "Check-up").param("data", domingo).param("hora", "10:00")
                .param("local", LOCAL_TESTE)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(corpo).contains("Appointments from Monday to Saturday");
    }

    @Test
    void solicitacaoDoTutorAvisaClinicaEConfirmacaoDaClinicaAvisaTutor() throws Exception {
        mvc.perform(post("/consultas/salvar").with(tutor()).with(csrf()).param("animalId", "1")
                .param("tipo", "Retorno").param("data", dataUtil()).param("hora", "15:00")
                .param("local", LOCAL_TESTE)).andExpect(redirectedUrl("/consultas"));

        assertThat(db.queryForObject(
                "SELECT COUNT(*) FROM TB_FV_NOTIFICACAO WHERE TIPO='CONSULTA_SOLICITADA' AND ID_USUARIO IS NULL",
                Integer.class)).isEqualTo(1);
        assertThat(html("/notificacoes", clinica())).contains("Nova solicitação de consulta").contains("Bento");
        assertThat(html("/", clinica())).contains("class=\"contador\"");

        long id = db.queryForObject("SELECT ID_CONSULTA FROM TB_CONSULTA WHERE LOCAL_CONSULTA=?", Long.class,
                LOCAL_TESTE);
        mvc.perform(post("/consultas/{id}/confirmar", id).with(clinica()).with(csrf()))
                .andExpect(flash().attributeExists("sucesso"));

        long tutorId = db.queryForObject("SELECT ID_USUARIO FROM TB_USUARIO WHERE EMAIL=?", Long.class, TUTOR);
        assertThat(db.queryForObject(
                "SELECT COUNT(*) FROM TB_FV_NOTIFICACAO WHERE TIPO='CONSULTA_CONFIRMADA' AND ID_USUARIO=?",
                Integer.class, tutorId)).isEqualTo(1);
        assertThat(html("/notificacoes?lang=en", tutor())).contains("Appointment confirmed");

        mvc.perform(post("/notificacoes/lidas").with(tutor()).with(csrf())).andExpect(redirectedUrl("/notificacoes"));
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_FV_NOTIFICACAO WHERE ID_USUARIO=? AND LIDA=0",
                Integer.class, tutorId)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_FV_NOTIFICACAO WHERE ID_USUARIO IS NULL AND LIDA=0",
                Integer.class)).as("marcar como lido não afeta os avisos da clínica").isEqualTo(1);
    }

    @Test
    void operacaoRecusadaNaoGeraAviso() throws Exception {
        String domingo = LocalDate.now(clock).plusDays(7).with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).toString();
        mvc.perform(post("/consultas/salvar").with(tutor()).with(csrf()).param("animalId", "1")
                .param("tipo", "Retorno").param("data", domingo).param("hora", "10:00").param("local", LOCAL_TESTE))
                .andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_FV_NOTIFICACAO", Integer.class)).isZero();
    }

    @Test
    void consumidorDaFilaRegistraNotificacao() {
        consumidor.receber(new EventoFuturevet(TipoEvento.VACINA_REGISTRADA, null, "Luna", "V4 Felina",
                LocalDate.now(clock), LocalDateTime.now(clock)));
        assertThat(db.queryForObject("SELECT NOME_ANIMAL FROM TB_FV_NOTIFICACAO WHERE TIPO='VACINA_REGISTRADA'",
                String.class)).isEqualTo("Luna");
    }

    @Test
    void assistenteEnviaContextoDoTutorEExibeResposta() throws Exception {
        when(assistente.perguntar(any())).thenReturn(new RespostaAssistente("A V10 do Bento está atrasada.", "teste"));

        mvc.perform(post("/assistente").with(tutor()).with(csrf()).param("pergunta", "Alguma vacina atrasada?"))
                .andExpect(redirectedUrl("/assistente"));

        ArgumentCaptor<PerguntaAssistente> enviada = ArgumentCaptor.forClass(PerguntaAssistente.class);
        verify(assistente).perguntar(enviada.capture());
        assertThat(enviada.getValue().pergunta()).isEqualTo("Alguma vacina atrasada?");
        assertThat(enviada.getValue().idioma()).isEqualTo("pt-BR");
        assertThat(enviada.getValue().contexto()).contains("Bento").contains("Luna").contains("V10")
                .contains("ATRASADA").doesNotContain("Outro Tutor");
    }

    @Test
    void segundaPerguntaLevaAConversaRecenteNoContexto() throws Exception {
        when(assistente.perguntar(any())).thenReturn(new RespostaAssistente("A V10 está atrasada.", "teste"));
        var sessao = new org.springframework.mock.web.MockHttpSession();
        mvc.perform(post("/assistente").session(sessao).with(tutor()).with(csrf()).param("pergunta", "Tem vacina atrasada?"));
        mvc.perform(post("/assistente").session(sessao).with(tutor()).with(csrf()).param("pergunta", "E a do Bento?"));

        ArgumentCaptor<PerguntaAssistente> enviadas = ArgumentCaptor.forClass(PerguntaAssistente.class);
        verify(assistente, org.mockito.Mockito.times(2)).perguntar(enviadas.capture());
        assertThat(enviadas.getAllValues().get(0).contexto()).doesNotContain("Conversa recente");
        assertThat(enviadas.getAllValues().get(1).contexto()).contains("Conversa recente")
                .contains("Usuário: Tem vacina atrasada?").contains("Assistente: A V10 está atrasada.");
        assertThat(html("/assistente", tutor())).doesNotContain("E a do Bento?");
    }

    @Test
    void assistenteIndisponivelMostraMensagemAmigavel() throws Exception {
        Request requisicao = Request.create(Request.HttpMethod.POST, "/ia/assistente", Map.of(), null,
                StandardCharsets.UTF_8, null);
        when(assistente.perguntar(any())).thenThrow(new FeignException.ServiceUnavailable("sem chave", requisicao,
                null, Map.of()));
        mvc.perform(post("/assistente").with(tutor()).with(csrf()).param("pergunta", "Oi"))
                .andExpect(redirectedUrl("/assistente"))
                .andExpect(flash().attribute("erro",
                        "O Assistente Vet ainda não foi configurado (falta a chave da API de IA)."));
    }

    @Test
    void apiRespondeServicoIndisponivelQuandoIaCai() throws Exception {
        when(assistente.perguntar(any())).thenThrow(new IllegalStateException("conexão recusada"));
        mvc.perform(post("/api/assistente").with(tutor()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"Oi\"}")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value(
                        "O Assistente Vet está indisponível no momento. Tente novamente em instantes."));
    }

    @Test
    void perguntaVaziaNaoChamaMicrosservico() throws Exception {
        mvc.perform(post("/assistente").with(tutor()).with(csrf()).param("pergunta", "  "))
                .andExpect(status().isOk());
        org.mockito.Mockito.verifyNoInteractions(assistente);
    }

    @Test
    void healthEhPublicoEMetricasSaoDaClinica() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.nome").value("FutureVet"));
        mvc.perform(get("/actuator/metrics")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/actuator/metrics").with(tutor())).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(clinica())).andExpect(status().isOk());
    }

    @Test
    void painelDeMonitoramentoMostraBancoOperandoEIaForaDoAr() throws Exception {
        String corpo = html("/admin/monitoramento", clinica());
        assertThat(corpo).contains("Banco de dados").contains("Operando").contains("Fora do ar")
                .contains("Modo local");
    }

    @Test
    void apiDeNotificacoesEAssistenteExigemTokenEFuncionamComJwt() throws Exception {
        mvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TUTOR + "\",\"senha\":\"Futurevet123!\"}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + json.readTree(login).get("token").asText();

        mvc.perform(get("/api/notificacoes/nao-lidas").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        when(assistente.perguntar(any())).thenReturn(new RespostaAssistente("Olá!", "teste"));
        mvc.perform(post("/api/assistente").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"Oi\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Olá!"));
        mvc.perform(post("/api/assistente").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void apiMantemContratoDoAppMobile() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TUTOR + "\",\"senha\":\"Futurevet123!\"}")).andReturn().getResponse()
                .getContentAsString();
        String token = "Bearer " + json.readTree(login).get("token").asText();
        mvc.perform(get("/api/consultas").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AGENDADA"));
        mvc.perform(get("/api/vacinas").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").exists()).andExpect(jsonPath("$[0].descricao").exists())
                .andExpect(jsonPath("$[0].vacina.proximaDose").exists()).andExpect(jsonPath("$[0].dias").exists());
        mvc.perform(get("/api/animais").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").exists()).andExpect(jsonPath("$[0].especie").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TUTOR + "\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }
}
