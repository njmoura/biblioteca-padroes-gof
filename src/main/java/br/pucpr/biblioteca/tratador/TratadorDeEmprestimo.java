package br.pucpr.biblioteca.tratador;

import br.pucpr.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.circulacao.SituacaoDTO;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.solicitacao.ExecutorDeSolicitacoes;
import br.pucpr.biblioteca.solicitacao.ResultadoSolicitacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeCirculacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeReserva;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tratador do Estudante (f1 a f4). Nao tem regra de dominio nem testa a situacao do
 * livro: delega a fachada iCirculacao e converte excecoes de dominio em Resultado.
 */
public class TratadorDeEmprestimo implements iEmprestimo {
    private final iCirculacao circulacao;
    private final ExecutorDeSolicitacoes executor;

    public TratadorDeEmprestimo(iCirculacao circulacao, ExecutorDeSolicitacoes executor) {
        this.circulacao = circulacao;
        this.executor = executor;
    }

    /** f1: a reserva e sempre uma solicitacao (Command) encaminhada ao executor. */
    @Override
    public Resultado reservarLivro(String idLivro, String idEstudante, LocalDate data) {
        return comoResultado(submeter(new SolicitacaoDeReserva(UUID.randomUUID(), "estudante " + idEstudante, idLivro, idEstudante, data)));
    }

    @Override
    public Resultado consultarReserva(String idLivro, String idEstudante) {
        try {
            ComprovanteReserva c = circulacao.consultarReserva(idLivro, idEstudante);
            return Resultado.ok("reserva do livro " + c.idLivro() + " para " + c.idEstudante() + " feita em " + c.reservadoEm());
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    @Override
    public Resultado emprestarLivro(String idLivro, String idEstudante, LocalDate data) {
        try {
            ComprovanteEmprestimo c = circulacao.efetivarEmprestimo(idLivro, idEstudante, data);
            return Resultado.ok("livro " + c.idLivro() + " emprestado a " + c.idEstudante() + " em " + c.emprestadoEm()
                    + "; vencimento " + c.vencimento());
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    /** f4: a renovacao e sempre uma solicitacao (Command) encaminhada ao executor. */
    @Override
    public Resultado renovarEmprestimo(String idLivro, String idEstudante) {
        return comoResultado(submeter(new SolicitacaoDeRenovacao(UUID.randomUUID(), "estudante " + idEstudante, idLivro, idEstudante)));
    }

    /** Command: a tela cria a solicitacao (reserva ou renovacao) e o tratador a encaminha ao executor. */
    public ResultadoSolicitacao submeter(SolicitacaoDeCirculacao solicitacao) {
        return executor.submeter(solicitacao);
    }

    public Resultado consultarSituacao(String idLivro) {
        try {
            SituacaoDTO s = circulacao.consultarSituacao(idLivro);
            return Resultado.ok(descrever(s));
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    private static Resultado comoResultado(ResultadoSolicitacao r) {
        return r.status() == ResultadoSolicitacao.Status.EXECUTADA ? Resultado.ok(r.mensagem()) : Resultado.recusado(r.mensagem());
    }

    static String descrever(SituacaoDTO s) {
        String texto = "livro " + s.idLivro() + " \"" + s.titulo() + "\" esta " + s.situacao();
        if (s.vencimento() != null) {
            texto += ", vencimento " + s.vencimento() + ", " + s.numRenovacoes() + (s.numRenovacoes() == 1 ? " renovacao" : " renovacoes");
        }
        return texto;
    }
}
