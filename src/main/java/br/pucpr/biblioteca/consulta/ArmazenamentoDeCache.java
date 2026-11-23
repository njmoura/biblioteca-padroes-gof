package br.pucpr.biblioteca.consulta;

import java.util.List;

/**
 * Onde o DecoradorDeCache guarda os metadados. E uma interface para que o armazenamento
 * possa ser trocado (memoria, servico externo) e para que a falha dele possa ser
 * verificada: se ler ou gravar lancar excecao, a consulta continua sem o cache.
 */
public interface ArmazenamentoDeCache {
    /** Devolve a lista guardada para o termo, ou null se nao ha entrada valida. */
    List<MetadadosLivro> ler(String termo);

    void gravar(String termo, List<MetadadosLivro> lista);

    /** Descarta todas as entradas. */
    void limpar();
}
