package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.persistencia.ConfiguradorDePersistencia;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.FamiliaIncompativelException;
import br.pucpr.biblioteca.persistencia.Perfil;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.persistencia.memoria.RepositorioLivroEmMemoria;
import br.pucpr.biblioteca.persistencia.memoria.RepositorioMovimentacaoEmMemoria;
import br.pucpr.biblioteca.persistencia.memoria.RepositorioRelacionamentoEmMemoria;
import br.pucpr.biblioteca.persistencia.memoria.UnidadeDeTrabalhoEmMemoria;
import br.pucpr.biblioteca.persistencia.mysql.FabricaMySQL;
import br.pucpr.biblioteca.persistencia.mysql.RepositorioLivroMySQL;
import br.pucpr.biblioteca.persistencia.mysql.RepositorioMovimentacaoMySQL;
import br.pucpr.biblioteca.persistencia.mysql.RepositorioRelacionamentoMySQL;
import br.pucpr.biblioteca.persistencia.mysql.UnidadeDeTrabalhoMySQL;
import org.junit.jupiter.api.Test;

/** Requisito 8 - Familias compativeis de acesso aos dados (Abstract Factory). */
class R08AbstractFactoryTest {
    private final ConfiguradorDePersistencia configurador = new ConfiguradorDePersistencia();

    @Test
    void R08_configuracaoDeTesteForneceApenasObjetosEmMemoria() {
        FabricaDePersistencia fabrica = configurador.fabricaPara(Perfil.TESTE);

        UnidadeDeTrabalho uow = fabrica.criarUnidadeDeTrabalho();
        RepositorioLivro livros = fabrica.criarRepositorioLivro(uow);
        RepositorioMovimentacao movimentacao = fabrica.criarRepositorioMovimentacao(uow);
        RepositorioRelacionamento relacionamento = fabrica.criarRepositorioRelacionamento(uow);

        assertInstanceOf(FabricaEmMemoria.class, fabrica);
        assertInstanceOf(UnidadeDeTrabalhoEmMemoria.class, uow);
        assertInstanceOf(RepositorioLivroEmMemoria.class, livros);
        assertInstanceOf(RepositorioMovimentacaoEmMemoria.class, movimentacao);
        assertInstanceOf(RepositorioRelacionamentoEmMemoria.class, relacionamento);
    }

    @Test
    void R08_configuracaoDeProducaoForneceRepositoriosMySQLNaMesmaUnidadeDeTrabalho() {
        FabricaDePersistencia fabrica = configurador.fabricaPara(Perfil.PRODUCAO);

        UnidadeDeTrabalho uow = fabrica.criarUnidadeDeTrabalho();              // a conexao so abre no primeiro uso
        RepositorioLivro livros = fabrica.criarRepositorioLivro(uow);
        RepositorioMovimentacao movimentacao = fabrica.criarRepositorioMovimentacao(uow);
        RepositorioRelacionamento relacionamento = fabrica.criarRepositorioRelacionamento(uow);

        assertInstanceOf(FabricaMySQL.class, fabrica);
        assertInstanceOf(UnidadeDeTrabalhoMySQL.class, uow);
        assertInstanceOf(RepositorioLivroMySQL.class, livros);
        assertInstanceOf(RepositorioMovimentacaoMySQL.class, movimentacao);
        assertInstanceOf(RepositorioRelacionamentoMySQL.class, relacionamento);
        assertSame(uow, livros.unidade());                                    // os tres compartilham a mesma unidade
        assertSame(uow, movimentacao.unidade());
        assertSame(uow, relacionamento.unidade());
    }

    @Test
    void R08_fabricaRecusaUnidadeDeTrabalhoDeOutraFamilia() {
        FabricaDePersistencia memoria = configurador.fabricaPara(Perfil.TESTE);
        FabricaDePersistencia mysql = configurador.fabricaPara(Perfil.PRODUCAO);

        assertThrows(FamiliaIncompativelException.class, () -> mysql.criarRepositorioLivro(memoria.criarUnidadeDeTrabalho()));
        assertThrows(FamiliaIncompativelException.class, () -> memoria.criarRepositorioMovimentacao(mysql.criarUnidadeDeTrabalho()));
    }

    @Test
    void R08_perfilAusenteOuDesconhecidoInterrompeAInicializacao() {
        assertThrows(IllegalArgumentException.class, () -> Perfil.de(null));
        assertThrows(IllegalArgumentException.class, () -> Perfil.de("HOMOLOGACAO"));
        assertThrows(IllegalArgumentException.class, () -> configurador.fabricaPara(null));
        assertEquals(Perfil.PRODUCAO, Perfil.de("producao"));
    }

    /**
     * Exercita o SQL da familia relacional sem servidor: a mesma FabricaMySQL aponta para um
     * banco H2 embutido em modo de compatibilidade MySQL (so nos testes).
     */
    @Test
    void R08_familiaRelacionalGravaLeEDesfazNaMesmaTransacao() {
        FabricaDePersistencia fabrica = new FabricaMySQL("jdbc:h2:mem:r08;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        Aplicacao app = Cenario.novo(fabrica, CodigoBanco.A, (para, assunto, texto) -> { });

        assertTrue(app.tratadorDeEmprestimo.reservarLivro("L1", "E1", Cenario.DIA).sucesso());
        assertTrue(app.tratadorDeEmprestimo.emprestarLivro("L1", "E1", Cenario.DIA).sucesso());
        assertTrue(app.tratadorDeEmprestimo.renovarEmprestimo("L1", "E1").sucesso());

        Livro lido = app.acervo.consultar("L1");                              // reconstituido a partir da tabela
        assertEquals(Situacao.RENOVADO, lido.getSituacao());
        assertEquals(Cenario.VENCIMENTO.plusDays(30), lido.getVencimento());
        assertEquals(1, lido.getNumRenovacoes());
        assertNull(app.movimentacao.consultarReserva("L1", "E1"));
        assertNotNull(app.movimentacao.consultarEmprestimo("L1", "E1"));

        // operacao recusada (a reserva e de outro estudante): nada muda
        app.tratadorDeEmprestimo.reservarLivro("L2", "E1", Cenario.DIA);
        app.tratadorDeEmprestimo.emprestarLivro("L2", "E2", Cenario.DIA);     // reserva e de outro estudante
        assertEquals(Situacao.RESERVADO, app.acervo.consultar("L2").getSituacao());
        assertNotNull(app.movimentacao.consultarReserva("L2", "E1"));

        assertTrue(app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(35)).sucesso());
        assertEquals(Situacao.DEVOLVIDO, app.acervo.consultar("L1").getSituacao());
        assertEquals(5, app.servicoDeMulta.listarMultas().get(0).getDiasAtraso());
    }

    /** Dois repositorios diferentes escrevem pela mesma unidade de trabalho: um ROLLBACK desfaz os dois, um COMMIT grava os dois. */
    @Test
    void R08_repositoriosRelacionaisCompartilhamAMesmaTransacao() {
        FabricaDePersistencia fabrica = new FabricaMySQL("jdbc:h2:mem:r08tx;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        UnidadeDeTrabalho uow = fabrica.criarUnidadeDeTrabalho();
        RepositorioLivro livros = fabrica.criarRepositorioLivro(uow);
        RepositorioRelacionamento relacionamento = fabrica.criarRepositorioRelacionamento(uow);

        uow.iniciar();
        livros.salvar(new Livro("L1", "Dom Casmurro"));
        relacionamento.salvarPessoa(new Pessoa(Cenario.CPF_ANA, "Ana", "Silva", "ana@exemplo.br"));
        assertNotNull(livros.buscar("L1"));                                   // visivel dentro da transacao
        uow.desfazer();

        assertNull(livros.buscar("L1"));                                      // o ROLLBACK unico desfez as duas escritas
        assertNull(relacionamento.buscarPessoa(Cenario.CPF_ANA));

        uow.iniciar();
        livros.salvar(new Livro("L1", "Dom Casmurro"));
        relacionamento.salvarPessoa(new Pessoa(Cenario.CPF_ANA, "Ana", "Silva", "ana@exemplo.br"));
        uow.confirmar();

        assertNotNull(livros.buscar("L1"));                                   // o COMMIT unico gravou as duas
        assertNotNull(relacionamento.buscarPessoa(Cenario.CPF_ANA));
        UnidadeDeTrabalho outraConexao = fabrica.criarUnidadeDeTrabalho();     // outra conexao enxerga o que foi confirmado
        assertNotNull(fabrica.criarRepositorioLivro(outraConexao).buscar("L1"));
    }
}
