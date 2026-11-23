package br.pucpr.biblioteca.multa;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** ConcreteStrategy TOLERANCIA: os dois primeiros dias nao sao cobrados; R$ 1,00 por dia excedente. */
public class MultaComTolerancia implements PoliticaDeMulta {
    private final BigDecimal valorDia = new BigDecimal("1.00");
    private final int toleranciaDias = 2;

    @Override
    public String id() { return "TOLERANCIA"; }

    @Override
    public BigDecimal calcular(int dias) {
        int d = Math.max(0, dias - toleranciaDias);
        return valorDia.multiply(BigDecimal.valueOf(d)).setScale(2, RoundingMode.HALF_UP);
    }
}
