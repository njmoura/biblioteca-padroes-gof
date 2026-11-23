package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** ConcreteProduct da familia relacional. O estado e gravado como texto (Situacao) e reconstituido ao ler. */
public class RepositorioLivroMySQL implements RepositorioLivro {
    private static final String COLUNAS = "id, titulo, situacao, vencimento, num_renovacoes, reservante";
    private final UnidadeDeTrabalhoMySQL uow;

    RepositorioLivroMySQL(UnidadeDeTrabalhoMySQL uow) {
        this.uow = uow;
    }

    @Override
    public Livro buscar(String id) {
        return Sql.um(uow.conexao(), "SELECT " + COLUNAS + " FROM livro WHERE id = ?", this::ler, id);
    }

    @Override
    public List<Livro> listar() {
        return Sql.consultar(uow.conexao(), "SELECT " + COLUNAS + " FROM livro ORDER BY id", this::ler);
    }

    @Override
    public void salvar(Livro l) {
        int alteradas = Sql.executar(uow.conexao(),
                "UPDATE livro SET titulo = ?, situacao = ?, vencimento = ?, num_renovacoes = ?, reservante = ? WHERE id = ?",
                l.getTitulo(), l.getSituacao().name(), l.getVencimento(), l.getNumRenovacoes(), l.getReservante(), l.getId());
        if (alteradas == 0) {
            Sql.executar(uow.conexao(),
                    "INSERT INTO livro (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?)",
                    l.getId(), l.getTitulo(), l.getSituacao().name(), l.getVencimento(), l.getNumRenovacoes(), l.getReservante());
        }
    }

    @Override
    public void excluir(String id) {
        Sql.executar(uow.conexao(), "DELETE FROM livro WHERE id = ?", id);
    }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }

    private Livro ler(ResultSet rs) throws SQLException {
        return new Livro(rs.getString("id"), rs.getString("titulo"),
                Situacao.valueOf(rs.getString("situacao")),
                rs.getObject("vencimento", LocalDate.class),
                rs.getInt("num_renovacoes"), rs.getString("reservante"));
    }
}
