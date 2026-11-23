package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.FamiliaIncompativelException;
import br.pucpr.biblioteca.persistencia.RepositorioLivro;
import br.pucpr.biblioteca.persistencia.RepositorioMovimentacao;
import br.pucpr.biblioteca.persistencia.RepositorioRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/** Abstract Factory (ConcreteFactory): familia em memoria, usada no perfil TESTE. */
public class FabricaEmMemoria implements FabricaDePersistencia {
    private final BaseEmMemoria base = new BaseEmMemoria();

    @Override
    public UnidadeDeTrabalho criarUnidadeDeTrabalho() {
        return new UnidadeDeTrabalhoEmMemoria(base);
    }

    @Override
    public RepositorioLivro criarRepositorioLivro(UnidadeDeTrabalho uow) {
        return new RepositorioLivroEmMemoria(daFamilia(uow));
    }

    @Override
    public RepositorioMovimentacao criarRepositorioMovimentacao(UnidadeDeTrabalho uow) {
        return new RepositorioMovimentacaoEmMemoria(daFamilia(uow));
    }

    @Override
    public RepositorioRelacionamento criarRepositorioRelacionamento(UnidadeDeTrabalho uow) {
        return new RepositorioRelacionamentoEmMemoria(daFamilia(uow));
    }

    /** Impede misturar familias: so aceita a unidade de trabalho em memoria. */
    private UnidadeDeTrabalhoEmMemoria daFamilia(UnidadeDeTrabalho uow) {
        if (!(uow instanceof UnidadeDeTrabalhoEmMemoria)) {
            throw new FamiliaIncompativelException("em memoria", uow);
        }
        return (UnidadeDeTrabalhoEmMemoria) uow;
    }
}
