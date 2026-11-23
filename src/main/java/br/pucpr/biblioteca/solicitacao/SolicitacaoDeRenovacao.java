package br.pucpr.biblioteca.solicitacao;

import br.pucpr.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import java.util.UUID;

/** Command (ConcreteCommand): guarda os parametros da renovacao e chama o receptor. */
public class SolicitacaoDeRenovacao extends SolicitacaoDeCirculacao {
    private final String idLivro;
    private final String idEstudante;

    public SolicitacaoDeRenovacao(UUID id, String autor, String idLivro, String idEstudante) {
        super(id, autor);
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
    }

    @Override
    public ResultadoSolicitacao executar(iCirculacao circulacao) {
        ComprovanteRenovacao c = circulacao.renovar(idLivro, idEstudante);
        return ResultadoSolicitacao.executada(getId(),
                "renovacao " + c.numRenovacoes() + " do livro " + c.idLivro() + "; novo vencimento " + c.novoVencimento());
    }
}
