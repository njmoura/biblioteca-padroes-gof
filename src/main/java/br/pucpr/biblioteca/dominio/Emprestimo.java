package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/** Emprestimo ativo. Vencimento e numero de renovacoes ficam no Livro (dados do ciclo atual). */
public class Emprestimo extends Transacao {
    public Emprestimo(LocalDate data, String idLivro, String idEstudante) {
        super(data, idLivro, idEstudante);
    }
}
