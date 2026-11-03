# Roteiro do vídeo · Sprint 4 · DevOps (CI/CD)

Requisitos do vídeo: **720p ou mais**, áudio claro, **explicação por voz** (sem legendas), sem cortes durante a pipeline e o CRUD. Duração estimada: 12–15 min.

## Antes de gravar (checklist)

- [ ] Recursos criados com `infra/criar-recursos-azure.sh` e **um primeiro pipeline já executado com sucesso** (permissões do Variable Group, service connection e environment já liberadas — o pedido "Permit" não pode aparecer no meio do vídeo)
- [ ] Abas abertas: **IDE** com o projeto · **GitHub** do repositório · **Azure DevOps** (Pipelines, Library) · **Portal Azure** no Web App `futurevet-562766` · **SQL Developer** conectado ao Oracle FIAP com `database/crud-evidencias.sql` aberto · **app** em `https://futurevet-562766.azurewebsites.net`
- [ ] `git push` testado (credencial do GitHub salva na máquina)
- [ ] Conta de tutor de teste criada **ou** pronta para criar no vídeo; pet "Thor" ainda **não** existe no banco
- [ ] Microfone testado, resolução da tela em 1080p, zoom do navegador em 110–125%

---

## 1. Abertura (≈1 min)

> "Somos o grupo do FutureVet, plataforma de gestão veterinária do Challenge com a Clyvo Vet. Nesta Sprint 4 de DevOps vamos mostrar a esteira de CI/CD no Azure DevOps: a cada alteração de código na branch master, o pipeline compila, roda os testes, gera o artefato e faz o deploy automático no Azure Web App, que usa o banco Oracle da FIAP."

Mostre rapidamente o **diagrama** do README (`docs/arquitetura-cicd.png`) e passe pelos números 1 a 8.

## 2. Ferramentas usadas (≈1 min)

Passe por cada aba dizendo o papel de cada uma:
- **IDE** — código Spring Boot e o `azure-pipelines.yml`
- **GitHub** — repositório, branch `master`
- **Azure DevOps** — projeto *Sprint 4 – Azure DevOps* (mostre *Project settings → Overview*: nome, descrição, visibilidade Private, processo Scrum)
- **Portal Azure** — grupo `rg-futurevet-562766`: plano do App Service e Web App
- **SQL Developer** — Oracle da FIAP

## 3. Configuração da pipeline (≈2–3 min)

No Azure DevOps abra *Pipelines → FutureVet → Edit* (ou o `azure-pipelines.yml` na IDE) e explique de cima para baixo:
- `trigger: master` → "dispara a cada alteração de código na master"
- `variables: group futurevet-secrets` → abra *Library → futurevet-secrets* e mostre os **cadeados** em `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`: "nenhuma senha fica no código"
- **Estágio CI**: checkout → `Maven@4` com `clean package` (compila e roda os testes, publica resultados) → `CopyFiles` → `PublishPipelineArtifact` (artefato `futurevet-jar`)
- **Estágio CD**: `dependsOn: CI` → "só roda depois que o artefato novo foi gerado" → download do artefato → Azure CLI aplicando as variáveis protegidas → `az webapp deploy --type jar` → health check
- Mostre a **service connection** `sc-azure-futurevet` (*Project settings → Service connections*)

## 4. Alteração no código + disparo automático (≈1 min)

Antes, mostre a tela de login do app no ar com o texto **"Vamos cuidar juntos?"**.

Na IDE, abra `src/main/resources/mensagens.properties` e troque a linha:

```properties
login.chamada=Vamos cuidar juntos?
```
por
```properties
login.chamada=Vamos cuidar juntos? Deploy automático via Azure DevOps
```

No terminal:
```bash
git add src/main/resources/mensagens.properties
git commit -m "Altera chamada da tela de login para demonstrar o CI/CD"
git push origin master
```

Volte ao Azure DevOps → *Pipelines* e mostre a execução **iniciando sozinha**, com o commit que acabou de subir.

## 5. Execução do CI/CD comentada (≈4–6 min, sem cortes)

Abra a execução e clique em cada job para mostrar os logs enquanto roda:
1. **CI · Checkout** — código baixado do commit novo
2. **CI · Maven** — compilação e testes; aponte as linhas `Tests run: ..., Failures: 0`
3. **CI · Publish Pipeline Artifact** — artefato publicado
4. **CD** começando automaticamente depois do CI
5. **CD · App Settings** — mostre que os valores secretos aparecem como `***`
6. **CD · Deploy do .jar** — `az webapp deploy`
7. **CD · Health check** — resposta `{"status":"UP"}`

## 6. Artefato e testes (≈1 min)

Na página da execução:
- **Tests** (aba) — total de testes, todos passando
- **1 published** / *Related → Artifacts* — abra `futurevet-jar` e mostre o `futurevet.jar`

## 7. Recursos no Portal Azure (≈1 min)

- Grupo `rg-futurevet-562766` → plano e Web App
- Web App → **Overview** (status Running, URL) → **Deployment Center / Deployment logs** (deploy recém-feito) → **Environment variables** (mostre as chaves, valores ocultos)
- Opcional: **Log stream** com a aplicação subindo

## 8. Aplicação com a alteração + CRUD com SELECT (≈4 min, sem cortes)

Abra `https://futurevet-562766.azurewebsites.net/login` → mostre o **texto novo** na tela ("a alteração do commit já está em produção"). Mostre também `/actuator/info` com o número do build.

Faça cada operação e, logo depois, rode o SELECT correspondente de `database/crud-evidencias.sql`:

| Operação | Na aplicação | No SQL Developer |
|---|---|---|
| Create | Cadastre-se → *Animais → + Cadastrar animal*: Thor · Cão · Labrador · 3 anos · Médio · 12,5 kg | bloco 1 (aparece o Thor) |
| Create (tabela relacionada) | *Vacinas → nova*: V10 para o Thor, data de hoje, próxima dose em 1 ano | bloco 1, segundo SELECT |
| Read | Lista de animais e de vacinas | bloco 2 |
| Update | Editar o Thor: peso 13,8 | bloco 3 (peso atualizado) |
| Delete | Excluir a vacina V10, depois excluir o Thor | bloco 4 (sem linhas) |

> Fale: "A aplicação está no Azure Web App, o dado está no Oracle da FIAP, e cada operação feita na tela aparece no banco pelo SELECT."

## 9. Encerramento (≈30 s)

> "Recapitulando: alteramos o código, o push na master disparou o CI, que compilou, rodou os testes e publicou o artefato; o CD pegou esse artefato novo, aplicou as variáveis protegidas e fez o deploy no Azure Web App; e comprovamos a alteração e o CRUD no banco Oracle em nuvem."
