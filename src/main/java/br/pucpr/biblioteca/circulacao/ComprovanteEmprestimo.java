package br.pucpr.biblioteca.circulacao;

import java.time.LocalDate;

public record ComprovanteEmprestimo(String idLivro, String idEstudante, LocalDate emprestadoEm, LocalDate vencimento) { }
