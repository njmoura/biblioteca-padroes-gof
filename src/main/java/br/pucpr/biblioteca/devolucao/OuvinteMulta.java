package br.pucpr.biblioteca.devolucao;

import br.pucpr.biblioteca.multa.iMulta;

/** ConcreteObserver: se houve atraso, solicita a multa ao Financeiro. */
public class OuvinteMulta implements OuvinteDeDevolucao {
    private final iMulta financeiro;

    public OuvinteMulta(iMulta financeiro) {
        this.financeiro = financeiro;
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        if (evento.diasAtraso() > 0) {
            financeiro.emitirMulta(evento.idDevolucao(), evento.cpfEstudante(), evento.diasAtraso(), evento.devolvidoEm());
        }
    }
}
