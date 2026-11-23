package br.pucpr.biblioteca.banco;

import java.util.ArrayList;
import java.util.List;

/**
 * Recebe as confirmacoes ja traduzidas pelos adaptadores e registra a quitacao.
 * A garantia de uma unica quitacao esta aqui (via quitarSeAberta), nao no adaptador.
 */
public class ServicoDeQuitacao implements ReceptorDePagamento {
    private final RegistroDeCobrancas registro;
    private final List<ConfirmacaoPagamento> desconhecidas = new ArrayList<>();
    private int quitacoes;

    public ServicoDeQuitacao(RegistroDeCobrancas registro) {
        this.registro = registro;
    }

    @Override
    public synchronized void confirmar(ConfirmacaoPagamento c) {
        Cobranca cobranca = registro.buscar(c.banco(), c.idExterno());
        if (cobranca == null) {
            desconhecidas.add(c);                 // fica para conciliacao manual; nada e quitado
            return;
        }
        if (registro.quitarSeAberta(cobranca.getIdCobranca(), c.idPagamento())) {
            quitacoes++;                          // notificacao repetida nao entra aqui
        }
    }

    public synchronized int totalDeQuitacoes() { return quitacoes; }

    public synchronized List<ConfirmacaoPagamento> desconhecidas() { return List.copyOf(desconhecidas); }
}
