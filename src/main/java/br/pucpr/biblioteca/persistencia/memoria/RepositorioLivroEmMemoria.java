package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.util.ArrayList;
import java.util.List;

/**
 * ConcreteProduct da familia em memoria. Entrega e guarda COPIAS do Livro, como um
 * banco faria: alterar o objeto so vale depois de salvar(), e desfazer() funciona.
 */
public class RepositorioLivroEmMemoria implements RepositorioLivro {
    private final UnidadeDeTrabalhoEmMemoria uow;

    RepositorioLivroEmMemoria(UnidadeDeTrabalhoEmMemoria uow) {
        this.uow = uow;
    }

    @Override
    public Livro buscar(String id) {
        Livro l = uow.base().livros.get(id);
        return l == null ? null : l.copia();
    }

    @Override
    public List<Livro> listar() {
        List<Livro> lista = new ArrayList<>();
        for (Livro l : uow.base().livros.values()) {
            lista.add(l.copia());
        }
        return lista;
    }

    @Override
    public void salvar(Livro livro) {
        uow.base().livros.put(livro.getId(), livro.copia());
    }

    @Override
    public void excluir(String id) {
        uow.base().livros.remove(id);
    }

    @Override
    public UnidadeDeTrabalho unidade() { return uow; }
}
