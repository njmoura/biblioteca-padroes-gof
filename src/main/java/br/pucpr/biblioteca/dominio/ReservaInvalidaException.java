package br.pucpr.biblioteca.dominio;

/** Reserva ausente, de outro estudante ou estudante inexistente. */
public class ReservaInvalidaException extends ExcecaoDeDominio {
    public ReservaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
