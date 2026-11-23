package br.pucpr.biblioteca.dominio;

/** Os sete estados do Livro. E este valor que vai para a persistencia. */
public enum Situacao {
    LIVRE, RESERVADO, EMPRESTADO, RENOVADO, DEVOLVIDO, REPARANDO, INCINERADO
}
