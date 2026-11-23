package br.pucpr.biblioteca.dominio;

/**
 * ConcreteState DEVOLVIDO: aguarda a decisao da inspecao (aprovar ou reprovar).
 * Nao sobrescreve reservar, logo a reserva e recusada ate a inspecao.
 */
class Devolvido extends EstadoLivro {
    static final Devolvido INSTANCIA = new Devolvido();

    @Override
    void aprovarInspecao(Livro l) {
        l.limparCiclo();                       // acao: limpa vencimento, numRenovacoes e reservante
        l.mudarPara(Livre.INSTANCIA);
    }

    @Override
    void reprovarInspecao(Livro l) {
        l.mudarPara(Reparando.INSTANCIA);      // livro danificado: segue para reparo
    }

    @Override
    Situacao situacao() { return Situacao.DEVOLVIDO; }
}
