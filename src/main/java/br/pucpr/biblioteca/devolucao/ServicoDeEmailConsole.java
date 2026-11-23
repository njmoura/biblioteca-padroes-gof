package br.pucpr.biblioteca.devolucao;

/** Envio simulado: apenas mostra o e-mail na saida padrao. */
public class ServicoDeEmailConsole implements ServicoDeEmail {
    @Override
    public void enviar(String para, String assunto, String texto) {
        System.out.println("  ouvinte e-mail -> " + para + ": " + assunto + " - " + texto);
    }
}
