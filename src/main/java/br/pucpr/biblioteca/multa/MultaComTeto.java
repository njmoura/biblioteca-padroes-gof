package br.pucpr.biblioteca.multa;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** ConcreteStrategy TETO: R$ 1,00 por dia, limitado a R$ 20,00. */
public class MultaComTeto implements PoliticaDeMulta {
    private final BigDecimal valorDia = new BigDecimal("1.00");
    private final BigDecimal teto = new BigDecimal("20.00");

    @Override
    public String id() { return "TETO"; }

    @Override
    public BigDecimal calcular(int dias) {
        int d = Math.max(0, dias);
        BigDecimal valor = valorDia.multiply(BigDecimal.valueOf(d));
        return valor.min(teto).setScale(2, RoundingMode.HALF_UP);
    }
}
