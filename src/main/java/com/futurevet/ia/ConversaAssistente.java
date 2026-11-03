package com.futurevet.ia;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class ConversaAssistente implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int LIMITE_TROCAS = 10;

    public record Troca(String pergunta, String resposta) implements Serializable {
    }

    private final List<Troca> trocas = new ArrayList<>();

    public synchronized void registrar(String pergunta, String resposta) {
        trocas.add(new Troca(pergunta, resposta));
        if (trocas.size() > LIMITE_TROCAS)
            trocas.remove(0);
    }

    public synchronized List<Troca> trocas() {
        return Collections.unmodifiableList(new ArrayList<>(trocas));
    }

    public synchronized void limpar() {
        trocas.clear();
    }
}
