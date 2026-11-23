package br.pucpr.biblioteca.multa;

import br.pucpr.biblioteca.banco.Cobranca;
import br.pucpr.biblioteca.banco.Moeda;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Multa emitida. O valor e a politica aplicada sao gravados na emissao; uma troca de
 * politica depois disso nao recalcula a multa.
 */
public class Multa {
    private final String idDevolucao;
    private final String cpf;
    private final int diasAtraso;
    private final BigDecimal valor;
    private final String politicaAplicada;
    private final LocalDate emitidaEm;
    private Cobranca cobranca;

    public Multa(String idDevolucao, String cpf, int diasAtraso, BigDecimal valor, String politicaAplicada, LocalDate emitidaEm) {
        this.idDevolucao = idDevolucao;
        this.cpf = cpf;
        this.diasAtraso = diasAtraso;
        this.valor = valor;
        this.politicaAplicada = politicaAplicada;
        this.emitidaEm = emitidaEm;
    }

    public String getIdDevolucao() { return idDevolucao; }

    public String getCpf() { return cpf; }

    public int getDiasAtraso() { return diasAtraso; }

    public BigDecimal getValor() { return valor; }

    public String getPoliticaAplicada() { return politicaAplicada; }

    public LocalDate getEmitidaEm() { return emitidaEm; }

    public Cobranca getCobranca() { return cobranca; }

    void definirCobranca(Cobranca cobranca) { this.cobranca = cobranca; }

    @Override
    public String toString() {
        return "Multa " + Moeda.reais(valor) + " (" + diasAtraso + (diasAtraso == 1 ? " dia" : " dias")
                + " de atraso, politica " + politicaAplicada + "), CPF " + cpf + (cobranca != null ? ", " + cobranca : "");
    }
}
