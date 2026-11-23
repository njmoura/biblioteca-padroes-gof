package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.banco.AdaptadorBancoA;
import br.pucpr.biblioteca.banco.AdaptadorBancoB;
import br.pucpr.biblioteca.banco.ApiBancoA;
import br.pucpr.biblioteca.banco.ApiBancoB;
import br.pucpr.biblioteca.banco.Cobranca;
import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.banco.DocumentoInvalidoException;
import br.pucpr.biblioteca.banco.GatewayDeCobranca;
import br.pucpr.biblioteca.banco.RegistroDeCobrancas;
import br.pucpr.biblioteca.banco.ServicoDeQuitacao;
import br.pucpr.biblioteca.banco.SituacaoCobranca;
import br.pucpr.biblioteca.banco.SolicitacaoCobranca;
import br.pucpr.biblioteca.banco.ValorInvalidoException;
import br.pucpr.biblioteca.multa.ConfiguracaoAdministrativa;
import br.pucpr.biblioteca.multa.Multa;
import br.pucpr.biblioteca.multa.SeletorDePolitica;
import br.pucpr.biblioteca.multa.ServicoDeMulta;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Requisito 3 - Integracao com bancos incompativeis (Adapter). */
class R03AdapterTest {
    private final RegistroDeCobrancas registro = new RegistroDeCobrancas();
    private final ServicoDeQuitacao quitacao = new ServicoDeQuitacao(registro);
    private final ApiBancoA apiA = new ApiBancoA();
    private final ApiBancoB apiB = new ApiBancoB();
    private final AdaptadorBancoA adaptadorA = new AdaptadorBancoA(apiA, quitacao);
    private final AdaptadorBancoB adaptadorB = new AdaptadorBancoB(apiB, quitacao);

    /** O mesmo ServicoDeMulta, sem nenhum "if" por banco: so muda o gateway recebido na montagem. */
    private ServicoDeMulta servicoCom(GatewayDeCobranca gateway) {
        return new ServicoDeMulta(new SeletorDePolitica(new ConfiguracaoAdministrativa()), gateway, registro);
    }

    private Multa multaDe(String idDevolucao, String valor) {
        return new Multa(idDevolucao, Cenario.CPF_ANA, 7, new BigDecimal(valor), "ORIGINAL", Cenario.DIA);
    }

    @Test
    void R03_seteReaisViram700CentavosNoBancoAe7ReaisNoBancoB() {
        SolicitacaoCobranca solicitacao = new SolicitacaoCobranca("COB-1", Cenario.CPF_ANA, new BigDecimal("7.00"));

        Cobranca noA = adaptadorA.emitirBoleto(solicitacao);                  // a MESMA solicitacao nos dois bancos
        Cobranca noB = adaptadorB.emitirBoleto(solicitacao);

        assertEquals(700L, apiA.ultimosCentavos());                           // banco A: centavos
        assertEquals(new BigDecimal("7.00"), apiB.ultimoValorDecimal());      // banco B: decimal
        assertEquals("111.111.111-11", apiB.ultimoDocumento());               // banco B: documento formatado
        assertEquals("23790.11111111111.700", noA.getLinhaDigitavel());       // banco A: CPF so com digitos
        assertEquals(CodigoBanco.A, noA.getBanco());
        assertEquals(CodigoBanco.B, noB.getBanco());
        assertEquals("A1", noA.getIdExterno());                               // nossoNumero e txid viram idExterno
        assertEquals("B1", noB.getIdExterno());
        assertEquals(new BigDecimal("7.00"), noA.getValor());                 // a cobranca institucional e igual nos dois
        assertEquals(new BigDecimal("7.00"), noB.getValor());
        assertEquals(SituacaoCobranca.ABERTA, adaptadorA.consultarSituacao(noA));   // EM_ABERTO e ATIVA viram ABERTA
        assertEquals(SituacaoCobranca.ABERTA, adaptadorB.consultarSituacao(noB));
    }

    @Test
    void R03_mesmoServicoDeMultasCobraNosDoisBancosSemCondicionalPorBanco() {
        Cobranca noA = servicoCom(adaptadorA).cobrar(multaDe("D1", "7.00"));
        Cobranca noB = servicoCom(adaptadorB).cobrar(multaDe("D2", "7.00"));

        assertEquals(700L, apiA.ultimosCentavos());
        assertEquals(new BigDecimal("7.00"), apiB.ultimoValorDecimal());
        assertEquals(CodigoBanco.A, noA.getBanco());
        assertEquals(CodigoBanco.B, noB.getBanco());
        assertEquals(2, registro.listar().size());                            // as duas ficaram no registro institucional
    }

    @Test
    void R03_notificacaoRepetidaNaoGeraSegundaQuitacao() {
        Cobranca cobranca = servicoCom(adaptadorA).cobrar(multaDe("D1", "7.00"));

        apiA.simularPagamento(cobranca.getIdExterno());                       // o banco notifica pelo webhook do adaptador
        String json = apiA.notificacaoDe(cobranca.getIdExterno());
        adaptadorA.receberWebhook(json);                                      // o banco reenviou a mesma notificacao
        apiA.simularPagamento(cobranca.getIdExterno());                       // e reenviou outra vez

        assertEquals(1, quitacao.totalDeQuitacoes());
        assertEquals(SituacaoCobranca.QUITADA, cobranca.getSituacao());
    }

    @Test
    void R03_notificacaoDoBancoBTambemETraduzidaPeloAdaptador() {
        Cobranca cobranca = servicoCom(adaptadorB).cobrar(multaDe("D2", "7.00"));

        apiB.simularPagamento(cobranca.getIdExterno());                       // banco chama o adaptador com o XML dele
        adaptadorB.receberNotificacao(apiB.notificacaoDe(cobranca.getIdExterno()));

        assertEquals(1, quitacao.totalDeQuitacoes());
        assertEquals(SituacaoCobranca.QUITADA, cobranca.getSituacao());
        assertEquals(SituacaoCobranca.QUITADA, adaptadorB.consultarSituacao(cobranca));
    }

    @Test
    void R03_valorComMaisDeDuasCasasFalhaAntesDeChamarOBanco() {
        assertThrows(ValorInvalidoException.class, () -> servicoCom(adaptadorA).cobrar(multaDe("D3", "7.005")));
        assertEquals(0L, apiA.ultimosCentavos());
        assertThrows(ValorInvalidoException.class, () -> servicoCom(adaptadorB).cobrar(multaDe("D4", "7.005")));
        assertNull(apiB.ultimoValorDecimal());                                // o banco B tambem nao foi chamado
        assertTrue(registro.listar().isEmpty());
    }

    @Test
    void R03_cpfForaDoFormatoERecusadoPeloAdaptadorDoBancoB() {
        SolicitacaoCobranca solicitacao = new SolicitacaoCobranca("COB-9", "123", new BigDecimal("7.00"));

        assertThrows(DocumentoInvalidoException.class, () -> adaptadorB.emitirBoleto(solicitacao));
        assertNull(apiB.ultimoDocumento());
    }

    @Test
    void R03_notificacaoDeCobrancaNaoPagaNaoQuita() {
        Cobranca noA = servicoCom(adaptadorA).cobrar(multaDe("D1", "7.00"));
        Cobranca noB = servicoCom(adaptadorB).cobrar(multaDe("D2", "7.00"));

        adaptadorA.receberWebhook(apiA.notificacaoDe(noA.getIdExterno()));        // status EM_ABERTO
        adaptadorB.receberNotificacao(apiB.notificacaoDe(noB.getIdExterno()));    // status ATIVA

        assertEquals(0, quitacao.totalDeQuitacoes());                         // o adaptador traduz o status antes de confirmar
        assertEquals(SituacaoCobranca.ABERTA, noA.getSituacao());
        assertEquals(SituacaoCobranca.ABERTA, noB.getSituacao());
    }
}
