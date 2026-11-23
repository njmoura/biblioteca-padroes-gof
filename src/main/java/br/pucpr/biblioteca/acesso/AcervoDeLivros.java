package br.pucpr.biblioteca.acesso;

import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import java.util.List;

/** Ponto interno de acesso aos livros (Trabalho 02). Guarda os livros pelo repositorio da familia escolhida. */
public class AcervoDeLivros implements iAcervo {
    private final RepositorioLivro repositorio;

    public AcervoDeLivros(RepositorioLivro repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void inserir(Livro livro) { repositorio.salvar(livro); }

    @Override
    public Livro consultar(String id) { return repositorio.buscar(id); }

    @Override
    public void atualizar(Livro livro) { repositorio.salvar(livro); }

    @Override
    public void excluir(Livro livro) { repositorio.excluir(livro.getId()); }

    @Override
    public List<Livro> listar() { return repositorio.listar(); }
}
