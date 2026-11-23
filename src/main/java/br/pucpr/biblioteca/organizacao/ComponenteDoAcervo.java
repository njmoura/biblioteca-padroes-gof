package br.pucpr.biblioteca.organizacao;

import java.util.List;

/** Composite (papel Component). Operacoes uniformes para grupos e itens, e referencia ao pai (0..1). */
public abstract class ComponenteDoAcervo {
    private final String nome;
    private GrupoDoAcervo pai;

    protected ComponenteDoAcervo(String nome) {
        this.nome = nome;
    }

    public abstract int contarLivros();

    public abstract List<String> listarIds();

    /** Procura, a partir deste no, o componente com o nome dado. */
    public ComponenteDoAcervo buscar(String nome) {
        return this.nome.equals(nome) ? this : null;
    }

    public String getNome() { return nome; }

    public GrupoDoAcervo getPai() { return pai; }

    void definirPai(GrupoDoAcervo pai) { this.pai = pai; }
}
