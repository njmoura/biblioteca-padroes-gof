package br.pucpr.biblioteca.organizacao;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

public class HierarquiaInvalidaException extends ExcecaoDeDominio {
    public HierarquiaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
