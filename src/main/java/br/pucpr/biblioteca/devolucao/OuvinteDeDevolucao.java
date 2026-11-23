package br.pucpr.biblioteca.devolucao;

/** Observer (papel Observer). Contrato de quem reage a uma devolucao confirmada. */
public interface OuvinteDeDevolucao {
    void aoDevolver(EventoDevolucao evento);
}
