package br.pucpr.biblioteca.banco;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adapter (papel Adaptee). Simula a API do banco B, usada como ela e:
 * documento FORMATADO (000.000.000-00), valor DECIMAL em reais e notificacao em XML.
 */
public class ApiBancoB implements BancoSimulado {
    public record CobrancaB(String txid, String copiaECola) { }

    private final Map<String, BigDecimal> cobrancas = new LinkedHashMap<>();
    private final Map<String, String> status = new LinkedHashMap<>();
    private Consumer<String> notificador = xml -> { };
    private BigDecimal ultimoValorDecimal;
    private String ultimoDocumento;
    private int sequencia;

    public CobrancaB abrirCobranca(String documento, BigDecimal valorDecimal) {
        String txid = "B" + (++sequencia);
        cobrancas.put(txid, valorDecimal);
        status.put(txid, "ATIVA");
        ultimoValorDecimal = valorDecimal;
        ultimoDocumento = documento;
        return new CobrancaB(txid, "00020126." + documento + "." + valorDecimal);
    }

    public String statusCobranca(String txid) {
        return status.get(txid);
    }

    public void registrarNotificador(Consumer<String> notificador) {
        this.notificador = notificador;
    }

    /** Chamado de novo para uma cobranca ja paga, o banco reenvia a mesma notificacao. */
    @Override
    public void simularPagamento(String txid) {
        status.put(txid, "CONCLUIDA");
        notificador.accept(notificacaoDe(txid));
    }

    /** Notificacao no formato do banco B, com o status atual da cobranca (ATIVA ou CONCLUIDA). */
    public String notificacaoDe(String txid) {
        return "<pagamento><txid>" + txid + "</txid><e2e>E2E-" + txid + "</e2e><valor>"
                + cobrancas.get(txid) + "</valor><status>" + status.get(txid) + "</status></pagamento>";
    }

    public BigDecimal ultimoValorDecimal() { return ultimoValorDecimal; }

    public String ultimoDocumento() { return ultimoDocumento; }
}
