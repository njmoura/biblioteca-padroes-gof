package br.pucpr.biblioteca.multa;

import java.time.LocalDate;
import java.util.List;

/** Interface fornecida pelo Financeiro a Biblioteca (Trabalho 02): emissao e consulta de multas. */
public interface iMulta {
    /** Devolve a multa emitida, ou null quando o valor calculado e zero. */
    Multa emitirMulta(String idDevolucao, String cpf, int diasAtraso, LocalDate dataEmissao);

    Multa consultarMulta(String idDevolucao);

    List<Multa> listarMultas();
}
