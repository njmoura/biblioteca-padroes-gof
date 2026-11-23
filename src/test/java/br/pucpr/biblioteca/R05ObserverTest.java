package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.devolucao.EventoDevolucao;
import br.pucpr.biblioteca.devolucao.FalhaDeEnvioException;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.multa.Multa;
import br.pucpr.biblioteca.tratador.Resultado;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Requisito 5 - Reacoes independentes a devolucao (Observer). */
class R05ObserverTest {

    @Test
    void R05_devolucaoAtrasadaAlimentaInspecaoMultaEIndicadores() {
        Aplicacao app = Cenario.novo();
        List<EventoDevolucao> recebidos = new ArrayList<>();
        app.publicador.registrar(recebidos::add);
        Cenario.emprestar(app, "L1", "E1");

        Resultado r = app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(3));

        assertTrue(r.sucesso());
        assertTrue(app.agenda.pendentes().contains("L1"));                    // inspecao agendada
        Multa multa = app.servicoDeMulta.listarMultas().get(0);               // multa solicitada ao Financeiro
        assertEquals(3, multa.getDiasAtraso());
        assertEquals(new BigDecimal("3.00"), multa.getValor());
        assertEquals(Cenario.CPF_ANA, multa.getCpf());
        assertEquals(1, app.painel.getDevolucoesComAtraso());                 // indicadores
        assertEquals(3, app.painel.getDiasDeAtrasoAcumulados());
        assertEquals(Situacao.DEVOLVIDO, app.acervo.consultar("L1").getSituacao());   // ate a decisao da inspecao
        EventoDevolucao evento = recebidos.get(0);                            // o evento leva os dados necessarios
        assertEquals(Cenario.VENCIMENTO, evento.vencimento());
        assertEquals(Cenario.VENCIMENTO.plusDays(3), evento.devolvidoEm());
        assertEquals("E1", evento.idEstudante());
    }

    @Test
    void R05_devolucaoNoPrazoNaoGeraMulta() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");

        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO);

        assertTrue(app.servicoDeMulta.listarMultas().isEmpty());
        assertTrue(app.agenda.pendentes().contains("L1"));
        assertEquals(1, app.painel.getDevolucoes());
        assertEquals(0, app.painel.getDevolucoesComAtraso());
        assertEquals(Situacao.DEVOLVIDO, app.acervo.consultar("L1").getSituacao());
    }

    @Test
    void R05_falhaDoEmailNaoAfetaOsDemaisNemDesfazADevolucao() {
        Aplicacao app = Cenario.novo(CodigoBanco.A, (para, assunto, texto) -> {
            throw new FalhaDeEnvioException("servidor de e-mail fora do ar");
        });
        List<EventoDevolucao> depoisDoEmail = new ArrayList<>();
        app.publicador.registrar(depoisDoEmail::add);                         // registrado DEPOIS do ouvinte que falha
        Cenario.emprestar(app, "L1", "E1");

        Resultado r = app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(3));

        assertTrue(r.sucesso());                                              // devolucao continua confirmada
        assertNotNull(app.movimentacao.consultarDevolucao("L1", "E1"));
        assertEquals(1, app.servicoDeMulta.listarMultas().size());            // os outros ouvintes reagiram
        assertEquals(1, app.painel.getDevolucoes());
        assertTrue(app.agenda.pendentes().contains("L1"));
        assertEquals(1, app.publicador.falhas().size());                      // a falha ficou registrada
        assertTrue(app.publicador.falhas().get(0).startsWith("OuvinteEmail"));
        assertEquals(1, depoisDoEmail.size());                                // quem vem depois da falha tambem e notificado
    }

    @Test
    void R05_publicacaoOcorreDepoisDaConfirmacaoDaDevolucao() {
        Aplicacao app = Cenario.novo();
        List<String> vistoPeloOuvinte = new ArrayList<>();
        app.publicador.registrar(evento -> {                                  // o que ja esta gravado na hora da notificacao
            vistoPeloOuvinte.add("devolucao=" + (app.movimentacao.consultarDevolucao("L1", "E1") != null));
            vistoPeloOuvinte.add("emprestimo=" + (app.movimentacao.consultarEmprestimo("L1", "E1") != null));
            vistoPeloOuvinte.add("situacao=" + app.acervo.consultar("L1").getSituacao());
        });
        Cenario.emprestar(app, "L1", "E1");

        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(3));

        assertEquals(List.of("devolucao=true", "emprestimo=false", "situacao=DEVOLVIDO"), vistoPeloOuvinte);
        assertTrue(app.publicador.falhas().isEmpty());
        assertNull(app.movimentacao.consultarEmprestimo("L1", "E1"));
    }

    @Test
    void R05_novoOuvinteEIncluidoSemAlterarOTratador() {
        Aplicacao app = Cenario.novo();
        List<EventoDevolucao> recebidos = new ArrayList<>();
        app.publicador.registrar(recebidos::add);                             // extensao: so um registrar()
        Cenario.emprestar(app, "L1", "E1");

        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(2));

        assertEquals(1, recebidos.size());
        assertEquals(2, recebidos.get(0).diasAtraso());                       // o evento traz os dados (push)
        assertEquals(Cenario.VENCIMENTO, recebidos.get(0).vencimento());
        assertEquals("E1", recebidos.get(0).idEstudante());
    }
}
