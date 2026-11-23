package br.pucpr.biblioteca.tratador;

import java.time.LocalDate;

/** Servicos do Atendente (Trabalho 02): f5 registrar devolucao, f6 emitir boleto de multa, inspecao e retorno do reparo. */
public interface iDevolucao {
    Resultado registrarDevolucao(String idLivro, String idEstudante, LocalDate data);            // f5

    Resultado emitirBoletoMulta(String idLivro, String idEstudante);                             // f6

    Resultado inspecionarLivro(String idLivro, boolean emBomEstado);

    Resultado registrarRetornoDoReparo(String idLivro, boolean reparado);
}
