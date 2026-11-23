package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.dominio.Livro;
import java.util.List;

/** Abstract Factory (AbstractProduct). */
public interface RepositorioLivro {
    Livro buscar(String id);

    List<Livro> listar();

    void salvar(Livro livro);

    void excluir(String id);

    UnidadeDeTrabalho unidade();
}
