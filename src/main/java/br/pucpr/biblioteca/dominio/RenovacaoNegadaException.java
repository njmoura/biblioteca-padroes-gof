package br.pucpr.biblioteca.dominio;

/** Terceira renovacao: a guarda numRenovacoes < 2 nao foi satisfeita. */
public class RenovacaoNegadaException extends ExcecaoDeDominio {
    public RenovacaoNegadaException(String idLivro) {
        super("renovacao negada para o livro " + idLivro + ": limite de " + Livro.MAX_RENOVACOES + " renovacoes");
    }
}
