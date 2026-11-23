package br.pucpr.biblioteca.consulta;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Armazenamento padrao do cache: em memoria, com tempo de validade por entrada. */
public class ArmazenamentoEmMemoria implements ArmazenamentoDeCache {
    private record Entrada(List<MetadadosLivro> lista, Instant expiraEm) { }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Duration validade;

    public ArmazenamentoEmMemoria(Duration validade) {
        this.validade = validade;
    }

    @Override
    public List<MetadadosLivro> ler(String termo) {
        Entrada entrada = entradas.get(termo);
        if (entrada == null) {
            return null;
        }
        if (!Instant.now().isBefore(entrada.expiraEm())) {
            entradas.remove(termo);                              // entrada vencida
            return null;
        }
        return entrada.lista();
    }

    @Override
    public void gravar(String termo, List<MetadadosLivro> lista) {
        entradas.put(termo, new Entrada(List.copyOf(lista), Instant.now().plus(validade)));
    }

    @Override
    public void limpar() {
        entradas.clear();
    }
}
