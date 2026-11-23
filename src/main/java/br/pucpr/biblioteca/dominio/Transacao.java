package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/** Reserva, Emprestimo e Devolucao sao casos particulares de transacao. Sao imutaveis. */
public abstract class Transacao {
    private final LocalDate data;
    private final String idLivro;
    private final String idEstudante;

    protected Transacao(LocalDate data, String idLivro, String idEstudante) {
        this.data = data;
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
    }

    public LocalDate getData() { return data; }

    public String getIdLivro() { return idLivro; }

    public String getIdEstudante() { return idEstudante; }
}
