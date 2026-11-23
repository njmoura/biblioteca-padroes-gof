package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Reserva;
import br.pucpr.biblioteca.dominio.Transacao;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.util.List;

/** ConcreteProduct da familia em memoria. */
public class RepositorioMovimentacaoEmMemoria implements RepositorioMovimentacao {
    private final UnidadeDeTrabalhoEmMemoria uow;

    RepositorioMovimentacaoEmMemoria(UnidadeDeTrabalhoEmMemoria uow) {
        this.uow = uow;
    }

    @Override
    public void inserirReserva(Reserva r) { uow.base().reservas.add(r); }

    @Override
    public void removerReserva(Reserva r) {
        uow.base().reservas.removeIf(x -> mesma(x, r.getIdLivro(), r.getIdEstudante()));
    }

    @Override
    public Reserva buscarReserva(String idLivro, String idEstudante) {
        return ultima(uow.base().reservas, idLivro, idEstudante);
    }

    @Override
    public void inserirEmprestimo(Emprestimo e) { uow.base().emprestimos.add(e); }

    @Override
    public void removerEmprestimo(Emprestimo e) {
        uow.base().emprestimos.removeIf(x -> mesma(x, e.getIdLivro(), e.getIdEstudante()));
    }

    @Override
    public Emprestimo buscarEmprestimo(String idLivro, String idEstudante) {
        return ultima(uow.base().emprestimos, idLivro, idEstudante);
    }

    @Override
    public void inserirDevolucao(Devolucao d) { uow.base().devolucoes.add(d); }

    @Override
    public Devolucao buscarDevolucao(String idLivro, String idEstudante) {
        return ultima(uow.base().devolucoes, idLivro, idEstudante);
    }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }

    private boolean mesma(Transacao t, String idLivro, String idEstudante) {
        return t.getIdLivro().equals(idLivro) && t.getIdEstudante().equals(idEstudante);
    }

    private <T extends Transacao> T ultima(List<T> lista, String idLivro, String idEstudante) {
        T achada = null;
        for (T t : lista) {
            if (mesma(t, idLivro, idEstudante)) {
                achada = t;
            }
        }
        return achada;
    }
}
