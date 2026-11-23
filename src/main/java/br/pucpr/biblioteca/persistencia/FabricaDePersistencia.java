package br.pucpr.biblioteca.persistencia;

/**
 * Abstract Factory (papel AbstractFactory). Cria a unidade de trabalho e os tres
 * repositorios; cada fabrica concreta so cria produtos da propria familia.
 */
public interface FabricaDePersistencia {
    UnidadeDeTrabalho criarUnidadeDeTrabalho();

    RepositorioLivro criarRepositorioLivro(UnidadeDeTrabalho uow);

    RepositorioMovimentacao criarRepositorioMovimentacao(UnidadeDeTrabalho uow);

    RepositorioRelacionamento criarRepositorioRelacionamento(UnidadeDeTrabalho uow);
}
