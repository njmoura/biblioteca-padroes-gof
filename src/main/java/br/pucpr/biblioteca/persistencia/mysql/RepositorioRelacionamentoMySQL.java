package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/** ConcreteProduct da familia relacional: tabelas pessoa, estudante e atendente. */
public class RepositorioRelacionamentoMySQL implements RepositorioRelacionamento {
    private final UnidadeDeTrabalhoMySQL uow;

    RepositorioRelacionamentoMySQL(UnidadeDeTrabalhoMySQL uow) {
        this.uow = uow;
    }

    @Override
    public Pessoa buscarPessoa(String cpf) {
        return Sql.um(uow.conexao(), "SELECT cpf, nome, sobrenome, email FROM pessoa WHERE cpf = ?",
                rs -> new Pessoa(rs.getString("cpf"), rs.getString("nome"), rs.getString("sobrenome"), rs.getString("email")),
                cpf);
    }

    @Override
    public void salvarPessoa(Pessoa p) {
        int alteradas = Sql.executar(uow.conexao(), "UPDATE pessoa SET nome = ?, sobrenome = ?, email = ? WHERE cpf = ?",
                p.getNome(), p.getSobrenome(), p.getEmail(), p.getCpf());
        if (alteradas == 0) {
            Sql.executar(uow.conexao(), "INSERT INTO pessoa (cpf, nome, sobrenome, email) VALUES (?, ?, ?, ?)",
                    p.getCpf(), p.getNome(), p.getSobrenome(), p.getEmail());
        }
    }

    @Override
    public void excluirPessoa(String cpf) {
        Sql.executar(uow.conexao(), "DELETE FROM pessoa WHERE cpf = ?", cpf);
    }

    @Override
    public Estudante buscarEstudante(String id) {
        return Sql.um(uow.conexao(), "SELECT id, cpf FROM estudante WHERE id = ?",
                rs -> new Estudante(rs.getString("id"), rs.getString("cpf")), id);
    }

    @Override
    public void salvarEstudante(Estudante e) {
        salvarPapel("estudante", e.getId(), e.getCpf());
    }

    @Override
    public void excluirEstudante(String id) {
        Sql.executar(uow.conexao(), "DELETE FROM estudante WHERE id = ?", id);
    }

    @Override
    public Atendente buscarAtendente(String id) {
        return Sql.um(uow.conexao(), "SELECT id, cpf FROM atendente WHERE id = ?",
                rs -> new Atendente(rs.getString("id"), rs.getString("cpf")), id);
    }

    @Override
    public void salvarAtendente(Atendente a) {
        salvarPapel("atendente", a.getId(), a.getCpf());
    }

    @Override
    public void excluirAtendente(String id) {
        Sql.executar(uow.conexao(), "DELETE FROM atendente WHERE id = ?", id);
    }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }

    private void salvarPapel(String tabela, String id, String cpf) {
        int alteradas = Sql.executar(uow.conexao(), "UPDATE " + tabela + " SET cpf = ? WHERE id = ?", cpf, id);
        if (alteradas == 0) {
            Sql.executar(uow.conexao(), "INSERT INTO " + tabela + " (id, cpf) VALUES (?, ?)", id, cpf);
        }
    }
}
