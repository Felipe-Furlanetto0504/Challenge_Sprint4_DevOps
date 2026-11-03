-- =====================================================================================
--  FutureVet — SELECTs de evidência do CRUD (vídeo da Sprint 4 · DevOps)
--  Banco: Oracle da FIAP (oracle.fiap.com.br:1521/ORCL) — mesmo schema usado pelo Web App
--  Rode cada bloco no SQL Developer logo depois da operação feita na aplicação no Azure.
--  Tabelas do CORE: TB_ANIMAL (pet) e TB_VACINA (carteira de vacinação), ligadas por ID_ANIMAL.
-- =====================================================================================

-- 0) Antes de começar: conferir que o Flyway da aplicação está aplicado no schema
SELECT "version", "description", "success"
  FROM "futurevet_web_history"
 ORDER BY "installed_rank";

-- -------------------------------------------------------------------------------------
-- 1) CREATE — depois de cadastrar o pet "Thor" na tela Animais > Novo animal
-- -------------------------------------------------------------------------------------
SELECT a.ID_ANIMAL, a.NOME_ANIMAL, a.ESPEC_ANIMAL, a.RACA_ANIMAL, a.IDADE_ANIMAL,
       a.PORTE_ANIMAL, a.PESO_ANIMAL, u.NOME AS TUTOR
  FROM TB_ANIMAL a
  JOIN TB_USUARIO u ON u.ID_USUARIO = a.ID_USUARIO
 WHERE a.NOME_ANIMAL = 'Thor';

-- ... e depois de registrar a vacina V10 para o Thor (tabela relacionada)
SELECT v.ID_VACINA, v.NOME_VACINA, v.DATA_VACINA, v.PROX_DOSE, v.LOC_VACINACAO,
       a.NOME_ANIMAL
  FROM TB_VACINA v
  JOIN TB_ANIMAL a ON a.ID_ANIMAL = v.ID_ANIMAL
 WHERE a.NOME_ANIMAL = 'Thor';

-- -------------------------------------------------------------------------------------
-- 2) READ — a mesma listagem que a aplicação mostra (pets e suas vacinas)
-- -------------------------------------------------------------------------------------
SELECT a.ID_ANIMAL, a.NOME_ANIMAL, a.ESPEC_ANIMAL,
       COUNT(v.ID_VACINA) AS QTD_VACINAS
  FROM TB_ANIMAL a
  LEFT JOIN TB_VACINA v ON v.ID_ANIMAL = a.ID_ANIMAL
 GROUP BY a.ID_ANIMAL, a.NOME_ANIMAL, a.ESPEC_ANIMAL
 ORDER BY a.ID_ANIMAL;

-- -------------------------------------------------------------------------------------
-- 3) UPDATE — depois de editar o Thor (ex.: peso 12.5 -> 13.8 e porte MEDIO)
-- -------------------------------------------------------------------------------------
SELECT ID_ANIMAL, NOME_ANIMAL, PESO_ANIMAL, PORTE_ANIMAL
  FROM TB_ANIMAL
 WHERE NOME_ANIMAL = 'Thor';

-- -------------------------------------------------------------------------------------
-- 4) DELETE — depois de excluir a vacina e, em seguida, o pet Thor
--    (as duas consultas devem voltar SEM LINHAS)
-- -------------------------------------------------------------------------------------
SELECT v.ID_VACINA, v.NOME_VACINA
  FROM TB_VACINA v
  JOIN TB_ANIMAL a ON a.ID_ANIMAL = v.ID_ANIMAL
 WHERE a.NOME_ANIMAL = 'Thor';

SELECT ID_ANIMAL, NOME_ANIMAL
  FROM TB_ANIMAL
 WHERE NOME_ANIMAL = 'Thor';
