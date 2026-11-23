package br.pucpr.biblioteca.dominio;

/** ConcreteState LIVRE: aceita reservar, enviarParaReparo (dano identificado no acervo) e excluir. */
class Livre extends EstadoLivro {
    static final Livre INSTANCIA = new Livre();

    @Override
    void reservar(Livro l, Estudante e) {
        if (e == null) {                       // guarda: [estudante cadastrado]
            throw new ReservaInvalidaException("estudante nao cadastrado");
        }
        l.definirReservante(e.getId());        // acao: reservante := e
        l.mudarPara(Reservado.INSTANCIA);
    }

    @Override
    void enviarParaReparo(Livro l) {
        l.mudarPara(Reparando.INSTANCIA);
    }

    @Override
    void excluir(Livro l) {
        // permitido: so um livro LIVRE pode ser excluido do acervo
    }

    @Override
    Situacao situacao() { return Situacao.LIVRE; }
}
