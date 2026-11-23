package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/** ConcreteProduct da familia em memoria. */
public class RepositorioRelacionamentoEmMemoria implements RepositorioRelacionamento {
    private final UnidadeDeTrabalhoEmMemoria uow;

    RepositorioRelacionamentoEmMemoria(UnidadeDeTrabalhoEmMemoria uow) {
        this.uow = uow;
    }

    @Override
    public Pessoa buscarPessoa(String cpf) { return uow.base().pessoas.get(cpf); }

    @Override
    public void salvarPessoa(Pessoa p) { uow.base().pessoas.put(p.getCpf(), p); }

    @Override
    public void excluirPessoa(String cpf) { uow.base().pessoas.remove(cpf); }

    @Override
    public Estudante buscarEstudante(String id) { return uow.base().estudantes.get(id); }

    @Override
    public void salvarEstudante(Estudante e) { uow.base().estudantes.put(e.getId(), e); }

    @Override
    public void excluirEstudante(String id) { uow.base().estudantes.remove(id); }

    @Override
    public Atendente buscarAtendente(String id) { return uow.base().atendentes.get(id); }

    @Override
    public void salvarAtendente(Atendente a) { uow.base().atendentes.put(a.getId(), a); }

    @Override
    public void excluirAtendente(String id) { uow.base().atendentes.remove(id); }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }
}
