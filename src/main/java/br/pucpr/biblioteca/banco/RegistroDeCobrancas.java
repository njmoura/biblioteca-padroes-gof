package br.pucpr.biblioteca.banco;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Armazena as cobrancas emitidas. quitarSeAberta e a atualizacao condicional que impede a segunda quitacao. */
public class RegistroDeCobrancas {
    private final Map<String, Cobranca> cobrancas = new LinkedHashMap<>();

    public synchronized void registrar(Cobranca c) {
        cobrancas.put(c.getIdCobranca(), c);
    }

    public synchronized Cobranca buscar(CodigoBanco banco, String idExterno) {
        for (Cobranca c : cobrancas.values()) {
            if (c.getBanco() == banco && c.getIdExterno().equals(idExterno)) {
                return c;
            }
        }
        return null;
    }

    /** Atomica: devolve true so na primeira vez; se a cobranca ja esta QUITADA devolve false. */
    public synchronized boolean quitarSeAberta(String idCobranca, String idPagamento) {
        Cobranca c = cobrancas.get(idCobranca);
        if (c == null || c.getSituacao() != SituacaoCobranca.ABERTA) {
            return false;
        }
        c.quitar(idPagamento);
        return true;
    }

    public synchronized List<Cobranca> listar() {
        return new ArrayList<>(cobrancas.values());
    }
}
