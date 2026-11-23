package br.pucpr.biblioteca.devolucao;

import br.pucpr.biblioteca.circulacao.ComprovanteDevolucao;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Observer (estado notificado, modelo push). Objeto de valor imutavel: uma fotografia do
 * momento da devolucao, com tudo que os ouvintes precisam. Assim nenhum ouvinte depende do
 * Acervo nem da Movimentacao, nem do vencimento do Livro, que e apagado na inspecao.
 */
public record EventoDevolucao(String idDevolucao, String idLivro, String idEstudante, String cpfEstudante,
                              LocalDate vencimento, LocalDate devolvidoEm, Instant ocorridoEm) {

    public EventoDevolucao(ComprovanteDevolucao c) {
        this(c.idDevolucao(), c.idLivro(), c.idEstudante(), c.cpfEstudante(), c.vencimento(), c.devolvidoEm(), Instant.now());
    }

    /** Dias corridos inteiros de atraso; nunca negativo. */
    public int diasAtraso() {
        return (int) Math.max(0, ChronoUnit.DAYS.between(vencimento, devolvidoEm));
    }
}
