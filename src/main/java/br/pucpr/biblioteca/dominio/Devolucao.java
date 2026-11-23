package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/** Devolucao registrada. Guarda o vencimento, pois o emprestimo ativo e removido na devolucao. */
public class Devolucao extends Transacao {
    private final String id;
    private final LocalDate vencimento;

    public Devolucao(String id, LocalDate data, String idLivro, String idEstudante, LocalDate vencimento) {
        super(data, idLivro, idEstudante);
        this.id = id;
        this.vencimento = vencimento;
    }

    public String getId() { return id; }

    public LocalDate getVencimento() { return vencimento; }
}
