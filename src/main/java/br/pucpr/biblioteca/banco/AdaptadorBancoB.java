package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter (papel Adapter, de objeto) do banco B. Todas as conversoes do banco B ficam aqui:
 * CPF -> documento formatado, valor -> decimal com duas casas, txid -> idExterno,
 * texto de status -> SituacaoCobranca, XML -> ConfirmacaoPagamento.
 */
public class AdaptadorBancoB implements GatewayDeCobranca {
    private final ApiBancoB api;
    private final ReceptorDePagamento receptor;

    public AdaptadorBancoB(ApiBancoB api, ReceptorDePagamento receptor) {
        this.api = api;
        this.receptor = receptor;
        api.registrarNotificador(this::receberNotificacao);
    }

    @Override
    public Cobranca emitirBoleto(SolicitacaoCobranca s) {
        BigDecimal valorDecimal = paraDecimal(s.valor());              // 7,00 continua 7.00 (duas casas)
        String documento = paraDocumento(s.cpf());                     // 12345678901 -> 123.456.789-01
        ApiBancoB.CobrancaB cobranca = api.abrirCobranca(documento, valorDecimal);
        return new Cobranca(s.idCobranca(), CodigoBanco.B, cobranca.txid(), s.valor(), cobranca.copiaECola());
    }

    @Override
    public SituacaoCobranca consultarSituacao(Cobranca c) {
        return traduzir(api.statusCobranca(c.getIdExterno()));
    }

    /**
     * Entrada: traduz a notificacao XML do banco B e entrega ao receptor institucional.
     * So a notificacao cujo status traduzido e QUITADA vira confirmacao de pagamento.
     */
    public void receberNotificacao(String xml) {
        if (traduzir(campo(xml, "<status>([^<]+)</status>")) != SituacaoCobranca.QUITADA) {
            return;                                                    // cobranca ainda ativa: nada a quitar
        }
        String txid = campo(xml, "<txid>([^<]+)</txid>");
        String idPagamento = campo(xml, "<e2e>([^<]+)</e2e>");
        BigDecimal valorPago = new BigDecimal(campo(xml, "<valor>([^<]+)</valor>"));
        receptor.confirmar(new ConfirmacaoPagamento(CodigoBanco.B, txid, idPagamento, valorPago));
    }

    private BigDecimal paraDecimal(BigDecimal valor) {
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ValorInvalidoException(valor);                   // falha ANTES de chamar o banco
        }
    }

    private String paraDocumento(String cpf) {
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) {
            throw new DocumentoInvalidoException(cpf);                 // falha ANTES de chamar o banco
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    private SituacaoCobranca traduzir(String status) {
        if ("CONCLUIDA".equals(status)) {
            return SituacaoCobranca.QUITADA;
        }
        if ("ATIVA".equals(status)) {
            return SituacaoCobranca.ABERTA;
        }
        return SituacaoCobranca.DESCONHECIDA;
    }

    private String campo(String texto, String expressao) {
        Matcher m = Pattern.compile(expressao).matcher(texto);
        if (!m.find()) {
            throw new IllegalArgumentException("notificacao do banco B fora do formato: " + texto);
        }
        return m.group(1);
    }
}
