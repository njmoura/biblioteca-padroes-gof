package br.pucpr.biblioteca.dominio;

/** Recusa de uma regra do dominio. Os tratadores convertem em Resultado de recusa. */
public class ExcecaoDeDominio extends RuntimeException {
    public ExcecaoDeDominio(String mensagem) {
        super(mensagem);
    }
}
