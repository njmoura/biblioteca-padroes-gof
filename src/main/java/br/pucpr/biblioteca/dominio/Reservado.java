package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/** ConcreteState RESERVADO: aceita apenas emprestar, para o mesmo estudante da reserva. */
class Reservado extends EstadoLivro {
    static final Reservado INSTANCIA = new Reservado();

    @Override
    void emprestar(Livro l, Estudante e, LocalDate hoje) {
        if (e == null || !e.getId().equals(l.getReservante())) {            // guarda: [e = reservante]
            throw new ReservaInvalidaException("livro " + l.getId() + " esta reservado para outro estudante");
        }
        l.definirVencimento(hoje.plusDays(Livro.PRAZO_EMPRESTIMO_DIAS));    // acao: vencimento := hoje + prazo
        l.mudarPara(Emprestado.INSTANCIA);
    }

    @Override
    Situacao situacao() { return Situacao.RESERVADO; }
}
