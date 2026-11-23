package br.pucpr.biblioteca.banco;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import java.math.BigDecimal;

/** Valor com mais de duas casas decimais: nao pode ser convertido em centavos sem perda. */
public class ValorInvalidoException extends ExcecaoDeDominio {
    public ValorInvalidoException(BigDecimal valor) {
        super("valor invalido para cobranca: " + valor);
    }
}
