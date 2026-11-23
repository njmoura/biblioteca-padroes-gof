package br.pucpr.biblioteca.solicitacao;

import br.pucpr.biblioteca.circulacao.iCirculacao;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

/**
 * Command (papel Invoker). Dispara a execucao e garante que o mesmo identificador nao
 * seja executado duas vezes. O receptor (iCirculacao) e quem aplica as regras.
 */
public class ExecutorDeSolicitacoes {
    private final RegistroDeSolicitacoes registro;
    private final iCirculacao circulacao;

    public ExecutorDeSolicitacoes(RegistroDeSolicitacoes registro, iCirculacao circulacao) {
        this.registro = registro;
        this.circulacao = circulacao;
    }

    public ResultadoSolicitacao submeter(SolicitacaoDeCirculacao s) {
        if (!registro.reservarExecucao(s.getId())) {                 // id ja executado ou em execucao
            ResultadoSolicitacao gravado = registro.resultadoDe(s.getId());
            return gravado != null ? gravado.comoRepetida()          // nada e executado de novo
                    : ResultadoSolicitacao.emProcessamento(s.getId());
        }
        ResultadoSolicitacao resultado;
        try {
            resultado = s.executar(circulacao);                      // regras ficam no receptor
        } catch (ExcecaoDeDominio recusa) {
            resultado = ResultadoSolicitacao.recusada(s.getId(), recusa.getMessage());
        } catch (RuntimeException falhaTecnica) {
            registro.liberar(s.getId());
            throw falhaTecnica;
        }
        registro.concluir(s.getId(), resultado);                     // reaproveitado nas repeticoes
        return resultado;
    }
}
