package br.pucpr.biblioteca.acesso;

import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Reserva;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;

/**
 * Ponto unico de acesso interno as transacoes (Trabalho 02), mantido por associacoes
 * distintas: reservas, emprestimos e devolucoes.
 */
public class Movimentacao {
    private final RepositorioMovimentacao repositorio;

    public Movimentacao(RepositorioMovimentacao repositorio) {
        this.repositorio = repositorio;
    }

    public void inserir(Reserva reserva) { repositorio.inserirReserva(reserva); }

    public void inserir(Emprestimo emprestimo) { repositorio.inserirEmprestimo(emprestimo); }

    public void inserir(Devolucao devolucao) { repositorio.inserirDevolucao(devolucao); }

    public void remover(Reserva reserva) { repositorio.removerReserva(reserva); }

    public void remover(Emprestimo emprestimo) { repositorio.removerEmprestimo(emprestimo); }

    public Reserva consultarReserva(String idLivro, String idEstudante) {
        return repositorio.buscarReserva(idLivro, idEstudante);
    }

    public Emprestimo consultarEmprestimo(String idLivro, String idEstudante) {
        return repositorio.buscarEmprestimo(idLivro, idEstudante);
    }

    public Devolucao consultarDevolucao(String idLivro, String idEstudante) {
        return repositorio.buscarDevolucao(idLivro, idEstudante);
    }
}
