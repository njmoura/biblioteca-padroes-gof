package br.pucpr.biblioteca.banco;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

/** CPF que nao tem 11 digitos: nao pode ser convertido no documento formatado do banco B. */
public class DocumentoInvalidoException extends ExcecaoDeDominio {
    public DocumentoInvalidoException(String cpf) {
        super("CPF invalido para cobranca: " + cpf);
    }
}
