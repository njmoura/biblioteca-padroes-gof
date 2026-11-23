package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.Atendente;

/** Manter Atendente (Administrador). Implementada por TratadorDeAtendente. */
public interface iAtendente {
    ResultadoCadastro<Atendente> inserirAtendente(String id, String cpf);

    Atendente consultarAtendente(String id);

    boolean atualizarAtendente(String id, String cpf);

    boolean excluirAtendente(String id);
}
