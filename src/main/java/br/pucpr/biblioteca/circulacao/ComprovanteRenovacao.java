package br.pucpr.biblioteca.circulacao;

import java.time.LocalDate;

public record ComprovanteRenovacao(String idLivro, String idEstudante, LocalDate novoVencimento, int numRenovacoes) { }
