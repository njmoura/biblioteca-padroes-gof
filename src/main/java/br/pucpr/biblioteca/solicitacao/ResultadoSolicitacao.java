package br.pucpr.biblioteca.solicitacao;

import java.util.UUID;

/**
 * Resultado de uma solicitacao; fica gravado no registro e e reaproveitado nas repeticoes.
 * "repetida" indica que o executor NAO executou de novo: devolveu o resultado ja gravado.
 */
public record ResultadoSolicitacao(UUID id, Status status, String mensagem, boolean repetida) {

    public enum Status { EXECUTADA, RECUSADA, EM_PROCESSAMENTO }

    public static ResultadoSolicitacao executada(UUID id, String mensagem) {
        return new ResultadoSolicitacao(id, Status.EXECUTADA, mensagem, false);
    }

    public static ResultadoSolicitacao recusada(UUID id, String motivo) {
        return new ResultadoSolicitacao(id, Status.RECUSADA, motivo, false);
    }

    public static ResultadoSolicitacao emProcessamento(UUID id) {
        return new ResultadoSolicitacao(id, Status.EM_PROCESSAMENTO,
                "a mesma solicitacao ja esta em execucao; nada foi executado de novo", false);
    }

    /** O mesmo resultado, marcado como resposta a uma submissao repetida do mesmo identificador. */
    public ResultadoSolicitacao comoRepetida() {
        return new ResultadoSolicitacao(id, status, mensagem, true);
    }

    @Override
    public String toString() {
        return (repetida ? "JA PROCESSADA (resultado gravado, nada foi executado de novo): " : "")
                + status + ": " + mensagem + " (solicitacao " + id + ")";
    }
}
