package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Reserva;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.time.LocalDate;

/** ConcreteProduct da familia relacional: tabelas reserva, emprestimo e devolucao. */
public class RepositorioMovimentacaoMySQL implements RepositorioMovimentacao {
    private final UnidadeDeTrabalhoMySQL uow;

    RepositorioMovimentacaoMySQL(UnidadeDeTrabalhoMySQL uow) {
        this.uow = uow;
    }

    @Override
    public void inserirReserva(Reserva r) {
        Sql.executar(uow.conexao(), "INSERT INTO reserva (id_livro, id_estudante, data_transacao) VALUES (?, ?, ?)",
                r.getIdLivro(), r.getIdEstudante(), r.getData());
    }

    @Override
    public void removerReserva(Reserva r) {
        Sql.executar(uow.conexao(), "DELETE FROM reserva WHERE id_livro = ? AND id_estudante = ?",
                r.getIdLivro(), r.getIdEstudante());
    }

    @Override
    public Reserva buscarReserva(String idLivro, String idEstudante) {
        return Sql.um(uow.conexao(),
                "SELECT id_livro, id_estudante, data_transacao FROM reserva WHERE id_livro = ? AND id_estudante = ?",
                rs -> new Reserva(rs.getObject("data_transacao", LocalDate.class), rs.getString("id_livro"), rs.getString("id_estudante")),
                idLivro, idEstudante);
    }

    @Override
    public void inserirEmprestimo(Emprestimo e) {
        Sql.executar(uow.conexao(), "INSERT INTO emprestimo (id_livro, id_estudante, data_transacao) VALUES (?, ?, ?)",
                e.getIdLivro(), e.getIdEstudante(), e.getData());
    }

    @Override
    public void removerEmprestimo(Emprestimo e) {
        Sql.executar(uow.conexao(), "DELETE FROM emprestimo WHERE id_livro = ? AND id_estudante = ?",
                e.getIdLivro(), e.getIdEstudante());
    }

    @Override
    public Emprestimo buscarEmprestimo(String idLivro, String idEstudante) {
        return Sql.um(uow.conexao(),
                "SELECT id_livro, id_estudante, data_transacao FROM emprestimo WHERE id_livro = ? AND id_estudante = ?",
                rs -> new Emprestimo(rs.getObject("data_transacao", LocalDate.class), rs.getString("id_livro"), rs.getString("id_estudante")),
                idLivro, idEstudante);
    }

    @Override
    public void inserirDevolucao(Devolucao d) {
        Sql.executar(uow.conexao(),
                "INSERT INTO devolucao (id, id_livro, id_estudante, data_transacao, vencimento) VALUES (?, ?, ?, ?, ?)",
                d.getId(), d.getIdLivro(), d.getIdEstudante(), d.getData(), d.getVencimento());
    }

    @Override
    public Devolucao buscarDevolucao(String idLivro, String idEstudante) {
        return Sql.um(uow.conexao(),
                "SELECT id, id_livro, id_estudante, data_transacao, vencimento FROM devolucao"
                        + " WHERE id_livro = ? AND id_estudante = ? ORDER BY data_transacao DESC",
                rs -> new Devolucao(rs.getString("id"), rs.getObject("data_transacao", LocalDate.class),
                        rs.getString("id_livro"), rs.getString("id_estudante"), rs.getObject("vencimento", LocalDate.class)),
                idLivro, idEstudante);
    }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }
}
