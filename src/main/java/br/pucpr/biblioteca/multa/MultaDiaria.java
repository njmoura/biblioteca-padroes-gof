package br.pucpr.biblioteca.multa;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** ConcreteStrategy ORIGINAL: R$ 1,00 por dia de atraso. */
public class MultaDiaria implements PoliticaDeMulta {
    private final BigDecimal valorDia = new BigDecimal("1.00");

    @Override
    public String id() { return "ORIGINAL"; }

    @Override
    public BigDecimal calcular(int dias) {
        int d = Math.max(0, dias);
        return valorDia.multiply(BigDecimal.valueOf(d)).setScale(2, RoundingMode.HALF_UP);
    }
}
