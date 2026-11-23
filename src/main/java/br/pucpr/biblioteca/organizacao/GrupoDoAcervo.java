package br.pucpr.biblioteca.organizacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite (papel Composite). Unidade, secao ou subsecao: mantem os filhos, soma a
 * contagem e junta os ids deles, e protege as invariantes da arvore: sem ciclos, um
 * pai por componente, cada livro em um unico grupo e niveis na ordem
 * UNIDADE > SECAO > SUBSECAO.
 */
public class GrupoDoAcervo extends ComponenteDoAcervo {
    private final TipoDeGrupo tipo;
    private final List<ComponenteDoAcervo> filhos = new ArrayList<>();

    public GrupoDoAcervo(String nome, TipoDeGrupo tipo) {
        super(nome);
        this.tipo = tipo;
    }

    public void adicionar(ComponenteDoAcervo c) {
        if (c == this || ehAncestralDeste(c)) {
            throw new HierarquiaInvalidaException("ciclo: " + c.getNome() + " nao pode ficar dentro de " + getNome());
        }
        if (c.getPai() != null) {
            throw new HierarquiaInvalidaException(c.getNome() + " ja pertence ao grupo " + c.getPai().getNome());
        }
        if (c instanceof GrupoDoAcervo grupo && grupo.tipo.ordinal() < tipo.ordinal()) {
            throw new HierarquiaInvalidaException(grupo.tipo + " " + grupo.getNome() + " nao pode ficar dentro de "
                    + tipo + " " + getNome());
        }
        List<String> jaNaArvore = raizDaArvore().listarIds();
        for (String idLivro : c.listarIds()) {                   // um livro em um unico grupo, em toda a arvore
            if (jaNaArvore.contains(idLivro)) {
                throw new HierarquiaInvalidaException("livro " + idLivro + " ja pertence a um grupo");
            }
        }
        filhos.add(c);
        c.definirPai(this);
    }

    public void remover(ComponenteDoAcervo c) {
        if (filhos.remove(c)) {
            c.definirPai(null);
        }
    }

    @Override
    public int contarLivros() {
        int total = 0;
        for (ComponenteDoAcervo filho : filhos) {
            total += filho.contarLivros();           // grupo ou item: mesma operacao
        }
        return total;
    }

    @Override
    public List<String> listarIds() {
        List<String> ids = new ArrayList<>();
        for (ComponenteDoAcervo filho : filhos) {
            ids.addAll(filho.listarIds());
        }
        return ids;
    }

    @Override
    public ComponenteDoAcervo buscar(String nome) {
        if (getNome().equals(nome)) {
            return this;
        }
        for (ComponenteDoAcervo filho : filhos) {
            ComponenteDoAcervo achado = filho.buscar(nome);
            if (achado != null) {
                return achado;
            }
        }
        return null;
    }

    /** Procura so entre os grupos (este e os de baixo): um livro com id igual ao nome nao atrapalha. */
    public GrupoDoAcervo buscarGrupo(String nome) {
        if (getNome().equals(nome)) {
            return this;
        }
        for (ComponenteDoAcervo filho : filhos) {
            if (filho instanceof GrupoDoAcervo grupo) {
                GrupoDoAcervo achado = grupo.buscarGrupo(nome);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    /** Procura so entre os itens, pelo id do livro. */
    public ItemDoAcervo buscarItem(String idLivro) {
        for (ComponenteDoAcervo filho : filhos) {
            if (filho instanceof ItemDoAcervo item) {
                if (item.getIdLivro().equals(idLivro)) {
                    return item;
                }
            } else if (filho instanceof GrupoDoAcervo grupo) {
                ItemDoAcervo achado = grupo.buscarItem(idLivro);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    public TipoDeGrupo getTipo() { return tipo; }

    /** c e um dos grupos acima deste? Entao adicionar c aqui criaria um ciclo. */
    private boolean ehAncestralDeste(ComponenteDoAcervo c) {
        for (GrupoDoAcervo g = getPai(); g != null; g = g.getPai()) {
            if (g == c) {
                return true;
            }
        }
        return false;
    }

    private GrupoDoAcervo raizDaArvore() {
        GrupoDoAcervo g = this;
        while (g.getPai() != null) {
            g = g.getPai();
        }
        return g;
    }
}
