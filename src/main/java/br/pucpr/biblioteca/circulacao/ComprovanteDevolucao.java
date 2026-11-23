package br.pucpr.biblioteca.circulacao;

import java.time.LocalDate;

public record ComprovanteDevolucao(String idDevolucao, String idLivro, String idEstudante, String cpfEstudante,
                                   LocalDate vencimento, LocalDate devolvidoEm) { }
