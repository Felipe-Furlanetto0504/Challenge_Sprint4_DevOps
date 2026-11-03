package com.futurevet.service;

import com.futurevet.model.Animal;
import com.futurevet.model.Consulta;
import com.futurevet.model.StatusConsulta;
import com.futurevet.model.TipoEvento;
import com.futurevet.repository.ConsultaRepository;
import com.futurevet.security.Acesso;
import com.futurevet.web.forms.AgendaForm;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo de agendamento: o tutor solicita, a clínica confirma e conclui. Toda alteração bloqueia a
 * agenda (SELECT ... FOR UPDATE) para que dois pedidos simultâneos não ocupem o mesmo horário.
 */
@Service
public class AgendaService {
    private static final LocalTime ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    private static final int LIMITE_TEXTO = 255;

    private final ConsultaRepository repo;
    private final AnimalService animais;
    private final Acesso acesso;
    private final PublicadorEventos eventos;
    private final Clock clock;

    public AgendaService(ConsultaRepository repo, AnimalService animais, Acesso acesso, PublicadorEventos eventos,
            Clock clock) {
        this.repo = repo;
        this.animais = animais;
        this.acesso = acesso;
        this.eventos = eventos;
        this.clock = clock;
    }

    public List<Consulta> listar() {
        return repo.listar(acesso.atual());
    }

    public Consulta buscar(long id) {
        var c = repo.buscar(id).orElseThrow(() -> new RegraException("erro.consulta.nao-encontrada"));
        acesso.dono(c.usuarioId());
        return c;
    }

    @Transactional
    public long agendar(AgendaForm f) {
        repo.bloquearAgenda();
        Animal animal = validar(f, -1);
        long id = repo.criar(f);
        publicar(TipoEvento.CONSULTA_SOLICITADA, animal.usuarioId(), animal.nome(), f);
        return id;
    }

    @Transactional
    public void reagendar(long id, AgendaForm f) {
        repo.bloquearAgenda();
        var c = buscar(id);
        if (!c.ativa())
            throw new RegraException("erro.consulta.encerrada-reagendar");
        f.setAnimalId(c.animalId());
        validar(f, id);
        repo.reagendar(id, f);
        publicar(TipoEvento.CONSULTA_REAGENDADA, c.usuarioId(), c.animal(), f);
    }

    @Transactional
    public void confirmar(long id) {
        acesso.admin();
        repo.bloquearAgenda();
        var c = buscar(id);
        if (!c.aguardandoConfirmacao())
            throw new RegraException("erro.consulta.confirmar-somente-agendada");
        if (passada(c))
            throw new RegraException("erro.consulta.confirmar-passada");
        repo.status(id, StatusConsulta.CONFIRMADA, c.observacao());
        publicar(TipoEvento.CONSULTA_CONFIRMADA, c);
    }

    @Transactional
    public void cancelar(long id, String motivo) {
        repo.bloquearAgenda();
        var c = buscar(id);
        if (!c.ativa())
            throw new RegraException("erro.consulta.encerrada");
        repo.status(id, StatusConsulta.CANCELADA, textoObrigatorio(motivo, "erro.consulta.motivo"));
        publicar(TipoEvento.CONSULTA_CANCELADA, c);
    }

    @Transactional
    public void concluir(long id, String observacao) {
        acesso.admin();
        repo.bloquearAgenda();
        var c = buscar(id);
        if (!c.confirmada())
            throw new RegraException("erro.consulta.concluir-sem-confirmar");
        if (c.hora() == null || !passada(c))
            throw new RegraException("erro.consulta.concluir-antes-horario");
        repo.status(id, StatusConsulta.REALIZADA, textoObrigatorio(observacao, "erro.consulta.resumo"));
        publicar(TipoEvento.CONSULTA_REALIZADA, c);
    }

    private Animal validar(AgendaForm f, long ignorar) {
        Animal animal = animais.buscar(f.getAnimalId());
        f.setLocal(f.getLocal().strip());
        LocalTime hora = f.getHora();
        if (hora.getSecond() != 0 || hora.getNano() != 0)
            throw new RegraException("erro.consulta.horario-minutos");
        if (!LocalDateTime.of(f.getData(), hora).isAfter(LocalDateTime.now(clock)))
            throw new RegraException("erro.consulta.data-futura");
        if (f.getData().getDayOfWeek() == DayOfWeek.SUNDAY || hora.isBefore(ABERTURA) || hora.isAfter(FECHAMENTO))
            throw new RegraException("erro.consulta.horario-funcionamento");
        if (repo.conflito(f.getAnimalId(), f.getData(), hora.toString(), f.getLocal(), ignorar))
            throw new RegraException("erro.consulta.conflito");
        return animal;
    }

    private static String textoObrigatorio(String texto, String codigoErro) {
        if (texto == null || texto.isBlank() || texto.length() > LIMITE_TEXTO)
            throw new RegraException(codigoErro, LIMITE_TEXTO);
        return texto.strip();
    }

    private boolean passada(Consulta c) {
        return c.hora() != null
                && LocalDateTime.of(c.data(), LocalTime.parse(c.hora())).isBefore(LocalDateTime.now(clock));
    }

    private void publicar(TipoEvento tipo, long tutor, String animal, AgendaForm f) {
        eventos.publicar(tipo, tutor, animal, f.getHora().toString(), f.getData());
    }

    private void publicar(TipoEvento tipo, Consulta c) {
        eventos.publicar(tipo, c.usuarioId(), c.animal(), c.hora(), c.data());
    }
}
