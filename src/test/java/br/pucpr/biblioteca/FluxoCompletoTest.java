package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.banco.SituacaoCobranca;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.multa.Multa;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** O fluxo da demonstracao, so pelos tratadores: reserva, emprestimo, renovacao, devolucao atrasada, multa e quitacao. */
class FluxoCompletoTest {

    @Test
    void fluxoCompleto_reservaEmprestimoRenovacaoDevolucaoAtrasadaMultaEQuitacao() {
        Aplicacao app = Cenario.novo();

        assertTrue(app.tratadorDeEmprestimo.reservarLivro("L1", "E1", Cenario.DIA).sucesso());
        assertTrue(app.tratadorDeEmprestimo.consultarReserva("L1", "E1").sucesso());
        assertTrue(app.tratadorDeEmprestimo.emprestarLivro("L1", "E1", Cenario.DIA).sucesso());
        assertTrue(app.tratadorDeEmprestimo.renovarEmprestimo("L1", "E1").sucesso());          // vence em 08/12
        assertTrue(app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(34)).sucesso());

        Multa multa = app.servicoDeMulta.listarMultas().get(0);
        assertEquals(4, multa.getDiasAtraso());
        assertEquals(new BigDecimal("4.00"), multa.getValor());
        assertTrue(app.tratadorDeDevolucao.emitirBoletoMulta("L1", "E1").sucesso());           // f6: 2a via

        assertTrue(app.tratadorDeMulta.simularPagamentoNoBanco(multa.getCobranca().getIdExterno()).sucesso());
        assertEquals(SituacaoCobranca.QUITADA, multa.getCobranca().getSituacao());
        assertEquals(1, app.quitacao.totalDeQuitacoes());

        assertTrue(app.tratadorDeDevolucao.inspecionarLivro("L1", true).sucesso());
        assertEquals(Situacao.LIVRE, app.acervo.consultar("L1").getSituacao());
    }
}
