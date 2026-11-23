package br.pucpr.biblioteca.banco;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adapter (papel Adaptee). Simula a API do banco A, usada como ela e:
 * CPF so com digitos, valor em CENTAVOS (long) e notificacao em JSON.
 */
public class ApiBancoA implements BancoSimulado {
    public record TituloA(String nossoNumero, String linhaDigitavel) { }

    public enum StatusA { EM_ABERTO, LIQUIDADO }

    private final Map<String, Long> titulos = new LinkedHashMap<>();
    private final Map<String, StatusA> status = new LinkedHashMap<>();
    private Consumer<String> webhook = json -> { };
    private long ultimosCentavos;
    private int sequencia;

    public TituloA gerarTitulo(String cpf, long centavos) {
        String nossoNumero = "A" + (++sequencia);
        titulos.put(nossoNumero, centavos);
        status.put(nossoNumero, StatusA.EM_ABERTO);
        ultimosCentavos = centavos;
        return new TituloA(nossoNumero, "23790." + cpf + "." + centavos);
    }

    public StatusA consultarTitulo(String nossoNumero) {
        return status.get(nossoNumero);
    }

    public void registrarWebhook(Consumer<String> webhook) {
        this.webhook = webhook;
    }

    /**
     * O banco recebe o pagamento e chama o webhook com a notificacao no formato dele.
     * Chamado de novo para um titulo ja pago, o banco reenvia a mesma notificacao.
     */
    @Override
    public void simularPagamento(String nossoNumero) {
        status.put(nossoNumero, StatusA.LIQUIDADO);
        webhook.accept(notificacaoDe(nossoNumero));
    }

    /** Notificacao no formato do banco A, com o status atual do titulo (EM_ABERTO ou LIQUIDADO). */
    public String notificacaoDe(String nossoNumero) {
        return "{\"nossoNumero\":\"" + nossoNumero + "\",\"pagamento\":\"PG-" + nossoNumero
                + "\",\"centavos\":" + titulos.get(nossoNumero) + ",\"status\":\"" + status.get(nossoNumero) + "\"}";
    }

    public long ultimosCentavos() { return ultimosCentavos; }
}
