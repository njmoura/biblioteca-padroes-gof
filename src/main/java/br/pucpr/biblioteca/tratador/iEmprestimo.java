package br.pucpr.biblioteca.tratador;

import java.time.LocalDate;

/** Servicos do Estudante (Trabalho 02): f1 reservar, f2 consultar reserva, f3 emprestar, f4 renovar. */
public interface iEmprestimo {
    Resultado reservarLivro(String idLivro, String idEstudante, LocalDate data);                 // f1

    Resultado consultarReserva(String idLivro, String idEstudante);                              // f2

    Resultado emprestarLivro(String idLivro, String idEstudante, LocalDate data);                // f3

    Resultado renovarEmprestimo(String idLivro, String idEstudante);                             // f4
}
