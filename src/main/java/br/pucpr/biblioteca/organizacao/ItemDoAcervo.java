package br.pucpr.biblioteca.organizacao;

import java.util.List;

/** Composite (papel Leaf). Representa um exemplar apenas pelo id do Livro; nao guarda nem altera o Livro. */
public class ItemDoAcervo extends ComponenteDoAcervo {
    private final String idLivro;

    public ItemDoAcervo(String idLivro) {
        super(idLivro);
        this.idLivro = idLivro;
    }

    @Override
    public int contarLivros() { return 1; }

    @Override
    public List<String> listarIds() { return List.of(idLivro); }

    public String getIdLivro() { return idLivro; }
}
