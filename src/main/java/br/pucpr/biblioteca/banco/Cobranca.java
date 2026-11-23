package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;

/** Cobranca institucional: liga o identificador interno ao identificador externo do banco. */
public class Cobranca {
    private final String idCobranca;
    private final CodigoBanco banco;
    private final String idExterno;
    private final BigDecimal valor;
    private final String linhaDigitavel;
    private SituacaoCobranca situacao = SituacaoCobranca.ABERTA;
    private String idPagamento;

    public Cobranca(String idCobranca, CodigoBanco banco, String idExterno, BigDecimal valor, String linhaDigitavel) {
        this.idCobranca = idCobranca;
        this.banco = banco;
        this.idExterno = idExterno;
        this.valor = valor;
        this.linhaDigitavel = linhaDigitavel;
    }

    public String getIdCobranca() { return idCobranca; }

    public CodigoBanco getBanco() { return banco; }

    public String getIdExterno() { return idExterno; }

    public BigDecimal getValor() { return valor; }

    public String getLinhaDigitavel() { return linhaDigitavel; }

    public SituacaoCobranca getSituacao() { return situacao; }

    public String getIdPagamento() { return idPagamento; }

    void quitar(String idPagamento) {
        this.situacao = SituacaoCobranca.QUITADA;
        this.idPagamento = idPagamento;
    }

    @Override
    public String toString() {
        return "boleto " + idExterno + " do banco " + banco + " [" + situacao + "]";
    }
}
