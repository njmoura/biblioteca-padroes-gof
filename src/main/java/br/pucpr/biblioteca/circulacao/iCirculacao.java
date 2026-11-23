package br.pucpr.biblioteca.circulacao;

import java.time.LocalDate;

/** Facade (contrato da fachada). Operacoes completas de circulacao; os tratadores delegam a este contrato. */
public interface iCirculacao {
    ComprovanteReserva reservar(String idLivro, String idEstudante, LocalDate data);

    ComprovanteReserva consultarReserva(String idLivro, String idEstudante);

    ComprovanteEmprestimo efetivarEmprestimo(String idLivro, String idEstudante, LocalDate data);

    ComprovanteRenovacao renovar(String idLivro, String idEstudante);

    ComprovanteDevolucao devolver(String idLivro, String idEstudante, LocalDate data);

    ComprovanteDevolucao consultarDevolucao(String idLivro, String idEstudante);

    SituacaoDTO registrarInspecao(String idLivro, boolean emBomEstado);

    SituacaoDTO registrarRetornoDoReparo(String idLivro, boolean reparado);

    SituacaoDTO consultarSituacao(String idLivro);
}
