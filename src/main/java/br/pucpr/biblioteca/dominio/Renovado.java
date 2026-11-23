package br.pucpr.biblioteca.dominio;

/**
 * ConcreteState RENOVADO: emprestimo que ja foi renovado ao menos uma vez.
 * Aceita renovar (enquanto numRenovacoes < 2, permanecendo RENOVADO) e devolver.
 */
class Renovado extends EstadoLivro {
    static final Renovado INSTANCIA = new Renovado();

    @Override
    void renovar(Livro l) {
        if (l.getNumRenovacoes() >= Livro.MAX_RENOVACOES) {    // guarda: numRenovacoes < 2
            throw new RenovacaoNegadaException(l.getId());     // avaliada ANTES de alterar o vencimento
        }
        l.prorrogar(Livro.DIAS_POR_RENOVACAO);                 // acao: vencimento += 30; numRenovacoes++
        // permanece RENOVADO (auto-transicao)
    }

    @Override
    void devolver(Livro l) {
        l.mudarPara(Devolvido.INSTANCIA);
    }

    @Override
    Situacao situacao() { return Situacao.RENOVADO; }
}
