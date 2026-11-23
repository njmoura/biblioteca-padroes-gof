package br.pucpr.biblioteca.acesso;

import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;

/**
 * Ponto unico de acesso interno a pessoas, estudantes e atendentes (Trabalho 02),
 * mantido por associacoes distintas.
 */
public class Relacionamento {
    private final RepositorioRelacionamento repositorio;

    public Relacionamento(RepositorioRelacionamento repositorio) {
        this.repositorio = repositorio;
    }

    public Pessoa consultarPessoa(String cpf) { return repositorio.buscarPessoa(cpf); }

    public void inserirPessoa(Pessoa pessoa) { repositorio.salvarPessoa(pessoa); }

    public void atualizarPessoa(Pessoa pessoa) { repositorio.salvarPessoa(pessoa); }

    public void excluirPessoa(Pessoa pessoa) { repositorio.excluirPessoa(pessoa.getCpf()); }

    public Estudante consultarEstudante(String id) { return repositorio.buscarEstudante(id); }

    public void inserirEstudante(Estudante estudante) { repositorio.salvarEstudante(estudante); }

    public void excluirEstudante(Estudante estudante) { repositorio.excluirEstudante(estudante.getId()); }

    public Atendente consultarAtendente(String id) { return repositorio.buscarAtendente(id); }

    public void inserirAtendente(Atendente atendente) { repositorio.salvarAtendente(atendente); }

    public void excluirAtendente(Atendente atendente) { repositorio.excluirAtendente(atendente.getId()); }
}
