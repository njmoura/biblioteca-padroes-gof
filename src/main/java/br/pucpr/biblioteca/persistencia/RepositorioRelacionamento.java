package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Pessoa;

/** Abstract Factory (AbstractProduct). Pessoas, estudantes e atendentes em colecoes/tabelas distintas. */
public interface RepositorioRelacionamento {
    Pessoa buscarPessoa(String cpf);

    void salvarPessoa(Pessoa p);

    void excluirPessoa(String cpf);

    Estudante buscarEstudante(String id);

    void salvarEstudante(Estudante e);

    void excluirEstudante(String id);

    Atendente buscarAtendente(String id);

    void salvarAtendente(Atendente a);

    void excluirAtendente(String id);

    UnidadeDeTrabalho unidade();
}
