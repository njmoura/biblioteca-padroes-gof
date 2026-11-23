package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.Estudante;

/** Manter Estudante (Administrador). Implementada por TratadorDeEstudante. */
public interface iEstudante {
    ResultadoCadastro<Estudante> inserirEstudante(String id, String cpf);

    Estudante consultarEstudante(String id);

    boolean atualizarEstudante(String id, String cpf);

    boolean excluirEstudante(String id);
}
