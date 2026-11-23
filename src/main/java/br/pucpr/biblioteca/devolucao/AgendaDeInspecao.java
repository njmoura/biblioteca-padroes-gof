package br.pucpr.biblioteca.devolucao;

import java.util.ArrayList;
import java.util.List;

/** Fila de livros devolvidos que aguardam a inspecao do atendente. */
public class AgendaDeInspecao {
    private final List<String> pendentes = new ArrayList<>();

    public void agendar(String idLivro) {
        if (!pendentes.contains(idLivro)) {
            pendentes.add(idLivro);
        }
    }

    public void concluir(String idLivro) {
        pendentes.remove(idLivro);
    }

    public List<String> pendentes() { return List.copyOf(pendentes); }
}
