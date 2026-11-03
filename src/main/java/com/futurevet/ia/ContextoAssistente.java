package com.futurevet.ia;

import com.futurevet.model.Animal;
import com.futurevet.model.Consulta;
import com.futurevet.model.Usuario;
import com.futurevet.service.AgendaService;
import com.futurevet.service.AnimalService;
import com.futurevet.service.CarteiraService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Monta, a partir dos dados que o usuário logado pode ver, o resumo enviado à IA. Assim o assistente
 * responde sobre os animais, as vacinas e as consultas reais — e nunca sobre dados de outro tutor.
 */
@Component
public class ContextoAssistente {
    private static final int LIMITE_ITENS = 15;
    private static final int TROCAS_NO_CONTEXTO = 3;
    private static final int LIMITE_TEXTO_TROCA = 300;

    private final AnimalService animais;
    private final CarteiraService carteira;
    private final AgendaService agenda;
    private final Clock clock;

    public ContextoAssistente(AnimalService animais, CarteiraService carteira, AgendaService agenda, Clock clock) {
        this.animais = animais;
        this.carteira = carteira;
        this.agenda = agenda;
        this.clock = clock;
    }

    public String montar(Usuario usuario) {
        return montar(usuario, List.of());
    }

    public String montar(Usuario usuario, List<ConversaAssistente.Troca> historico) {
        LocalDate hoje = LocalDate.now(clock);
        StringBuilder sb = new StringBuilder();
        sb.append("Data de hoje: ").append(hoje).append('\n');
        sb.append("Perfil de quem pergunta: ")
                .append(usuario.admin() ? "equipe da clínica veterinária" : "tutor de animais").append('\n');

        sb.append("\nAnimais:\n");
        List<Animal> pets = animais.listar();
        pets.stream().limit(LIMITE_ITENS).forEach(a -> sb.append("- ").append(descrever(a)).append('\n'));
        if (pets.isEmpty())
            sb.append("- nenhum animal cadastrado\n");

        sb.append("\nCarteira de vacinação (situação calculada pelo sistema):\n");
        var doses = carteira.listar().stream().filter(i -> !i.vacina().substituida())
                .limit(LIMITE_ITENS).toList();
        doses.forEach(i -> sb.append("- ").append(i.vacina().animal()).append(": ").append(i.vacina().nome())
                .append(", aplicada em ").append(i.vacina().data()).append(", próxima dose ")
                .append(i.vacina().proximaDose() == null ? "não informada" : i.vacina().proximaDose())
                .append(", situação ").append(i.situacao()).append('\n'));
        if (doses.isEmpty())
            sb.append("- nenhuma vacina registrada\n");

        sb.append("\nConsultas futuras:\n");
        List<Consulta> futuras = agenda.listar().stream().filter(c -> c.ativa() && !c.data().isBefore(hoje))
                .limit(LIMITE_ITENS).toList();
        futuras.forEach(c -> sb.append("- ").append(c.animal()).append(": ").append(c.tipo()).append(" em ")
                .append(c.data()).append(' ').append(c.hora() == null ? "" : c.hora()).append(", situação ")
                .append(c.status()).append('\n'));
        if (futuras.isEmpty())
            sb.append("- nenhuma consulta agendada\n");

        if (!historico.isEmpty()) {
            sb.append("\nConversa recente (mais antiga primeiro):\n");
            historico.stream().skip(Math.max(0, historico.size() - TROCAS_NO_CONTEXTO))
                    .forEach(t -> sb.append("- Usuário: ").append(resumir(t.pergunta())).append("\n- Assistente: ")
                            .append(resumir(t.resposta())).append('\n'));
        }
        return sb.toString();
    }

    private static String resumir(String texto) {
        String limpo = texto.replace('\n', ' ').strip();
        return limpo.length() <= LIMITE_TEXTO_TROCA ? limpo : limpo.substring(0, LIMITE_TEXTO_TROCA) + "…";
    }

    private static String descrever(Animal a) {
        StringBuilder sb = new StringBuilder(a.nome()).append(" (espécie ").append(a.especie());
        if (a.raca() != null)
            sb.append(", raça ").append(a.raca());
        if (a.idade() != null)
            sb.append(", idade ").append(a.idade());
        if (a.porte() != null)
            sb.append(", porte ").append(a.porte());
        if (a.peso() != null)
            sb.append(", ").append(a.peso()).append(" kg");
        return sb.append(')').toString();
    }
}
