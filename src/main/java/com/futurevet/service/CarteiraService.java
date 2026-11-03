package com.futurevet.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.futurevet.model.Animal;
import com.futurevet.model.SituacaoVacina;
import com.futurevet.model.TipoEvento;
import com.futurevet.model.Vacina;
import com.futurevet.repository.VacinaRepository;
import com.futurevet.security.Acesso;
import com.futurevet.web.forms.ReforcoForm;
import com.futurevet.web.forms.VacinaForm;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carteira de vacinação: classifica cada dose pela data prevista da próxima aplicação e encadeia os
 * reforços, preservando a dose anterior como histórico.
 */
@Service
public class CarteiraService {
    /** Doses previstas para os próximos dias entram como "próxima dose". */
    static final int JANELA_PROXIMA_DOSE_DIAS = 30;

    public record Item(Vacina vacina, SituacaoVacina situacao, Long dias) {
        /** Mantém o campo "descricao" no JSON consumido pelo app mobile. */
        @JsonProperty("descricao")
        public String descricao() {
            return situacao.descricao();
        }
    }

    private final VacinaRepository repo;
    private final AnimalService animais;
    private final Acesso acesso;
    private final PublicadorEventos eventos;
    private final Clock clock;

    public CarteiraService(VacinaRepository repo, AnimalService animais, Acesso acesso, PublicadorEventos eventos,
            Clock clock) {
        this.repo = repo;
        this.animais = animais;
        this.acesso = acesso;
        this.eventos = eventos;
        this.clock = clock;
    }

    public List<Item> listar() {
        return repo.listar(acesso.atual()).stream().map(this::classificar).toList();
    }

    public Item classificar(Vacina v) {
        if (v.substituida())
            return new Item(v, SituacaoVacina.HISTORICO, null);
        if (v.proximaDose() == null)
            return new Item(v, SituacaoVacina.SEM_PREVISAO, null);
        long dias = ChronoUnit.DAYS.between(LocalDate.now(clock), v.proximaDose());
        SituacaoVacina situacao = dias < 0 ? SituacaoVacina.ATRASADA
                : dias <= JANELA_PROXIMA_DOSE_DIAS ? SituacaoVacina.PROXIMA : SituacaoVacina.EM_DIA;
        return new Item(v, situacao, dias);
    }

    public Vacina buscar(long id) {
        var v = repo.buscar(id).orElseThrow(() -> new RegraException("erro.vacina.nao-encontrada"));
        acesso.dono(v.usuarioId());
        return v;
    }

    @Transactional
    public long salvar(Long id, VacinaForm f) {
        if (id == null) {
            var animal = validar(f, -1);
            long novo = repo.criar(f, null);
            eventos.publicar(TipoEvento.VACINA_REGISTRADA, animal.usuarioId(), animal.nome(), f.getNome(),
                    f.getData());
            return novo;
        }
        var v = buscar(id);
        if (v.vinculadaAReforco())
            throw new RegraException("erro.vacina.vinculada-editar");
        f.setAnimalId(v.animalId());
        validar(f, id);
        repo.atualizar(id, f);
        return id;
    }

    @Transactional
    public long reforcar(long id, ReforcoForm f) {
        buscar(id);
        repo.bloquear(id);
        var v = buscar(id);
        if (v.substituida())
            throw new RegraException("erro.vacina.reforco-existente");
        if (!f.getData().isAfter(v.data()))
            throw new RegraException("erro.vacina.reforco-data");
        VacinaForm dose = new VacinaForm();
        dose.setAnimalId(v.animalId());
        dose.setNome(v.nome());
        dose.setData(f.getData());
        dose.setProximaDose(f.getData().plusMonths(f.getIntervalo()));
        dose.setLocal(f.getLocal());
        validar(dose, -1);
        long novo = repo.criar(dose, id);
        eventos.publicar(TipoEvento.REFORCO_REGISTRADO, v.usuarioId(), v.animal(), v.nome(), f.getData());
        return novo;
    }

    @Transactional
    public void excluir(long id) {
        if (buscar(id).vinculadaAReforco())
            throw new RegraException("erro.vacina.vinculada-excluir");
        repo.excluir(id);
    }

    private Animal validar(VacinaForm f, long ignorar) {
        var animal = animais.buscar(f.getAnimalId());
        if (f.getData().isAfter(LocalDate.now(clock)))
            throw new RegraException("erro.vacina.futura");
        if (f.getProximaDose() != null && !f.getProximaDose().isAfter(f.getData()))
            throw new RegraException("erro.vacina.proxima-dose");
        if (repo.duplicada(f, ignorar))
            throw new RegraException("erro.vacina.duplicada");
        return animal;
    }
}
