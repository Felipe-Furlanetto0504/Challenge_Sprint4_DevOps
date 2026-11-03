package com.futurevet;

import com.futurevet.model.SituacaoVacina;
import com.futurevet.model.StatusConsulta;
import com.futurevet.repository.*;
import com.futurevet.service.*;
import com.futurevet.web.forms.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class FluxosTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate db;
    @Autowired
    AgendaService agenda;
    @Autowired
    CarteiraService carteira;
    @Autowired
    AnimalService animais;
    @Autowired
    UsuarioRepository users;
    @Autowired
    Clock clock;

    private AgendaForm agendamento() {
        AgendaForm f = new AgendaForm();
        f.setAnimalId(1L);
        f.setTipo("Check-up");
        LocalDate data = LocalDate.now(clock).plusDays(7);
        while (data.getDayOfWeek() == DayOfWeek.SUNDAY)
            data = data.plusDays(1);
        f.setData(data);
        f.setHora(LocalTime.of(11, 30));
        f.setLocal("Sala de testes");
        return f;
    }

    @Test
    void loginRealComSenhaCorretaEIncorreta() throws Exception {
        mvc.perform(
                formLogin("/login").user("email", "tutor@futurevet.local").password("senha", "Futurevet123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(formLogin("/login").user("email", "tutor@futurevet.local").password("senha", "errada"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void visitanteNaoAbreAreaPrivadaEPostSemCsrfFalha() throws Exception {
        mvc.perform(get("/animais")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/registro")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void todasAsTelasDoTutorRenderizam() throws Exception {
        for (String url : new String[] { "/", "/animais", "/animais/novo", "/animais/1/editar", "/consultas",
                "/consultas/nova", "/consultas/1/reagendar", "/vacinas", "/vacinas/nova", "/vacinas/1/editar",
                "/vacinas/1/reforco", "/notificacoes", "/assistente" })
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @WithMockUser(username = "clinica@futurevet.local", roles = "ADMIN")
    void telasDaClinicaRenderizam() throws Exception {
        for (String url : new String[] { "/", "/animais", "/consultas", "/vacinas", "/admin/usuarios",
                "/admin/monitoramento", "/notificacoes", "/assistente" })
            mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void telasPublicasRenderizam() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/registro")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "outro@futurevet.local", roles = "TUTOR")
    void propriedadeEhVerificadaNasLeiturasEEscritas() throws Exception {
        assertThat(animais.listar()).isEmpty();
        assertThatThrownBy(() -> animais.buscar(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> agenda.cancelar(1, "teste")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> carteira.excluir(1)).isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/animais/1/editar")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/monitoramento")).andExpect(status().isForbidden());
        mvc.perform(post("/animais/1/excluir").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void agendamentoBloqueiaConflitoECancelamentoLiberaHorario() {
        AgendaForm f = agendamento();
        long id = agenda.agendar(f);
        assertThat(agenda.buscar(id).status()).isEqualTo(StatusConsulta.AGENDADA);
        assertThatThrownBy(() -> agenda.agendar(f)).isInstanceOf(RegraException.class);
        agenda.cancelar(id, "Mudança de planos");
        assertThat(agenda.buscar(id).status()).isEqualTo(StatusConsulta.CANCELADA);
        assertThat(agenda.agendar(f)).isPositive();
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void tutorNaoConfirmaNemConclui() {
        long id = agenda.agendar(agendamento());
        assertThatThrownBy(() -> agenda.confirmar(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> agenda.concluir(id, "Atendido")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "clinica@futurevet.local", roles = "ADMIN")
    void fluxoDaClinicaConfirmaEConcluiSomenteAposHorario() {
        long id = agenda.agendar(agendamento());
        agenda.confirmar(id);
        assertThat(agenda.buscar(id).status()).isEqualTo(StatusConsulta.CONFIRMADA);
        assertThatThrownBy(() -> agenda.concluir(id, "Tudo bem")).isInstanceOf(RegraException.class);
        db.update("UPDATE TB_CONSULTA SET DATA_CONSULTA=? WHERE ID_CONSULTA=?",
                LocalDate.now(clock).minusDays(1), id);
        agenda.concluir(id, "Atendimento concluído");
        assertThat(agenda.buscar(id).status()).isEqualTo(StatusConsulta.REALIZADA);
        assertThatThrownBy(() -> agenda.cancelar(id, "teste")).isInstanceOf(RegraException.class);
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void reagendamentoVoltaAguardarConfirmacao() {
        AgendaForm f = agendamento();
        long id = agenda.agendar(f);
        db.update("UPDATE TB_CONSULTA SET STATUS='CONFIRMADA' WHERE ID_CONSULTA=?", id);
        f.setHora(LocalTime.of(12, 0));
        agenda.reagendar(id, f);
        assertThat(agenda.buscar(id).hora()).isEqualTo("12:00");
        assertThat(agenda.buscar(id).status()).isEqualTo(StatusConsulta.AGENDADA);
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void passadoDomingoESegundosSaoRecusados() {
        AgendaForm f = agendamento();
        f.setData(LocalDate.now(clock).minusDays(1));
        assertThatThrownBy(() -> agenda.agendar(f)).isInstanceOf(RegraException.class);
        f.setData(LocalDate.now(clock).plusWeeks(1)
                .with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SUNDAY)));
        assertThatThrownBy(() -> agenda.agendar(f)).isInstanceOf(RegraException.class);
        f.setData(agendamento().getData());
        f.setHora(LocalTime.of(11, 0, 30));
        assertThatThrownBy(() -> agenda.agendar(f)).isInstanceOf(RegraException.class);
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void reforcoPreservaHistoricoEImpedeRepeticao() {
        ReforcoForm f = new ReforcoForm();
        f.setData(LocalDate.now(clock));
        f.setIntervalo(12);
        f.setLocal("Clínica");
        long novo = carteira.reforcar(1, f);
        assertThat(carteira.buscar(novo).proximaDose()).isEqualTo(LocalDate.now(clock).plusMonths(12));
        assertThat(carteira.classificar(carteira.buscar(1)).situacao()).isEqualTo(SituacaoVacina.HISTORICO);
        assertThatThrownBy(() -> carteira.reforcar(1, f)).isInstanceOf(RegraException.class);
        assertThatThrownBy(() -> carteira.excluir(1)).isInstanceOf(RegraException.class);
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void doseSemProximaDataNaoInventaPrazo() {
        var item = carteira.classificar(carteira.buscar(3));
        assertThat(item.situacao()).isEqualTo(SituacaoVacina.SEM_PREVISAO);
        assertThat(item.dias()).isNull();
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void crudAnimalAceitaIdadeTextualEDecimalEImpedeExcluirHistorico() throws Exception {
        mvc.perform(post("/animais/salvar").with(csrf()).param("nome", "Novo pet").param("especie", "GATO")
                .param("idade", "6 meses").param("peso", "3.25").param("porte", "PEQUENO"))
                .andExpect(status().is3xxRedirection());
        long id = db.queryForObject("SELECT ID_ANIMAL FROM TB_ANIMAL WHERE NOME_ANIMAL='Novo pet'",
                Long.class);
        assertThat(
                db.queryForObject("SELECT IDADE_ANIMAL FROM TB_ANIMAL WHERE ID_ANIMAL=?", String.class, id))
                .isEqualTo("6 meses");
        assertThat(db.queryForObject("SELECT PESO_ANIMAL FROM TB_ANIMAL WHERE ID_ANIMAL=?",
                java.math.BigDecimal.class, id)).isEqualByComparingTo("3.25");
        mvc.perform(post("/animais/salvar").with(user("tutor@futurevet.local").roles("TUTOR")).with(csrf())
                .param("id", String.valueOf(id)).param("nome", "Atualizado").param("especie", "GATO"))
                .andExpect(status().is3xxRedirection());
        assertThat(db.queryForObject("SELECT NOME_ANIMAL FROM TB_ANIMAL WHERE ID_ANIMAL=?", String.class, id))
                .isEqualTo("Atualizado");
        mvc.perform(post("/animais/{id}/excluir", id).with(user("tutor@futurevet.local").roles("TUTOR"))
                .with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_ANIMAL WHERE ID_ANIMAL=?", Integer.class, id))
                .isZero();
        mvc.perform(
                post("/animais/1/excluir").with(user("tutor@futurevet.local").roles("TUTOR")).with(csrf()))
                .andExpect(flash().attributeExists("erro"));
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_ANIMAL WHERE ID_ANIMAL=1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void formularioInvalidoMantemPaginaComErros() throws Exception {
        mvc.perform(post("/animais/salvar").with(csrf()).param("nome", "x".repeat(31))
                .param("especie", "INVALIDA").param("peso", "9999")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome", "especie", "peso"));
    }

    @Test
    void cadastroNormalizaCpfETelefoneESalvaHash() throws Exception {
        mvc.perform(post("/registro").with(csrf()).param("nome", "Novo tutor")
                .param("email", "novo@example.com").param("senha", "SenhaTeste123!")
                .param("confirmacao", "SenhaTeste123!").param("cpf", "012.345.678-90")
                .param("telefone", "(11) 99888-7777").param("role", "ADMIN"))
                .andExpect(redirectedUrl("/login?cadastro"));
        var u = users.email("novo@example.com").orElseThrow();
        assertThat(u.cpf()).isEqualTo(1234567890L);
        assertThat(u.telefone()).isEqualTo(11998887777L);
        assertThat(u.role()).isEqualTo("TUTOR");
        assertThat(u.senha()).startsWith("$2a$");
    }

    @Test
    @WithMockUser(username = "tutor@futurevet.local", roles = "TUTOR")
    void crudVacinaPreservaCamposOpcionais() throws Exception {
        String ontem = LocalDate.now(clock).minusDays(1).toString();
        mvc.perform(post("/vacinas/salvar").with(csrf()).param("animalId", "1")
                .param("nome", "Vacina de teste").param("data", ontem)).andExpect(redirectedUrl("/vacinas"));
        long id = db.queryForObject("SELECT ID_VACINA FROM TB_VACINA WHERE NOME_VACINA='Vacina de teste'",
                Long.class);
        assertThat(db.queryForObject("SELECT PROX_DOSE FROM TB_VACINA WHERE ID_VACINA=?", java.sql.Date.class,
                id)).isNull();
        mvc.perform(post("/vacinas/salvar").with(user("tutor@futurevet.local").roles("TUTOR")).with(csrf())
                .param("id", String.valueOf(id)).param("animalId", "1").param("nome", "Vacina atualizada")
                .param("data", ontem).param("local", "Clínica teste")).andExpect(redirectedUrl("/vacinas"));
        assertThat(db.queryForObject("SELECT NOME_VACINA FROM TB_VACINA WHERE ID_VACINA=?", String.class, id))
                .isEqualTo("Vacina atualizada");
        mvc.perform(post("/vacinas/{id}/excluir", id).with(user("tutor@futurevet.local").roles("TUTOR"))
                .with(csrf())).andExpect(redirectedUrl("/vacinas"));
        assertThat(db.queryForObject("SELECT COUNT(*) FROM TB_VACINA WHERE ID_VACINA=?", Integer.class, id))
                .isZero();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void doisAgendamentosSimultaneosNaoOcupamMesmoHorario() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var inicio = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> tentativa = () -> {
            var contexto = org.springframework.security.core.context.SecurityContextHolder
                    .createEmptyContext();
            contexto.setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "tutor@futurevet.local", "",
                            java.util.List.of(
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                            "ROLE_TUTOR"))));
            org.springframework.security.core.context.SecurityContextHolder.setContext(contexto);
            try {
                inicio.await();
                AgendaForm f = agendamento();
                f.setLocal("Sala concorrência");
                f.setHora(LocalTime.of(16, 30));
                agenda.agendar(f);
                return true;
            } catch (RegraException e) {
                return false;
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        };
        try {
            var primeira = pool.submit(tentativa);
            var segunda = pool.submit(tentativa);
            inicio.countDown();
            int sucessos = (primeira.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0);
            assertThat(sucessos).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            db.update("DELETE FROM TB_CONSULTA WHERE LOCAL_CONSULTA='Sala concorrência'");
        }
    }
}
