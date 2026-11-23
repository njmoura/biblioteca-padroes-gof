package br.pucpr.biblioteca.devolucao;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer (papel Subject). Registra, remove e notifica os ouvintes. Cada ouvinte e
 * isolado: a excecao de um e registrada e nao se propaga, entao os demais continuam
 * sendo notificados e a devolucao ja confirmada nao e desfeita.
 */
public class PublicadorDeDevolucao {
    private final List<OuvinteDeDevolucao> ouvintes = new ArrayList<>();
    private final List<String> falhas = new ArrayList<>();

    public void registrar(OuvinteDeDevolucao ouvinte) {
        ouvintes.add(ouvinte);
    }

    public void remover(OuvinteDeDevolucao ouvinte) {
        ouvintes.remove(ouvinte);
    }

    public void publicar(EventoDevolucao evento) {
        for (OuvinteDeDevolucao ouvinte : List.copyOf(ouvintes)) {     // copia: um ouvinte pode registrar ou remover outro
            try {
                ouvinte.aoDevolver(evento);
            } catch (RuntimeException erro) {
                registrarFalha(ouvinte, evento, erro);
            }
        }
    }

    private void registrarFalha(OuvinteDeDevolucao ouvinte, EventoDevolucao evento, RuntimeException erro) {
        falhas.add(ouvinte.getClass().getSimpleName() + " falhou na devolucao " + evento.idDevolucao() + ": " + erro.getMessage());
    }

    public List<String> falhas() { return List.copyOf(falhas); }
}
