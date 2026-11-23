package br.pucpr.biblioteca.devolucao;

/** Envio de e-mail. Pode lancar FalhaDeEnvioException. */
public interface ServicoDeEmail {
    void enviar(String para, String assunto, String texto);
}
