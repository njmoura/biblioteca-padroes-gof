package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

public class ValidacaoException extends ExcecaoDeDominio {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
