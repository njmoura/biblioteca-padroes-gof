package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.organizacao.GrupoDoAcervo;
import br.pucpr.biblioteca.organizacao.HierarquiaInvalidaException;
import br.pucpr.biblioteca.organizacao.ItemDoAcervo;
import br.pucpr.biblioteca.organizacao.OrganizadorDoAcervo;
import br.pucpr.biblioteca.organizacao.TipoDeGrupo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Requisito 10 - Organizacao hierarquica do acervo (Composite). */
class R10CompositeTest {
    private final GrupoDoAcervo unidade = new GrupoDoAcervo("Unidade Centro", TipoDeGrupo.UNIDADE);
    private final GrupoDoAcervo secaoA = new GrupoDoAcervo("Secao A", TipoDeGrupo.SECAO);
    private final GrupoDoAcervo secaoB = new GrupoDoAcervo("Secao B", TipoDeGrupo.SECAO);

    private void montarTresEDois() {
        unidade.adicionar(secaoA);
        unidade.adicionar(secaoB);
        secaoA.adicionar(new ItemDoAcervo("L1"));
        secaoA.adicionar(new ItemDoAcervo("L2"));
        secaoA.adicionar(new ItemDoAcervo("L3"));
        secaoB.adicionar(new ItemDoAcervo("L4"));
        secaoB.adicionar(new ItemDoAcervo("L5"));
    }

    @Test
    void R10_unidadeComSecoesDeTresEDoisLivrosInformaCinco() {
        montarTresEDois();

        assertEquals(5, unidade.contarLivros());
        assertEquals(3, secaoA.contarLivros());
        assertEquals(2, secaoB.contarLivros());
        assertEquals(1, new ItemDoAcervo("L9").contarLivros());               // grupo e item: mesmas operacoes
        assertEquals(List.of("L1", "L2", "L3", "L4", "L5"), unidade.listarIds());
        assertEquals(List.of("L9"), new ItemDoAcervo("L9").listarIds());
    }

    @Test
    void R10_profundidadeVariavelComSubsecao() {
        montarTresEDois();
        GrupoDoAcervo subsecao = new GrupoDoAcervo("Subsecao A1", TipoDeGrupo.SUBSECAO);
        secaoA.adicionar(subsecao);
        subsecao.adicionar(new ItemDoAcervo("L6"));

        assertEquals(6, unidade.contarLivros());
        assertEquals(4, secaoA.contarLivros());
    }

    @Test
    void R10_moverLivroEntreSecoesNaoAlteraIdEstadoNemEmprestimo() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        String raiz = app.tratadorDeOrganizacao.raiz();
        app.tratadorDeOrganizacao.criarGrupo(raiz, "Secao A", TipoDeGrupo.SECAO);
        app.tratadorDeOrganizacao.criarGrupo(raiz, "Secao B", TipoDeGrupo.SECAO);
        app.tratadorDeOrganizacao.alocarLivro("L1", "Secao A");
        app.tratadorDeOrganizacao.alocarLivro("L2", "Secao A");
        Livro antes = app.acervo.consultar("L1");

        assertTrue(app.tratadorDeOrganizacao.moverLivro("L1", "Secao B").sucesso());

        Livro depois = app.acervo.consultar("L1");
        assertEquals(antes.getId(), depois.getId());                          // id
        assertEquals(Situacao.EMPRESTADO, depois.getSituacao());              // estado
        assertEquals(antes.getVencimento(), depois.getVencimento());
        assertNotNull(app.movimentacao.consultarEmprestimo("L1", "E1"));      // emprestimo
        assertEquals(List.of("L2"), app.organizador.listarIds("Secao A"));
        assertEquals(List.of("L1"), app.organizador.listarIds("Secao B"));
        assertEquals(2, app.organizador.contarLivros(raiz));                  // a unidade continua com os mesmos livros
    }

    @Test
    void R10_grupoRecusaCicloELivroQueJaPertenceAOutroGrupo() {
        montarTresEDois();
        ItemDoAcervo itemDeA = (ItemDoAcervo) unidade.buscar("L1");

        assertThrows(HierarquiaInvalidaException.class, () -> secaoA.adicionar(unidade));   // ciclo: ancestral
        assertThrows(HierarquiaInvalidaException.class, () -> secaoA.adicionar(secaoA));    // ciclo: ele mesmo
        assertThrows(HierarquiaInvalidaException.class, () -> secaoB.adicionar(itemDeA));   // ja tem pai
        assertSame(secaoA, itemDeA.getPai());                                               // arvore nao foi alterada
        assertEquals(5, unidade.contarLivros());

        // o proprio grupo recusa um livro que ja esta em outro grupo, mesmo vindo como um item novo
        assertThrows(HierarquiaInvalidaException.class, () -> secaoB.adicionar(new ItemDoAcervo("L1")));
        assertEquals(5, unidade.contarLivros());
        assertEquals(2, secaoB.contarLivros());

        // ciclo por um nivel a mais: a secao nao pode ir para dentro de uma subsecao dela
        GrupoDoAcervo subsecao = new GrupoDoAcervo("Subsecao A1", TipoDeGrupo.SUBSECAO);
        secaoA.adicionar(subsecao);
        assertThrows(HierarquiaInvalidaException.class, () -> subsecao.adicionar(secaoA));
        assertThrows(HierarquiaInvalidaException.class, () -> subsecao.adicionar(unidade));

        OrganizadorDoAcervo organizador = new OrganizadorDoAcervo(unidade);
        assertThrows(HierarquiaInvalidaException.class, () -> organizador.alocarLivro("L1", "Secao B"));   // livro em dois grupos
        organizador.mover(itemDeA, secaoB);                                                 // mover = remover + adicionar
        assertSame(secaoB, itemDeA.getPai());
        assertFalse(secaoA.listarIds().contains("L1"));
    }

    @Test
    void R10_niveisSeguemAOrdemUnidadeSecaoSubsecao() {
        montarTresEDois();
        GrupoDoAcervo subsecao = new GrupoDoAcervo("Subsecao A1", TipoDeGrupo.SUBSECAO);
        secaoA.adicionar(subsecao);

        assertThrows(HierarquiaInvalidaException.class,
                () -> subsecao.adicionar(new GrupoDoAcervo("Outra unidade", TipoDeGrupo.UNIDADE)));
        assertThrows(HierarquiaInvalidaException.class,
                () -> subsecao.adicionar(new GrupoDoAcervo("Outra secao", TipoDeGrupo.SECAO)));
        subsecao.adicionar(new GrupoDoAcervo("Subsecao A1a", TipoDeGrupo.SUBSECAO));   // mesmo nivel: profundidade variavel
        assertEquals(5, unidade.contarLivros());
    }

    @Test
    void R10_grupoComNomeDeLivroNaoEscondeOLivroELivroExcluidoSaiDaArvore() {
        montarTresEDois();
        OrganizadorDoAcervo organizador = new OrganizadorDoAcervo(unidade);

        organizador.criarGrupo("Unidade Centro", "L1", TipoDeGrupo.SECAO);    // grupo com o mesmo texto de um id de livro
        organizador.mover("L1", "L1");                                        // o livro L1 vai para o grupo L1

        assertEquals(List.of("L1"), organizador.listarIds("L1"));
        assertEquals(5, unidade.contarLivros());
        assertThrows(HierarquiaInvalidaException.class, () -> organizador.criarGrupo("Unidade Centro", "L1", TipoDeGrupo.SECAO));
        assertThrows(HierarquiaInvalidaException.class, () -> organizador.criarGrupo("Unidade Centro", " ", TipoDeGrupo.SECAO));
        assertThrows(HierarquiaInvalidaException.class, () -> organizador.mover(new ItemDoAcervo("L9"), secaoB));   // item solto

        organizador.desalocar("L5");                                          // o livro foi excluido do acervo
        organizador.desalocar("L5");                                          // repetir nao faz nada
        assertEquals(4, unidade.contarLivros());
        assertEquals(List.of("L4"), secaoB.listarIds());
    }

    @Test
    void R10_livroExcluidoPeloTratadorSaiDaHierarquia() {
        Aplicacao app = Cenario.novo();
        String raiz = app.tratadorDeOrganizacao.raiz();
        app.tratadorDeOrganizacao.alocarLivro("L1", raiz);
        app.tratadorDeOrganizacao.alocarLivro("L2", raiz);

        assertTrue(app.tratadorDeAcervo.excluirLivro("L2").sucesso());

        assertEquals(1, app.organizador.contarLivros(raiz));               // o item do livro excluido saiu da arvore
        assertEquals(List.of("L1"), app.organizador.listarIds(raiz));
    }

    @Test
    void R10_telaInformaOTipoComoTextoEOTratadorConfere() {
        Aplicacao app = Cenario.novo();
        String raiz = app.tratadorDeOrganizacao.raiz();

        assertTrue(app.tratadorDeOrganizacao.criarGrupo(raiz, "Literatura", "secao").sucesso());
        assertFalse(app.tratadorDeOrganizacao.criarGrupo(raiz, "Poesia", "prateleira").sucesso());
        assertTrue(app.tratadorDeOrganizacao.alocarLivro("L1", "Literatura").sucesso());
        assertTrue(app.tratadorDeOrganizacao.alocarLivro("L2", "Literatura").sucesso());
        assertEquals("OK: Literatura: 2 livros (L1, L2)", app.tratadorDeOrganizacao.contarEListar("Literatura").toString());
    }
}
