package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.acesso.AcervoDeLivros;
import br.pucpr.biblioteca.acesso.Movimentacao;
import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.circulacao.ServicoDeCirculacao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.tratador.Resultado;
import org.junit.jupiter.api.Test;

/** Requisito 4 - Entrada unica para as operacoes de circulacao (Facade). */
class R04FacadeTest {

    @Test
    void R04_efetivarEmprestimoValidaReservaRemoveReservaCriaEmprestimoEAtualizaLivro() {
        Aplicacao app = Cenario.novo();
        assertTrue(app.tratadorDeEmprestimo.reservarLivro("L1", "E1", Cenario.DIA).sucesso());
        assertNotNull(app.movimentacao.consultarReserva("L1", "E1"));

        Resultado resultado = app.tratadorDeEmprestimo.emprestarLivro("L1", "E1", Cenario.DIA);

        assertTrue(resultado.sucesso());                                      // a interface so recebe Resultado (texto)
        assertNull(app.movimentacao.consultarReserva("L1", "E1"));            // reserva ativa removida
        assertNotNull(app.movimentacao.consultarEmprestimo("L1", "E1"));      // emprestimo criado
        Livro livro = app.acervo.consultar("L1");
        assertEquals(Situacao.EMPRESTADO, livro.getSituacao());               // Livro atualizado
        assertEquals(Cenario.VENCIMENTO, livro.getVencimento());
    }

    @Test
    void R04_reservaDeOutroEstudanteERecusadaENadaEGravado() {
        Aplicacao app = Cenario.novo();
        app.tratadorDeEmprestimo.reservarLivro("L1", "E1", Cenario.DIA);

        Resultado resultado = app.tratadorDeEmprestimo.emprestarLivro("L1", "E2", Cenario.DIA);

        assertFalse(resultado.sucesso());
        assertNotNull(app.movimentacao.consultarReserva("L1", "E1"));         // a reserva continua la
        assertNull(app.movimentacao.consultarEmprestimo("L1", "E2"));
        assertEquals(Situacao.RESERVADO, app.acervo.consultar("L1").getSituacao());
    }

    @Test
    void R04_emprestimoComDataAnteriorAReservaERecusadoENadaEGravado() {
        Aplicacao app = Cenario.novo();
        app.tratadorDeEmprestimo.reservarLivro("L1", "E1", Cenario.DIA);

        Resultado resultado = app.tratadorDeEmprestimo.emprestarLivro("L1", "E1", Cenario.DIA.minusDays(1));

        assertFalse(resultado.sucesso());
        assertNotNull(app.movimentacao.consultarReserva("L1", "E1"));
        assertNull(app.movimentacao.consultarEmprestimo("L1", "E1"));
        assertEquals(Situacao.RESERVADO, app.acervo.consultar("L1").getSituacao());
    }

    @Test
    void R04_devolucaoComDataAnteriorAoEmprestimoERecusadaENadaEGravado() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");

        Resultado resultado = app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.DIA.minusDays(1));

        assertFalse(resultado.sucesso());
        assertNotNull(app.movimentacao.consultarEmprestimo("L1", "E1"));      // o emprestimo continua ativo
        assertNull(app.movimentacao.consultarDevolucao("L1", "E1"));
        assertEquals(Situacao.EMPRESTADO, app.acervo.consultar("L1").getSituacao());
        assertEquals(Cenario.VENCIMENTO, app.acervo.consultar("L1").getVencimento());
    }

    @Test
    void R04_falhaNoMeioDaOperacaoDesfazOsPassosJaFeitos() {
        FabricaDePersistencia fabrica = new FabricaEmMemoria();
        UnidadeDeTrabalho uow = fabrica.criarUnidadeDeTrabalho();
        AcervoDeLivros acervo = new AcervoDeLivros(fabrica.criarRepositorioLivro(uow));
        Relacionamento relacionamento = new Relacionamento(fabrica.criarRepositorioRelacionamento(uow));
        Movimentacao movimentacaoQueFalha = new Movimentacao(fabrica.criarRepositorioMovimentacao(uow)) {
            @Override
            public void inserir(Emprestimo emprestimo) {                      // falha DEPOIS de a reserva ser removida
                throw new IllegalStateException("falha de gravacao simulada");
            }
        };
        ServicoDeCirculacao fachada = new ServicoDeCirculacao(uow, relacionamento, movimentacaoQueFalha, acervo);
        relacionamento.inserirPessoa(new Pessoa(Cenario.CPF_ANA, "Ana", "Silva", "ana@exemplo.br"));
        relacionamento.inserirEstudante(new Estudante("E1", Cenario.CPF_ANA));
        acervo.inserir(new Livro("L1", "Dom Casmurro"));
        fachada.reservar("L1", "E1", Cenario.DIA);

        assertThrows(IllegalStateException.class, () -> fachada.efetivarEmprestimo("L1", "E1", Cenario.DIA));

        assertNotNull(movimentacaoQueFalha.consultarReserva("L1", "E1"));     // reserva restaurada
        assertEquals(Situacao.RESERVADO, acervo.consultar("L1").getSituacao());   // livro nao ficou EMPRESTADO
    }
}
