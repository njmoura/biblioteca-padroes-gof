package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.Pessoa;

/** Manter Pessoa (Administrador). Implementada por TratadorDePessoa. */
public interface iPessoa {
    ResultadoCadastro<Pessoa> inserirPessoa(String cpf, String nome, String sobrenome, String email);

    Pessoa consultarPessoa(String cpf);

    boolean atualizarPessoa(String cpf, String nome, String sobrenome, String email);

    boolean excluirPessoa(String cpf);
}
