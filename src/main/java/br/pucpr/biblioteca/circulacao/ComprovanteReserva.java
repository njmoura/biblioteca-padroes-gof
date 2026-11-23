package br.pucpr.biblioteca.circulacao;

import java.time.LocalDate;

/** DTO devolvido pela fachada: a interface nunca recebe colecoes nem objetos internos. */
public record ComprovanteReserva(String idLivro, String idEstudante, LocalDate reservadoEm) { }
