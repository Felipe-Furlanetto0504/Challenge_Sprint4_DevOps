# Conectar ao Oracle sem repetir os erros anteriores

## O que esta aplicação espera

O esquema base é o do SQL enviado: TB_USUARIO, TB_ANIMAL, TB_CONSULTA, TB_VACINA e TB_LOG_ERRO. Idade permanece VARCHAR2(30); peso NUMBER(5,2); CPF/telefone NUMBER(11); hora VARCHAR2(5). As FKs originais são preservadas, sem ON DELETE CASCADE.

A aplicação prepara, mas não executou no servidor da FIAP, estas mudanças:

1. SENHA de 18 para 255 caracteres; ROLE (TUTOR/ADMIN) em TB_USUARIO.
2. STATUS e OBSERVACAO em TB_CONSULTA. Registros antigos recebem INDEFINIDA, sem inventar histórico.
3. ID_DOSE_ORIGEM em TB_VACINA, FK para a dose anterior e restrição de um reforço por dose.
4. FV_SEQ_USUARIO, FV_SEQ_ANIMAL, FV_SEQ_CONSULTA e FV_SEQ_VACINA, iniciadas acima do maior ID existente.
5. TB_FV_AGENDA_LOCK e três índices para relações.
6. Conversão das senhas existentes em texto para BCrypt; hashes BCrypt válidos não são recalculados.

Os nomes das colunas originais e os dados de usuários/animais/vacinas/consultas não são removidos. A conversão de senha altera o conteúdo de SENHA e é irreversível sem backup.

### Sprint 4

A `V4__notificacoes.sql` cria a sequence `FV_SEQ_NOTIFICACAO` e a tabela `TB_FV_NOTIFICACAO` (FK para `TB_USUARIO`). Ela só acrescenta objetos novos e não altera tabelas existentes. Em um banco que já está na V3, basta subir a aplicação com `DB_MIGRATIONS_ENABLED=true`.

## Antes da atualização

- Confirme com o responsável pelo banco o uso dessas extensões. Faça backup recuperável e valide inicialmente em uma cópia do schema.
- Pare gravações de outras aplicações durante a atualização e confirme que elas também usarão os hashes BCrypt e as sequences novas. Comparar senha em texto ou continuar enviando IDs fixos deixará a integração incompatível.
- Execute apenas as consultas de leitura de `database/preflight_oracle.sql` e compare com o arquivo enviado. Verifique especialmente histórico Flyway, possíveis colunas adicionadas nas tentativas anteriores, sequences e e-mails duplicados sem diferenciar maiúsculas.
- A nova tabela de histórico é `futurevet_web_history`. O histórico antigo `flyway_schema_history` não é apagado ou modificado. A nova aplicação assume o versionamento das tabelas de domínio após a adoção; não mantenha duas aplicações executando migrations concorrentes sobre elas.
- Não execute `FutureVetDB.sql` inteiro em um banco existente: ele contém CREATEs, INSERTs e COMMITs. Não execute nenhum script de reset.

## Oracle já existente, igual ao script recebido

Após a revisão e o backup, configure no PowerShell, na pasta do projeto:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'oracle'
$env:DB_URL = 'jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL'
$env:DB_USER = 'SEU_USUARIO'
$env:DB_PASSWORD = 'SUA_SENHA'
$env:DB_EXISTENTE = 'true'
$env:DB_MIGRATIONS_ENABLED = 'true'
$env:FV_PRIMEIRO_ADMIN_EMAIL = 'EMAIL_DE_UM_USUARIO_EXISTENTE'
java -jar target/futurevet.jar
```

As credenciais acima são placeholders. Não salve valores reais em arquivos versionados. O e-mail de administrador é uma conta já existente no banco, escolhida explicitamente pelo responsável. A promoção acontece apenas quando não há nenhum ADMIN; a variável não redefine senha nem promove novas contas em todo reinício.

DB_EXISTENTE=true cria o baseline 1 no novo histórico e executa V2, V3 e V4. Só use isso se a V1 já estiver representada pelo schema correto. O baseline não valida a equivalência estrutural automaticamente. A V2 verifica e-mails repetidos por caixa e horários inválidos antes de executar seu DDL, mas isso não substitui o preflight.

Depois da adoção, remova FV_PRIMEIRO_ADMIN_EMAIL e DB_EXISTENTE das variáveis; pode manter Flyway ativado para validar o histórico. Se não quiser migrations automáticas em execuções de uso, deixe DB_MIGRATIONS_ENABLED=false após aplicar e verificar as versões. Não altere conteúdo de migrations que já foram aplicadas.

## Oracle novo e vazio

Use o mesmo procedimento com DB_EXISTENTE=false, sem FV_PRIMEIRO_ADMIN_EMAIL inicialmente. Flyway aplica V1, V2, V3 e V4. Não há usuários de teste nem carga fictícia nesse perfil. Cadastre o primeiro tutor pela tela; pare a aplicação; indique o e-mail dele em FV_PRIMEIRO_ADMIN_EMAIL e reinicie para torná-lo administrador. Remova a variável em seguida.

## Erros e limites

- Oracle faz commits implícitos em DDL. Se V2 falhar no meio, algumas mudanças podem já existir. Pare, inspecione o estado e planeje reparo supervisionado; não apague o histórico nem repita SQL às cegas. As migrations não são scripts idempotentes para execução manual repetida.
- Se o banco real tiver extensões diferentes, será necessário ajustar a estratégia antes da primeira adoção. Não use DB_EXISTENTE=true para contornar um schema desconhecido.
- As procedures originais fazem COMMIT interno e absorvem erros em TB_LOG_ERRO; a aplicação usa JDBC com transações próprias e não as invoca. Elas não são apagadas no banco existente. Para cumprir requisitos futuros de integração com procedures (Sprint 4), será necessário revisar seus contratos e o backend.
- As senhas do script original são curtas e em texto. A migração mantém a mesma senha de entrada por compatibilidade, mas novos cadastros exigem pelo menos 8 caracteres. Coordene a troca de senhas antigas fora desta entrega.
- Respeite os códigos CAO/GATO/COELHO/OUTRO e PEQUENO/MEDIO/GRANDE. Nenhuma coluna TB_PET é criada.
- Publicação exige HTTPS e HTTPS_ENABLED=true para cookie seguro. Não exponha demo online.

## Verificação após migrar a cópia

Confirme o histórico até V4, os totais de linhas antes/depois, PK/FK, leitura de dados legados com nulos, login com uma conta legada, criação de novos IDs, os dois perfis e os dois fluxos. Repita criação concorrente de consultas em duas sessões. Somente depois valide a mesma operação no schema oficial. **Essa verificação Oracle não foi realizada nesta entrega.**
