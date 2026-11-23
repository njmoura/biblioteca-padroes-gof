package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

public class PessoaNaoCadastradaException extends ExcecaoDeDominio {
    public PessoaNaoCadastradaException(String cpf) {
        super("pessoa nao cadastrada: CPF " + cpf);
    }
}
