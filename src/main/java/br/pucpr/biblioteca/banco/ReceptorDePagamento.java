package br.pucpr.biblioteca.banco;

/** Adapter (papel Target, entrada). Porta por onde os adaptadores entregam as confirmacoes de pagamento. */
public interface ReceptorDePagamento {
    void confirmar(ConfirmacaoPagamento confirmacao);
}
