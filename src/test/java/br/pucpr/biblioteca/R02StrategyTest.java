package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.multa.Multa;
import br.pucpr.biblioteca.multa.MultaComTeto;
import br.pucpr.biblioteca.multa.MultaComTolerancia;
import br.pucpr.biblioteca.multa.MultaDiaria;
import br.pucpr.biblioteca.multa.PoliticaDeMulta;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Requisito 2 - Politicas de calculo de multa (Strategy). */
class R02StrategyTest {

    @Test
    void R02_cincoDiasDeAtraso_5ReaisNaOriginalE3ReaisNaTolerancia() {
        assertEquals(new BigDecimal("5.00"), new MultaDiaria().calcular(5));
        assertEquals(new BigDecimal("3.00"), new MultaComTolerancia().calcular(5));
        assertEquals(new BigDecimal("5.00"), new MultaComTeto().calcular(5));       // nao atinge o teto
        assertEquals(new BigDecimal("20.00"), new MultaComTeto().calcular(45));     // teto de R$ 20,00
    }

    @Test
    void R02_semAtrasoValorZero() {
        for (PoliticaDeMulta p : List.of(new MultaDiaria(), new MultaComTeto(), new MultaComTolerancia())) {
            assertEquals(new BigDecimal("0.00"), p.calcular(0), p.id());
        }
        assertEquals(new BigDecimal("0.00"), new MultaComTolerancia().calcular(2));  // dentro da tolerancia
    }

    @Test
    void R02_simulacaoPelaConfiguracaoNaoCriaMulta() {
        Aplicacao app = Cenario.novo();

        var valores = app.tratadorDeMulta.simular(5);

        assertEquals(new BigDecimal("5.00"), valores.get("ORIGINAL"));
        assertEquals(new BigDecimal("3.00"), valores.get("TOLERANCIA"));
        assertTrue(app.servicoDeMulta.listarMultas().isEmpty());
    }

    @Test
    void R02_trocaDePoliticaNaoRecalculaMultaJaEmitida() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(5));
        Multa primeira = app.servicoDeMulta.listarMultas().get(0);
        assertEquals(new BigDecimal("5.00"), primeira.getValor());
        assertEquals("ORIGINAL", primeira.getPoliticaAplicada());

        assertTrue(app.tratadorDeMulta.definirPolitica("TOLERANCIA").sucesso());

        assertEquals(new BigDecimal("5.00"), primeira.getValor());                  // a emitida nao muda
        assertEquals("ORIGINAL", primeira.getPoliticaAplicada());

        Cenario.emprestar(app, "L2", "E2");
        app.tratadorDeDevolucao.registrarDevolucao("L2", "E2", Cenario.VENCIMENTO.plusDays(5));
        Multa segunda = app.servicoDeMulta.listarMultas().get(1);
        assertEquals(new BigDecimal("3.00"), segunda.getValor());                   // a nova usa a politica vigente
        assertEquals("TOLERANCIA", segunda.getPoliticaAplicada());

        assertNotNull(primeira.getCobranca());                                      // a emissao de boletos nao muda
        assertNotNull(segunda.getCobranca());
        assertEquals(primeira.getCobranca().getBanco(), segunda.getCobranca().getBanco());
        assertTrue(primeira.getCobranca().getIdCobranca().startsWith("COB-"));
        assertTrue(segunda.getCobranca().getIdCobranca().startsWith("COB-"));
        assertEquals(new BigDecimal("3.00"), segunda.getCobranca().getValor());
    }

    @Test
    void R02_novaPoliticaEntraSemAlterarOServicoDeMultas() {
        Aplicacao app = Cenario.novo();
        app.seletorDePolitica.registrar(new PoliticaDeMulta() {                     // extensao: so uma classe nova
            @Override
            public String id() { return "DOBRO"; }

            @Override
            public BigDecimal calcular(int dias) { return BigDecimal.valueOf(2L * Math.max(0, dias)); }   // escala 0: o servico ajusta
        });
        assertTrue(app.tratadorDeMulta.definirPolitica("DOBRO").sucesso());
        assertFalse(app.tratadorDeMulta.definirPolitica("INEXISTENTE").sucesso());   // id invalido e recusado

        Cenario.emprestar(app, "L1", "E1");
        app.tratadorDeDevolucao.registrarDevolucao("L1", "E1", Cenario.VENCIMENTO.plusDays(5));

        assertEquals(new BigDecimal("10.00"), app.servicoDeMulta.listarMultas().get(0).getValor());
        assertEquals("DOBRO", app.servicoDeMulta.listarMultas().get(0).getPoliticaAplicada());
    }
}
