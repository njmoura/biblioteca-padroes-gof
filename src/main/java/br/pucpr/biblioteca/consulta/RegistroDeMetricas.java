package br.pucpr.biblioteca.consulta;

import java.util.ArrayList;
import java.util.List;

/** Destino das medicoes de tempo de resposta e dos avisos de consulta atendida pelo cache. */
public class RegistroDeMetricas {
    public record Medicao(String termo, long nanos, boolean atendidaPeloCache) { }

    private final List<Medicao> medicoes = new ArrayList<>();
    private int acertosDeCache;
    private boolean acertoPendente;

    /** Chamado pelo decorador de medicao ao fim de cada consulta. */
    public synchronized void registrar(String termo, long nanos) {
        medicoes.add(new Medicao(termo, nanos, acertoPendente));
        acertoPendente = false;
    }

    /** Chamado pelo decorador de cache quando ele responde sem ir ao acervo. */
    public synchronized void registrarAcertoDeCache(String termo) {
        acertosDeCache++;
        acertoPendente = true;
    }

    public synchronized int total() { return medicoes.size(); }

    public synchronized int acertosDeCache() { return acertosDeCache; }

    /** Ultima consulta medida, ou null se nenhuma foi medida ainda. */
    public synchronized Medicao ultima() {
        return medicoes.isEmpty() ? null : medicoes.get(medicoes.size() - 1);
    }

    public synchronized List<Medicao> medicoes() { return List.copyOf(medicoes); }
}
