package com.futurevet.service;

import com.futurevet.mensageria.EventoFuturevet;
import com.futurevet.model.Notificacao;
import com.futurevet.repository.NotificacaoRepository;
import com.futurevet.security.Acesso;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {
    private final NotificacaoRepository repo;
    private final Acesso acesso;

    public NotificacaoService(NotificacaoRepository repo, Acesso acesso) {
        this.repo = repo;
        this.acesso = acesso;
    }

    public List<Notificacao> listar() {
        return repo.listar(acesso.atual());
    }

    public long naoLidas() {
        return repo.naoLidas(acesso.atual());
    }

    @Transactional
    public void marcarTodasLidas() {
        repo.marcarTodasLidas(acesso.atual());
    }

    /**
     * Chamado pelo consumidor da fila, inclusive depois do commit da transação que gerou o evento —
     * por isso abre uma transação própria.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoFuturevet evento) {
        repo.criar(evento);
    }
}
