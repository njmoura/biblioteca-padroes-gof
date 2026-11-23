package br.pucpr.biblioteca.dominio;

/** Evento que chegou a um estado do Livro que nao o aceita. */
public class OperacaoInvalidaException extends ExcecaoDeDominio {
    public OperacaoInvalidaException(Situacao situacao, String operacao) {
        super("operacao \"" + operacao + "\" recusada: o livro esta " + situacao);
    }
}
