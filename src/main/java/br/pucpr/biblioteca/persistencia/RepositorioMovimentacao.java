package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Reserva;

/** Abstract Factory (AbstractProduct). Reservas, emprestimos e devolucoes ficam em colecoes/tabelas distintas. */
public interface RepositorioMovimentacao {
    void inserirReserva(Reserva r);

    void removerReserva(Reserva r);

    Reserva buscarReserva(String idLivro, String idEstudante);

    void inserirEmprestimo(Emprestimo e);

    void removerEmprestimo(Emprestimo e);

    Emprestimo buscarEmprestimo(String idLivro, String idEstudante);

    void inserirDevolucao(Devolucao d);

    Devolucao buscarDevolucao(String idLivro, String idEstudante);

    UnidadeDeTrabalho unidade();
}
