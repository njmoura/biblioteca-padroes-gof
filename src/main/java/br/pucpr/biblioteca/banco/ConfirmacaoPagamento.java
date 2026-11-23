package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;

/** Notificacao de pagamento ja traduzida pelo adaptador para o formato institucional. */
public record ConfirmacaoPagamento(CodigoBanco banco, String idExterno, String idPagamento, BigDecimal valorPago) { }
