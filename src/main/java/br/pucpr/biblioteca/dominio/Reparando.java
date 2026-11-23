package br.pucpr.biblioteca.dominio;

/** ConcreteState REPARANDO: volta a LIVRE ou, com laudo irrecuperavel, vai a INCINERADO. */
class Reparando extends EstadoLivro {
    static final Reparando INSTANCIA = new Reparando();

    @Override
    void concluirReparo(Livro l) {
        l.limparCiclo();
        l.mudarPara(Livre.INSTANCIA);
    }

    @Override
    void registrarLaudoIrrecuperavel(Livro l) {
        l.limparCiclo();
        l.mudarPara(Incinerado.INSTANCIA);
    }

    @Override
    Situacao situacao() { return Situacao.REPARANDO; }
}
