# 🐾 FutureVet — Plataforma de Gestão Veterinária

> Challenge FIAP 2026 · 2º semestre · parceria **Clyvo Vet**
> **Java Advanced — Entrega Final (4ª Sprint)**

O FutureVet aproxima tutores e clínica no cuidado contínuo dos pets. Em um só lugar, o tutor agenda consultas, acompanha a carteira de vacinação e recebe avisos da clínica. A clínica confirma a agenda, registra os atendimentos e acompanha a saúde da própria plataforma.

---

## 🔗 Links da entrega

| Artefato | Link |
|---|---|
| 💻 Repositório GitHub | https://github.com/joaocaetano1310/Challenge_Sprint4_Java |
| 🌐 Aplicação online (deploy) | https://challenge-sprint3-java.onrender.com |
| 🤖 Microsserviço de IA (deploy) | https://futurevet-ia.onrender.com/actuator/health |
| 🎬 Vídeo da apresentação (até 15 min) | https://youtu.be/O-y-bdhvFW8 |
| 📱 App mobile (consome a API) | https://github.com/Felipe-Furlanetto0504/Challenge-FutureVet-Mobile |
| 📡 IoT (coleira PetLink) | https://github.com/Buenozw/CHALLENGE---IOT |

> ⚠️ No plano gratuito do Render, o primeiro acesso depois de um tempo parado pode levar até 60 segundos.

### Acesso de demonstração (perfil `demo`, local ou docker-compose)

| Perfil | E-mail | Senha |
|---|---|---|
| **ADMIN** (clínica) | clinica@futurevet.local | Futurevet123! |
| **TUTOR** | tutor@futurevet.local | Futurevet123! |
| **TUTOR** (outro tutor, para testar o isolamento) | outro@futurevet.local | Futurevet123! |

No perfil `oracle` (produção) não existem contas de demonstração. Crie a conta em `/registro` (veja [Banco de dados](#️-banco-de-dados)).

---

## 👥 Integrantes

| Nome | RM |
|------|----|
| João Victor Caetano Alves da Silva | RM562074 |
| João Victor Bueno Castelini da Silva | RM564115 |
| Ryan Vetoriano | RM565667 |
| Felipe Furlanetto | RM562766 |
| Raul Rezende Iemini Aguiar | RM564002 |

---

## 💡 Narrativa da solução

### O problema

Na rotina de uma clínica como a Clyvo Vet, a prevenção se perde em papel e mensagens soltas:

- vacinas atrasam porque ninguém lembra da próxima dose;
- horários são marcados em duplicidade;
- o tutor não sabe se a consulta foi confirmada;
- o histórico do animal fica espalhado.

### A proposta

Uma plataforma em que **cada pet tem uma jornada de saúde contínua**:

1. **Agenda com regras reais.** O tutor solicita, a clínica confirma e, depois do horário, registra o atendimento. O sistema impede conflito de horário, mesmo com dois pedidos simultâneos.
2. **Carteira de vacinação inteligente.** Cada dose é classificada como *atrasada*, *próxima* (até 30 dias), *em dia*, *sem previsão* ou *histórico*. O reforço calcula a próxima dose e preserva a dose anterior.
3. **Notificações por mensageria.** Toda ação relevante gera um evento no **RabbitMQ**. O outro lado é avisado: o tutor agenda e a clínica recebe; a clínica confirma e o tutor recebe.
4. **Assistente Vet (IA generativa).** Um chat responde dúvidas **com base nos dados reais do usuário** (animais, vacinas, consultas). Ele roda em um **microsserviço Spring AI separado**, chamado via **OpenFeign** e descoberto pelo **Eureka**.
5. **Internacionalização.** A interface inteira, inclusive mensagens de erro e avisos, funciona em português e inglês.
6. **Monitoramento.** A clínica vê a saúde da aplicação, do banco, da IA e da mensageria em uma tela alimentada pelo **Spring Boot Actuator**.

### O que torna a solução original

- **A IA conhece o pet.** O assistente não é um chat genérico. A aplicação monta um contexto só com os dados que *aquele* usuário pode ver. A pergunta "alguma vacina está atrasada?" é respondida com a situação calculada pelo sistema, e um tutor nunca recebe dados de outro.
- **O aviso não se perde.** Se o RabbitMQ estiver fora do ar, o produtor entrega o evento direto ao consumidor (*fallback*), e o painel de monitoramento mostra por qual canal cada evento passou.
- **Eventos só depois do commit.** As notificações são publicadas com `@TransactionalEventListener`. Se a operação falhar (ex.: horário em conflito), nenhum aviso falso é enviado.

---

## 🧭 Como os conceitos da disciplina foram aplicados

| Conceito (Java Advanced) | Onde está | Como aparece no app |
|---|---|---|
| **Spring MVC + Thymeleaf** | `web/`, `templates/` | 16 telas com layout em fragmentos, menu com página ativa e versão para celular |
| **Bean Validation** | `web/forms/` | Formulários com mensagens por campo e campo inválido destacado |
| **Spring Security (2 perfis)** | `security/SecurityConfig.java` | Sessão + formulário nas telas, JWT stateless na API; ADMIN × TUTOR; dono do registro verificado em `Acesso` |
| **Flyway** | `db/migration` (V1–V4) | V4 da Sprint 4 cria `TB_FV_NOTIFICACAO` |
| **Internacionalização (i18n)** | `internacionalizacao/`, `mensagens*.properties` | `?lang=pt-BR` / `?lang=en`, escolha lembrada em cookie; regras de negócio traduzidas |
| **Mensageria — RabbitMQ** | `mensageria/` | Exchange `futurevet.eventos` → fila `futurevet.notificacoes` → `@RabbitListener` gera o aviso |
| **Spring AI** | `microsservicos/futurevet-ia` | `ChatClient` com instruções de sistema + contexto do usuário |
| **Microsserviços + OpenFeign** | `ia/AssistenteClient.java` | `@FeignClient(name = "futurevet-ia")` chama o microsserviço de IA |
| **Eureka (service discovery)** | `microsservicos/servidor-eureka` | A aplicação encontra a IA pelo nome, sem IP fixo |
| **RestClient** | `monitoramento/MonitoramentoService.java` | Consulta o `/actuator/health` da IA para o painel |
| **Spring Boot Actuator** | `monitoramento/`, `/actuator/*` | Tela de monitoramento (memória, CPU, tempo no ar, requisições, saúde dos serviços) |
| **API REST + Swagger** | `web/api/`, `/swagger-ui/index.html` | Usada pelo app mobile, incluindo `/api/notificacoes` e `/api/assistente` |

---

## 🏗️ Arquitetura

```mermaid
flowchart LR
    subgraph Clientes
        W[Navegador<br/>Thymeleaf]
        M[App mobile<br/>React Native]
    end
    subgraph FutureVet["futurevet (Spring Boot · :8080)"]
        C[Controllers MVC<br/>e API REST + JWT]
        S[Services<br/>regras de negócio]
        R[Repositories<br/>Spring JDBC]
        P[ProdutorEventos]
        Q[ConsumidorNotificacoes]
        F[AssistenteClient<br/>OpenFeign]
        A[Actuator]
    end
    DB[(Oracle 19c<br/>Flyway V1–V4)]
    MQ{{RabbitMQ<br/>futurevet.eventos → futurevet.notificacoes}}
    EU[Eureka<br/>:8761]
    IA["futurevet-ia (Spring AI · :8082)"]
    LLM[Modelo de linguagem<br/>Groq / OpenAI]

    W --> C
    M --> C
    C --> S --> R --> DB
    S -- evento após commit --> P --> MQ --> Q --> R
    C --> F -- POST /ia/assistente --> IA --> LLM
    F -. descobre .-> EU
    IA -. registra .-> EU
    A -. health .-> IA
```

### Decisões de design e justificativas

| Decisão | Por quê |
|---|---|
| **IA em microsserviço separado** | A chave e as dependências do provedor de IA ficam isoladas. Se a IA cair, o resto do sistema continua funcionando (a tela mostra um aviso amigável e a API responde `503`). |
| **Contexto montado na aplicação principal** | Só ela conhece a sessão e as permissões. O microsserviço recebe apenas o resumo autorizado, o que reduz o risco de vazar dados de outro tutor. |
| **Prompt montado com objetos (`SystemMessage`/`UserMessage`)** | Chaves `{ }` digitadas pelo usuário não são interpretadas como variáveis de template. |
| **Provedor compatível com a API OpenAI (Groq por padrão)** | Groq tem plano gratuito. Trocar para OpenAI é só configuração (`IA_BASE_URL`, `IA_MODELO`), sem mudar código. |
| **RabbitMQ com *fallback* local** | O deploy gratuito não precisa de broker para funcionar. Com `RABBITMQ_ENABLED=true` a fila é usada de verdade (docker-compose ou CloudAMQP). |
| **Spring JDBC em vez de JPA** | O esquema Oracle legado (Sprints 1–2) tem nomes e tipos próprios. SQL explícito com parâmetros evita surpresas de mapeamento e SQL injection. |
| **Enums para status, perfis e eventos** | Elimina textos soltos ("AGENDADA", "ADMIN") repetidos pelo código. A lista SQL de status ativos é derivada do próprio enum. |
| **Mensagens de regra por código (i18n)** | `RegraException("erro.consulta.conflito")` é traduzida no idioma do usuário, nas telas e na API. |
| **Trava de agenda (`SELECT … FOR UPDATE`)** | Dois agendamentos simultâneos não ocupam o mesmo horário (há teste com duas threads). |

---

## 🔄 Principais fluxos do sistema

1. **Agendamento:** tutor solicita → evento `CONSULTA_SOLICITADA` → clínica recebe aviso → clínica confirma → evento `CONSULTA_CONFIRMADA` → tutor recebe aviso → após o horário, clínica conclui com resumo.
2. **Carteira de vacinação:** registrar dose → situação calculada → registrar reforço (próxima dose = data + intervalo; dose anterior vira *histórico*, travada para edição).
3. **Assistente Vet:** pergunta → contexto do usuário (animais, vacinas, consultas e as últimas trocas da conversa) → OpenFeign → `futurevet-ia` → Spring AI → resposta no idioma escolhido.
4. **Monitoramento (clínica):** `/admin/monitoramento` mostra saúde da aplicação, banco, IA e RabbitMQ, uso de memória e CPU, tempo no ar, requisições e eventos por canal.

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.5.16 | Base da aplicação e dos microsserviços |
| Spring Security + JJWT | 6.5 / 0.12.6 | Sessão (web) e JWT (API) |
| Spring JDBC + Flyway | — | Persistência e versionamento do banco |
| Thymeleaf | 3.1 | Interface web |
| Spring AMQP (RabbitMQ) | 3.2 | Mensageria |
| Spring Cloud (OpenFeign, Eureka) | 2025.0.3 | Microsserviços |
| Spring AI | 1.1.8 | Assistente com modelo de linguagem |
| Spring Boot Actuator / Micrometer | — | Monitoramento |
| Springdoc OpenAPI | 2.8.17 | Swagger da API |
| Oracle Database / H2 | 19c / 2.x | Produção / demonstração e testes |

---

## ⚙️ Como executar

### Antes de tudo: clonar o repositório

```bash
git clone https://github.com/joaocaetano1310/Challenge_Sprint4_Java.git
cd Challenge_Sprint4_Java
```

### Opção 1 — Arquitetura completa com Docker (recomendado)

Sobe **RabbitMQ + Eureka + IA + aplicação**:

```bash
cp .env.exemplo .env      # coloque sua chave em IA_API_KEY (gratuita em https://console.groq.com/keys)
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Aplicação | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Painel do RabbitMQ | http://localhost:15672 (guest / guest) |
| Swagger | http://localhost:8080/swagger-ui/index.html |

### Opção 2 — Sem Docker (Windows)

Requisito: Java 17 ou superior. Dê dois cliques em `iniciar-demo.cmd` (Linux/macOS: `./iniciar-demo.sh`).

O script compila e sobe **Eureka + IA + aplicação**. Sem RabbitMQ instalado, os avisos são entregues em modo local.

### Opção 3 — Só a aplicação principal

```bash
./mvnw spring-boot:run          # perfil demo (H2). O assistente mostrará "indisponível".
```

### Testes

```bash
./mvnw verify                                        # aplicação principal (38 testes)
./mvnw -f microsservicos/futurevet-ia/pom.xml verify # microsserviço de IA (2 testes)
```

---

## 🔐 Variáveis de ambiente

| Variável | Padrão | Função |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `demo` | `demo` (H2 + dados fictícios) ou `oracle` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | — | Conexão Oracle (perfil `oracle`) |
| `JWT_SECRET` | valor de desenvolvimento | Chave do token da API (**troque em produção**) |
| `RABBITMQ_ENABLED` | `false` | `true` publica e consome pela fila |
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | Endereço do broker (ex.: CloudAMQP) |
| `IA_URL` | `http://localhost:8082` | URL do microsserviço de IA. **Vazio** = descobrir pelo Eureka |
| `EUREKA_ENABLED`, `EUREKA_URL` | `false`, `http://localhost:8761/eureka/` | Registro/descoberta no Eureka |
| `IA_API_KEY` *(microsserviço de IA)* | — | Chave do provedor de IA (nunca versionar) |
| `IA_BASE_URL`, `IA_MODELO` *(IA)* | Groq, `llama-3.3-70b-versatile` | Provedor e modelo compatíveis com a API OpenAI |

---

## 🔌 API REST (app mobile)

Autenticação: `POST /api/auth/login` devolve o token. Envie o token como `Authorization: Bearer <token>` nas demais rotas.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` · `/api/auth/registro` | Login e cadastro de tutor |
| GET/POST/PUT/DELETE | `/api/animais` | CRUD de animais |
| GET/POST/PUT/DELETE | `/api/vacinas` · POST `/api/vacinas/{id}/reforco` | Carteira de vacinação |
| GET/POST/PUT | `/api/consultas` · POST `/{id}/confirmar` `/cancelar` `/concluir` | Agenda |
| GET | `/api/notificacoes` · `/api/notificacoes/nao-lidas` · POST `/lidas` | **Sprint 4** — avisos |
| POST | `/api/assistente` | **Sprint 4** — pergunta ao Assistente Vet |
| GET | `/actuator/health` · `/actuator/info` | **Sprint 4** — saúde (público) |

Documentação interativa: `/swagger-ui/index.html`.

---

## 🗄️ Banco de dados

O **Flyway** aplica as migrations automaticamente:

| Versão | Descrição |
|---|---|
| `V1__estrutura_original.sql` | Estrutura original das tabelas (Sprint 1) |
| `V2__suporte_aplicacao.java` | Perfis, status das consultas, vínculo de reforços, sequences e índices |
| `V3__proteger_senhas.java` | Conversão das senhas para hash BCrypt |
| `V4__notificacoes.sql` | **Sprint 4** — `TB_FV_NOTIFICACAO` e sequence `FV_SEQ_NOTIFICACAO` |

Procedimento seguro para o Oracle da FIAP: [docs/ORACLE.md](docs/ORACLE.md).

---

## 🤝 Integração multidisciplinar

| Disciplina | Como se conecta ao FutureVet | Evidência |
|---|---|---|
| **Mobile Application Development** | O app React Native/Expo consome a API REST desta aplicação (login JWT, animais, vacinas, consultas). Na Sprint 4 a API ganhou as rotas de notificações e do assistente, já disponíveis para o app. | [Repositório mobile](https://github.com/Felipe-Furlanetto0504/Challenge-FutureVet-Mobile) |
| **Disruptive Architectures: IoT, IoB & IA Generativa** | A coleira PetLink (ESP32-S3, DS18B20, MAX30102, MPU-6050) publica sinais vitais via MQTT/HiveMQ, com detecção de anomalias em Python. O Assistente Vet desta entrega aplica IA generativa ao mesmo domínio. | [Repositório IoT](https://github.com/Buenozw/CHALLENGE---IOT) |
| **DevOps Tools & Cloud Computing** | Dockerfiles multi-stage, `docker-compose.yml` com 4 serviços, deploy no Render via `render.yaml` com health check do Actuator. | `Dockerfile`, `docker-compose.yml`, `render.yaml` |
| **Compliance, Quality Assurance & Tests** | Backlog Scrum no Azure Boards (épicos → features → PBIs → tasks) e 40 testes automatizados (JUnit 5, MockMvc, Mockito), incluindo concorrência e segurança. | `src/test`, [docs/VALIDACAO.md](docs/VALIDACAO.md) |
| **Mastering Relational and Non-Relational Database** | Modelo Oracle das Sprints 1–2 (tabelas, constraints, procedures) evoluído com Flyway, sem perder dados legados. | `database/script_bd.sql`, `db/migration` |

---

## 📁 Estrutura do projeto

```
Challenge_Sprint4_Java/
├── src/main/java/com/futurevet/
│   ├── config/               # Dados de demonstração e primeiro administrador
│   ├── ia/                   # Cliente OpenFeign, contexto e conversa do Assistente Vet
│   ├── internacionalizacao/  # LocaleResolver, troca de idioma e resolução de mensagens
│   ├── mensageria/           # RabbitMQ: configuração, produtor e consumidor de eventos
│   ├── model/                # Records e enums do domínio
│   ├── monitoramento/        # Leitura das métricas do Actuator
│   ├── repository/           # SQL com JdbcTemplate
│   ├── security/             # Spring Security, JWT e verificação de dono
│   ├── service/              # Regras de negócio
│   └── web/                  # Controllers MVC, API REST (web/api) e formulários
├── src/main/resources/
│   ├── db/migration/         # Flyway (V1, V4) — V2 e V3 estão em src/main/java/db/migration
│   ├── templates/            # Telas Thymeleaf
│   ├── mensagens.properties  # Textos em português
│   └── mensagens_en.properties
├── microsservicos/
│   ├── futurevet-ia/         # Microsserviço Spring AI (porta 8082)
│   └── servidor-eureka/      # Service discovery (porta 8761)
├── docs/                     # Arquitetura, Oracle, validação e roteiro do vídeo
├── docker-compose.yml
├── render.yaml
└── iniciar-demo.cmd / .sh
```

---

## 📚 Documentação complementar

- [docs/ARQUITETURA.md](docs/ARQUITETURA.md) — guia para entender e explicar cada camada
- [docs/ORACLE.md](docs/ORACLE.md) — como aplicar as migrations no Oracle com segurança
- [docs/VALIDACAO.md](docs/VALIDACAO.md) — o que foi testado e como
- [docs/ROTEIRO-VIDEO.md](docs/ROTEIRO-VIDEO.md) — roteiro da apresentação de 15 minutos

<p align="center">Desenvolvido para o Challenge FIAP 2026 — 2º Semestre · Clyvo Vet</p>
