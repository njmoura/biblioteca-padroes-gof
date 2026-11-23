package br.pucpr.biblioteca.acesso;

import br.pucpr.biblioteca.dominio.Livro;
import java.util.List;

/** Interface do Acervo (Trabalho 02): incluir, consultar, atualizar e excluir um livro. */
public interface iAcervo {
    void inserir(Livro livro);

    Livro consultar(String id);

    void atualizar(Livro livro);

    void excluir(Livro livro);

    List<Livro> listar();
}
