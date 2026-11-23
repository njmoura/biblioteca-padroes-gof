package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter (papel Adapter, de objeto) do banco A. Todas as conversoes do banco A ficam aqui:
 * reais -> centavos, CPF -> so digitos, nossoNumero -> idExterno, StatusA -> SituacaoCobranca,
 * JSON -> ConfirmacaoPagamento.
 */
public class AdaptadorBancoA implements GatewayDeCobranca {
    private final ApiBancoA api;
    private final ReceptorDePagamento receptor;

    public AdaptadorBancoA(ApiBancoA api, ReceptorDePagamento receptor) {
        this.api = api;
        this.receptor = receptor;
        api.registrarWebhook(this::receberWebhook);
    }

    @Override
    public Cobranca emitirBoleto(SolicitacaoCobranca s) {
        long centavos = paraCentavos(s.valor());                       // 7,00 -> 700
        String cpf = paraSoDigitos(s.cpf());                           // 111.111.111-11 -> 11111111111
        ApiBancoA.TituloA titulo = api.gerarTitulo(cpf, centavos);
        return new Cobranca(s.idCobranca(), CodigoBanco.A, titulo.nossoNumero(), s.valor(), titulo.linhaDigitavel());
    }

    @Override
    public SituacaoCobranca consultarSituacao(Cobranca c) {
        return traduzir(api.consultarTitulo(c.getIdExterno()));
    }

    /**
     * Entrada: traduz a notificacao JSON do banco A e entrega ao receptor institucional.
     * So a notificacao cujo status traduzido e QUITADA vira confirmacao de pagamento.
     */
    public void receberWebhook(String json) {
        if (traduzir(statusDe(campo(json, "\"status\":\"([^\"]+)\""))) != SituacaoCobranca.QUITADA) {
            return;                                                    // titulo ainda em aberto: nada a quitar
        }
        String nossoNumero = campo(json, "\"nossoNumero\":\"([^\"]+)\"");
        String idPagamento = campo(json, "\"pagamento\":\"([^\"]+)\"");
        long centavos = Long.parseLong(campo(json, "\"centavos\":(\\d+)"));
        BigDecimal valorPago = BigDecimal.valueOf(centavos).movePointLeft(2);
        receptor.confirmar(new ConfirmacaoPagamento(CodigoBanco.A, nossoNumero, idPagamento, valorPago));
    }

    private long paraCentavos(BigDecimal valor) {
        try {
            return valor.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new ValorInvalidoException(valor);                   // falha ANTES de chamar o banco
        }
    }

    private String paraSoDigitos(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    /** Texto de status do JSON -> StatusA; um texto que o banco A nao define vira null (DESCONHECIDA). */
    private ApiBancoA.StatusA statusDe(String texto) {
        for (ApiBancoA.StatusA s : ApiBancoA.StatusA.values()) {
            if (s.name().equals(texto)) {
                return s;
            }
        }
        return null;
    }

    private SituacaoCobranca traduzir(ApiBancoA.StatusA status) {
        if (status == ApiBancoA.StatusA.LIQUIDADO) {
            return SituacaoCobranca.QUITADA;
        }
        if (status == ApiBancoA.StatusA.EM_ABERTO) {
            return SituacaoCobranca.ABERTA;
        }
        return SituacaoCobranca.DESCONHECIDA;
    }

    private String campo(String texto, String expressao) {
        Matcher m = Pattern.compile(expressao).matcher(texto);
        if (!m.find()) {
            throw new IllegalArgumentException("notificacao do banco A fora do formato: " + texto);
        }
        return m.group(1);
    }
}
