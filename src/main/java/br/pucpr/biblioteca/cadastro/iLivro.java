package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.tratador.Resultado;

/** Manter Livro (Administrador). Implementada por TratadorDeAcervo. */
public interface iLivro {
    ResultadoCadastro<Livro> inserirLivro(String id, String titulo);

    Livro consultarLivro(String id);

    boolean atualizarLivro(String id, String titulo);

    Resultado excluirLivro(String id);

    Resultado enviarParaReparo(String idLivro);
}
