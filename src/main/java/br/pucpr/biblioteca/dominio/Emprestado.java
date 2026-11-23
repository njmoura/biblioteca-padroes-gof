package br.pucpr.biblioteca.dominio;

/** ConcreteState EMPRESTADO: aceita renovar e devolver. */
class Emprestado extends EstadoLivro {
    static final Emprestado INSTANCIA = new Emprestado();

    /** Primeira renovacao: EMPRESTADO -> RENOVADO. */
    @Override
    void renovar(Livro l) {
        if (l.getNumRenovacoes() >= Livro.MAX_RENOVACOES) {    // guarda: numRenovacoes < 2
            throw new RenovacaoNegadaException(l.getId());     // avaliada ANTES de alterar o vencimento
        }
        l.prorrogar(Livro.DIAS_POR_RENOVACAO);                 // acao: vencimento += 30; numRenovacoes++
        l.mudarPara(Renovado.INSTANCIA);
    }

    @Override
    void devolver(Livro l) {
        l.mudarPara(Devolvido.INSTANCIA);
    }

    @Override
    Situacao situacao() { return Situacao.EMPRESTADO; }
}
