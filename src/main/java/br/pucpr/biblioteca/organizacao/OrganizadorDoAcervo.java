package br.pucpr.biblioteca.organizacao;

import java.util.List;

/**
 * Composite (papel Client). Navega a partir da raiz, cria grupos, aloca e move itens.
 * Nao toca em Livro nem em Movimentacao: so conhece o id do livro.
 * Grupos sao procurados pelo nome e itens pelo id do livro, em buscas separadas.
 */
public class OrganizadorDoAcervo {
    private final GrupoDoAcervo raiz;

    public OrganizadorDoAcervo(GrupoDoAcervo raiz) {
        this.raiz = raiz;
    }

    public GrupoDoAcervo getRaiz() { return raiz; }

    public GrupoDoAcervo criarGrupo(String nomeDoPai, String nome, TipoDeGrupo tipo) {
        if (nome == null || nome.isBlank()) {
            throw new HierarquiaInvalidaException("informe o nome do novo grupo");
        }
        if (raiz.buscarGrupo(nome) != null) {
            throw new HierarquiaInvalidaException("ja existe um grupo chamado \"" + nome + "\"");
        }
        GrupoDoAcervo grupo = new GrupoDoAcervo(nome, tipo);
        grupo(nomeDoPai).adicionar(grupo);
        return grupo;
    }

    /** Cada livro pertence a no maximo um grupo (a regra esta em GrupoDoAcervo.adicionar). */
    public ItemDoAcervo alocarLivro(String idLivro, String nomeDoGrupo) {
        if (idLivro == null || idLivro.isBlank()) {
            throw new HierarquiaInvalidaException("informe o id do livro");
        }
        ItemDoAcervo item = new ItemDoAcervo(idLivro);
        grupo(nomeDoGrupo).adicionar(item);
        return item;
    }

    /** Mover = remover da origem + adicionar no destino; se adicionar falhar, o item volta a origem. */
    public void mover(ItemDoAcervo item, GrupoDoAcervo destino) {
        GrupoDoAcervo origem = item.getPai();
        if (origem == null) {
            throw new HierarquiaInvalidaException("livro " + item.getIdLivro() + " nao esta alocado em nenhum grupo");
        }
        origem.remover(item);
        try {
            destino.adicionar(item);
        } catch (HierarquiaInvalidaException e) {
            origem.adicionar(item);
            throw e;
        }
    }

    public void mover(String idLivro, String nomeDoDestino) {
        ItemDoAcervo item = raiz.buscarItem(idLivro);
        if (item == null) {
            throw new HierarquiaInvalidaException("livro " + idLivro + " nao esta alocado em nenhum grupo");
        }
        mover(item, grupo(nomeDoDestino));
    }

    /** O livro saiu do acervo: retira o item da arvore. Se nao estava alocado, nao faz nada. */
    public void desalocar(String idLivro) {
        ItemDoAcervo item = raiz.buscarItem(idLivro);
        if (item != null) {
            item.getPai().remover(item);
        }
    }

    public int contarLivros(String nome) { return componente(nome).contarLivros(); }

    public List<String> listarIds(String nome) { return componente(nome).listarIds(); }

    /** Grupo (pelo nome) ou item (pelo id do livro): os dois respondem a contarLivros e listarIds. */
    private ComponenteDoAcervo componente(String nome) {
        ComponenteDoAcervo c = raiz.buscarGrupo(nome);
        if (c == null) {
            c = raiz.buscarItem(nome);
        }
        if (c == null) {
            throw new HierarquiaInvalidaException("grupo ou livro \"" + nome + "\" nao encontrado");
        }
        return c;
    }

    private GrupoDoAcervo grupo(String nome) {
        GrupoDoAcervo g = raiz.buscarGrupo(nome);
        if (g == null) {
            throw new HierarquiaInvalidaException("grupo \"" + nome + "\" nao encontrado");
        }
        return g;
    }
}
