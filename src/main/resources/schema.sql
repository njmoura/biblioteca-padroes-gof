-- Esquema da familia relacional (MySQL principal). Criado automaticamente na primeira conexao.
CREATE TABLE IF NOT EXISTS pessoa (
  cpf        VARCHAR(14)  NOT NULL PRIMARY KEY,
  nome       VARCHAR(80)  NOT NULL,
  sobrenome  VARCHAR(80),
  email      VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS estudante (
  id   VARCHAR(20) NOT NULL PRIMARY KEY,
  cpf  VARCHAR(14) NOT NULL
);

CREATE TABLE IF NOT EXISTS atendente (
  id   VARCHAR(20) NOT NULL PRIMARY KEY,
  cpf  VARCHAR(14) NOT NULL
);

-- situacao guarda o nome do estado (LIVRE, RESERVADO, ...); o objeto de estado e reconstituido na leitura
CREATE TABLE IF NOT EXISTS livro (
  id              VARCHAR(20)  NOT NULL PRIMARY KEY,
  titulo          VARCHAR(160) NOT NULL,
  situacao        VARCHAR(12)  NOT NULL,
  vencimento      DATE,
  num_renovacoes  INT          NOT NULL,
  reservante      VARCHAR(20)
);

-- chave primaria em id_livro: no maximo uma reserva ativa e um emprestimo ativo por exemplar
CREATE TABLE IF NOT EXISTS reserva (
  id_livro        VARCHAR(20) NOT NULL PRIMARY KEY,
  id_estudante    VARCHAR(20) NOT NULL,
  data_transacao  DATE        NOT NULL
);

CREATE TABLE IF NOT EXISTS emprestimo (
  id_livro        VARCHAR(20) NOT NULL PRIMARY KEY,
  id_estudante    VARCHAR(20) NOT NULL,
  data_transacao  DATE        NOT NULL
);

CREATE TABLE IF NOT EXISTS devolucao (
  id              VARCHAR(40) NOT NULL PRIMARY KEY,
  id_livro        VARCHAR(20) NOT NULL,
  id_estudante    VARCHAR(20) NOT NULL,
  data_transacao  DATE        NOT NULL,
  vencimento      DATE        NOT NULL
);
