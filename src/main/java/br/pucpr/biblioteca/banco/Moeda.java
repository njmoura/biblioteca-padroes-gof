package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Apresentacao de valores monetarios: sempre "R$ 5,00" (duas casas, virgula decimal). */
public final class Moeda {
    private Moeda() { }

    public static String reais(BigDecimal valor) {
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
