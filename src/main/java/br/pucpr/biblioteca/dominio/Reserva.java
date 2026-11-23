package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

public class Reserva extends Transacao {
    public Reserva(LocalDate data, String idLivro, String idEstudante) {
        super(data, idLivro, idEstudante);
    }
}
