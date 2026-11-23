package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.FamiliaIncompativelException;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/**
 * Abstract Factory (ConcreteFactory): familia relacional, usada no perfil PRODUCAO.
 * Todas as escritas vao para o MySQL principal; o Oracle e replica mantida pela
 * infraestrutura e nenhuma fabrica cria repositorio que escreva nele.
 */
public class FabricaMySQL implements FabricaDePersistencia {
    private final String url;
    private final String usuario;
    private final String senha;

    public FabricaMySQL(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    @Override
    public UnidadeDeTrabalho criarUnidadeDeTrabalho() {
        return new UnidadeDeTrabalhoMySQL(url, usuario, senha);
    }

    @Override
    public RepositorioLivro criarRepositorioLivro(UnidadeDeTrabalho uow) {
        return new RepositorioLivroMySQL(daFamilia(uow));
    }

    @Override
    public RepositorioMovimentacao criarRepositorioMovimentacao(UnidadeDeTrabalho uow) {
        return new RepositorioMovimentacaoMySQL(daFamilia(uow));
    }

    @Override
    public RepositorioRelacionamento criarRepositorioRelacionamento(UnidadeDeTrabalho uow) {
        return new RepositorioRelacionamentoMySQL(daFamilia(uow));
    }

    /** Impede misturar familias: so aceita a unidade de trabalho MySQL. */
    private UnidadeDeTrabalhoMySQL daFamilia(UnidadeDeTrabalho uow) {
        if (!(uow instanceof UnidadeDeTrabalhoMySQL)) {
            throw new FamiliaIncompativelException("MySQL", uow);
        }
        return (UnidadeDeTrabalhoMySQL) uow;
    }
}
