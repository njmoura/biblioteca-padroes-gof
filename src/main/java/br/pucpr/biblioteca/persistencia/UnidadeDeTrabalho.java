package br.pucpr.biblioteca.persistencia;

/** Abstract Factory (AbstractProduct). Demarca a transacao compartilhada pelos repositorios da mesma familia. */
public interface UnidadeDeTrabalho {
    void iniciar();

    void confirmar();

    void desfazer();
}
