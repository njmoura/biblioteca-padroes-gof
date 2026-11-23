package br.pucpr.biblioteca.consulta;

/** Unica informacao que a consulta publica (e o cache) guarda: id e titulo. Nunca a disponibilidade. */
public record MetadadosLivro(String id, String titulo) { }
