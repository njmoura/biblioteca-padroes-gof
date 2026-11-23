package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;

/** Pedido de cobranca no formato institucional (valor decimal em reais, CPF so com digitos). */
public record SolicitacaoCobranca(String idCobranca, String cpf, BigDecimal valor) { }
