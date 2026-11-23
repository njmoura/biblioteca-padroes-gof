package br.pucpr.biblioteca.app;

import br.pucpr.biblioteca.cadastro.TratadorDeAcervo;
import br.pucpr.biblioteca.cadastro.TratadorDeAtendente;
import br.pucpr.biblioteca.cadastro.TratadorDeEstudante;
import br.pucpr.biblioteca.cadastro.TratadorDePessoa;
import br.pucpr.biblioteca.tratador.TratadorDeConsulta;
import br.pucpr.biblioteca.tratador.TratadorDeDevolucao;
import br.pucpr.biblioteca.tratador.TratadorDeEmprestimo;
import br.pucpr.biblioteca.tratador.TratadorDeMulta;
import br.pucpr.biblioteca.tratador.TratadorDeOrganizacao;

/**
 * Tudo o que a interface recebe da aplicacao: apenas os tratadores. A tela nao tem como
 * alcancar a fachada, os repositorios nem Acervo, Movimentacao e Relacionamento.
 */
public record Tratadores(TratadorDePessoa pessoa, TratadorDeEstudante estudante, TratadorDeAtendente atendente,
                         TratadorDeAcervo acervo, TratadorDeEmprestimo emprestimo, TratadorDeDevolucao devolucao,
                         TratadorDeMulta multa, TratadorDeConsulta consulta, TratadorDeOrganizacao organizacao) {
}
