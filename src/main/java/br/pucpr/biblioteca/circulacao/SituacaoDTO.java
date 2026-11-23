package br.pucpr.biblioteca.circulacao;

import br.pucpr.biblioteca.dominio.Situacao;
import java.time.LocalDate;

public record SituacaoDTO(String idLivro, String titulo, Situacao situacao, LocalDate vencimento, int numRenovacoes) { }
