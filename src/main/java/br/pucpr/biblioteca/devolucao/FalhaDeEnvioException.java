package br.pucpr.biblioteca.devolucao;

public class FalhaDeEnvioException extends RuntimeException {
    public FalhaDeEnvioException(String mensagem) {
        super(mensagem);
    }
}
