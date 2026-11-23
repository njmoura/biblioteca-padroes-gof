package br.pucpr.biblioteca.banco;

/** Adapter (papel Target, saida). Contrato institucional de cobranca do qual o Financeiro depende. */
public interface GatewayDeCobranca {
    Cobranca emitirBoleto(SolicitacaoCobranca solicitacao);

    SituacaoCobranca consultarSituacao(Cobranca cobranca);
}
