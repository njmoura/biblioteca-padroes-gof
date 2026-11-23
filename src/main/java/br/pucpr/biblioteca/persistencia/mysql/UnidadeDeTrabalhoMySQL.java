package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.persistencia.ErroDePersistencia;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ConcreteProduct da familia relacional. Guarda a UNICA conexao com o MySQL principal;
 * todos os repositorios MySQL escrevem por ela, entao um confirmar() e um unico COMMIT.
 * A conexao so e aberta no primeiro uso. Nada aqui escreve no Oracle (replica).
 */
public class UnidadeDeTrabalhoMySQL implements UnidadeDeTrabalho {
    private final String url;
    private final String usuario;
    private final String senha;
    private Connection conexao;

    UnidadeDeTrabalhoMySQL(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    Connection conexao() {
        if (conexao == null) {
            Connection nova = null;
            try {
                nova = DriverManager.getConnection(url, usuario, senha);
                criarTabelas(nova);
                conexao = nova;                                      // so guarda a conexao com o esquema pronto
            } catch (SQLException | RuntimeException e) {
                fechar(nova);
                throw e instanceof ErroDePersistencia erro ? erro
                        : new ErroDePersistencia("nao foi possivel conectar ao banco em " + url, e);
            }
        }
        return conexao;
    }

    @Override
    public void iniciar() {
        try {
            conexao().setAutoCommit(false);
        } catch (SQLException e) {
            throw new ErroDePersistencia("falha ao iniciar a transacao", e);
        }
    }

    @Override
    public void confirmar() {
        try {
            conexao().commit();
            conexao().setAutoCommit(true);
        } catch (SQLException e) {
            throw new ErroDePersistencia("falha ao confirmar a transacao", e);
        }
    }

    @Override
    public void desfazer() {
        try {
            conexao().rollback();
            conexao().setAutoCommit(true);
        } catch (SQLException e) {
            // quem chama (ServicoDeCirculacao.emTransacao) anexa esta falha a original, sem mascara-la
            throw new ErroDePersistencia("falha ao desfazer a transacao", e);
        }
    }

    private static void fechar(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignorada) {
                // ja ha uma falha mais importante sendo propagada
            }
        }
    }

    /** Executa schema.sql (CREATE TABLE IF NOT EXISTS), para nao exigir configuracao manual. */
    private void criarTabelas(Connection c) throws SQLException {
        try (InputStream in = getClass().getResourceAsStream("/schema.sql"); Statement st = c.createStatement()) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^--.*$", "");                  // tira as linhas de comentario
            for (String comando : script.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        } catch (IOException e) {
            throw new ErroDePersistencia("nao foi possivel ler schema.sql", e);
        }
    }
}
