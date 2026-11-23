package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.OperacaoInvalidaException;
import br.pucpr.biblioteca.dominio.RenovacaoNegadaException;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.tratador.Resultado;
import org.junit.jupiter.api.Test;

/** Requisito 1 - Ciclo de vida do livro (State). */
class R01StateTest {

    @Test
    void R01_terceiraRenovacaoRecusada() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        assertTrue(app.tratadorDeEmprestimo.renovarEmprestimo("L1", "E1").sucesso());
        assertTrue(app.tratadorDeEmprestimo.renovarEmprestimo("L1", "E1").sucesso());
        Livro antes = app.acervo.consultar("L1");
        assertEquals(Cenario.VENCIMENTO.plusDays(60), antes.getVencimento());

        Resultado terceira = app.tratadorDeEmprestimo.renovarEmprestimo("L1", "E1");

        assertFalse(terceira.sucesso());
        Livro depois = app.acervo.consultar("L1");
        assertEquals(antes.getVencimento(), depois.getVencimento());      // vencimento nao mudou
        assertEquals(2, depois.getNumRenovacoes());
        assertEquals(Situacao.RENOVADO, depois.getSituacao());

        // direto no objeto: a guarda e avaliada ANTES de prorrogar, entao a recusa nao altera o Livro
        Livro renovado = new Livro("L8", "Senhora", Situacao.RENOVADO, Cenario.VENCIMENTO.plusDays(60), 2, "E1");
        assertThrows(RenovacaoNegadaException.class, renovado::renovar);
        assertEquals(Cenario.VENCIMENTO.plusDays(60), renovado.getVencimento());
        assertEquals(2, renovado.getNumRenovacoes());
        assertEquals(Situacao.RENOVADO, renovado.getSituacao());
    }

    @Test
    void R01_livroDevolvidoNaoInspecionadoNaoPodeSerReservado() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.DIA.plusDays(3));

        Resultado reserva = app.tratadorDeEmprestimo.reservarLivro("L1", "E2", Cenario.DIA.plusDays(3));

        assertFalse(reserva.sucesso());
        assertEquals(Situacao.DEVOLVIDO, app.acervo.consultar("L1").getSituacao());

        // depois da inspecao aprovada o livro volta a LIVRE e a reserva e aceita
        assertTrue(app.tratadorDeDevolucao.inspecionarLivro("L1", true).sucesso());
        assertTrue(app.tratadorDeEmprestimo.reservarLivro("L1", "E2", Cenario.DIA.plusDays(4)).sucesso());
    }

    @Test
    void R01_laudoIrrecuperavelLevaReparandoAIncinerado() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.DIA.plusDays(3));
        app.tratadorDeDevolucao.inspecionarLivro("L1", false);                 // danificado
        assertEquals(Situacao.REPARANDO, app.acervo.consultar("L1").getSituacao());

        app.tratadorDeDevolucao.registrarRetornoDoReparo("L1", false);         // laudo irrecuperavel

        assertEquals(Situacao.INCINERADO, app.acervo.consultar("L1").getSituacao());
        assertFalse(app.tratadorDeEmprestimo.reservarLivro("L1", "E2", Cenario.DIA).sucesso());
    }

    @Test
    void R01_eventoInvalidoERecusadoSemAlterarOLivro() {
        Livro livro = new Livro("L9", "Livro livre");

        assertThrows(OperacaoInvalidaException.class, livro::renovar);        // LIVRE nao aceita renovar
        assertThrows(OperacaoInvalidaException.class, livro::devolver);

        assertEquals(Situacao.LIVRE, livro.getSituacao());
        assertEquals(0, livro.getNumRenovacoes());
    }

    @Test
    void R01_inspecaoReprovadaSoValeParaDevolvidoEEnviarParaReparoSoParaLivre() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.DIA.plusDays(3));

        // DEVOLVIDO: so a inspecao decide; o envio direto ao reparo (dano no acervo) e recusado
        assertFalse(app.tratadorDeAcervo.enviarParaReparo("L1").sucesso());
        assertEquals(Situacao.DEVOLVIDO, app.acervo.consultar("L1").getSituacao());
        assertTrue(app.tratadorDeDevolucao.inspecionarLivro("L1", false).sucesso());      // reprovarInspecao
        assertEquals(Situacao.REPARANDO, app.acervo.consultar("L1").getSituacao());

        // LIVRE: nao ha inspecao a reprovar; o dano identificado no acervo usa enviarParaReparo
        assertFalse(app.tratadorDeDevolucao.inspecionarLivro("L2", false).sucesso());
        assertEquals(Situacao.LIVRE, app.acervo.consultar("L2").getSituacao());
        assertTrue(app.tratadorDeAcervo.enviarParaReparo("L2").sucesso());
        assertEquals(Situacao.REPARANDO, app.acervo.consultar("L2").getSituacao());
    }

    @Test
    void R01_estadoGravadoComoSituacaoEReconstituido() {
        // grava pelo Acervo (o repositorio guarda a Situacao) e le de volta: o objeto de estado e reconstituido
        Aplicacao app = Cenario.novo();
        app.acervo.atualizar(new Livro("L1", "Dom Casmurro", Situacao.RENOVADO, Cenario.VENCIMENTO.plusDays(60), 2, "E1"));

        Livro lido = app.acervo.consultar("L1");

        assertEquals(Situacao.RENOVADO, lido.getSituacao());
        assertThrows(RenovacaoNegadaException.class, lido::renovar);          // o estado reconstituido aplica a guarda
        assertEquals(Cenario.VENCIMENTO.plusDays(60), lido.getVencimento());
        assertEquals(2, lido.getNumRenovacoes());
        assertEquals("E1", lido.getReservante());

        Livro reservado = new Livro("L2", "O Cortico", Situacao.RESERVADO, null, 0, "E1");
        reservado.emprestar(new Estudante("E1", Cenario.CPF_ANA), Cenario.DIA);
        assertEquals(Situacao.EMPRESTADO, reservado.getSituacao());
    }
}
