package br.pucpr.biblioteca.multa;

import java.math.BigDecimal;

/** Strategy (papel Strategy). Contrato de calculo da multa a partir dos dias corridos de atraso. */
public interface PoliticaDeMulta {
    String id();

    BigDecimal calcular(int dias);
}
