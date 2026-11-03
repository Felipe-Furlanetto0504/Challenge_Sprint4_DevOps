# Guia para entender e explicar o projeto

O navegador envia um formulário com token CSRF. Spring Security verifica sessão e permissão de rota. O controller valida o formulário e chama o serviço. O serviço verifica o proprietário e aplica regras dentro de uma transação. O repository executa SQL com parâmetros e devolve records. Thymeleaf renderiza HTML com escape de texto.

Foi escolhido Spring JDBC, porque o esquema Oracle legado tem nomes e tipos bem definidos; o PDF não exige JPA. Não há concatenação de valores vindos do usuário em SQL. A única composição de identificador é a seleção interna de sequence em Ids, limitada a uma lista fixa de nomes.

## Sprint 4 — o que foi adicionado

- **Internacionalização:** `InternacionalizacaoConfig` registra o `CookieLocaleResolver` e o `LocaleChangeInterceptor` (`?lang=`). Os textos ficam em `mensagens.properties` e `mensagens_en.properties`. As regras de negócio lançam `RegraException` com um **código** (ex.: `erro.consulta.conflito`), e o texto é resolvido no idioma do usuário por `Mensagens`.
- **Mensageria:** os services chamam `PublicadorEventos`, que publica um `EventoFuturevet` no contexto do Spring. `ProdutorEventos` escuta com `@TransactionalEventListener` (só depois do commit) e envia ao exchange `futurevet.eventos`. `ConsumidorNotificacoes` (`@RabbitListener`) grava o aviso em `TB_FV_NOTIFICACAO`. Sem broker, o produtor entrega direto ao consumidor.
- **Microsserviço de IA:** `AssistenteService` monta a pergunta com `ContextoAssistente` (dados que o usuário logado pode ver + últimas trocas da conversa) e chama `AssistenteClient` (OpenFeign). O nome `futurevet-ia` é resolvido pelo Eureka ou, com `IA_URL`, por endereço fixo. O microsserviço usa o `ChatClient` do Spring AI.
- **Monitoramento:** `MonitoramentoService` lê as métricas do Micrometer (`jvm.memory.*`, `process.cpu.usage`, `process.uptime`, `http.server.requests`) e o `HealthEndpoint` do Actuator. A saúde da IA é consultada com `RestClient`.

## Pontos para a avaliação oral

1. Explique por que `usuarioId` não vem do formulário de cadastro de animal: ele vem da sessão e impede criar dados em nome de outro tutor.
2. Mostre `Acesso.dono`: conhecer um ID não é permissão para acessar aquele registro.
3. Mostre `AgendaService.agendar` e a transação com `bloquearAgenda`: sem a trava, dois pedidos simultâneos poderiam passar pela consulta de conflito antes de gravar.
4. Explique a diferença entre agendar, confirmar e concluir. O tutor não pode confirmar/concluir; quem reagenda volta a aguardar confirmação.
5. Mostre `CarteiraService.classificar`: NULL significa informação ausente, e não vacina em dia. O intervalo do reforço é informado conforme orientação veterinária, não recomendado pelo software.
6. Explique `ID_DOSE_ORIGEM`: mantém a cadeia e a constraint única bloqueia dois reforços para a mesma dose.
7. Mostre as migrations: V1 representa o banco original; V2 amplia o modelo; V3 converte senhas sem enviar dados a serviços externos. A conversão acontece na aplicação, com BCrypt, e não com um truncamento SQL.
8. Explique o baseline 1 apenas para um schema legado já existente e o motivo de não apagar o histórico anterior.
9. Explique a restrição de exclusão de animais com histórico e de doses encadeadas.
10. Explique por que o aviso é publicado com `@TransactionalEventListener`: se o agendamento falhar, nenhuma notificação falsa sai.
11. Mostre `ContextoAssistente`: a IA só recebe dados que o usuário logado pode ver.
12. Mostre como uma `RegraException` vira texto em português ou inglês (`Mensagens`, `Feedback`, `ApiExceptionHandler`).
13. Leia os testes e justifique quais erros cada um previne. Os testes locais usam H2 em modo Oracle, que não substitui testar o Oracle real.

O código foi elaborado com auxílio de IA. Antes da avaliação, execute, leia e explique com suas palavras cada fluxo; a existência dos arquivos não substitui o entendimento individual exigido no enunciado.
