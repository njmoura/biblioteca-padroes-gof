package br.pucpr.biblioteca.tratador;

import br.pucpr.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.circulacao.SituacaoDTO;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import br.pucpr.biblioteca.devolucao.AgendaDeInspecao;
import br.pucpr.biblioteca.devolucao.EventoDevolucao;
import br.pucpr.biblioteca.devolucao.PainelDeIndicadores;
import br.pucpr.biblioteca.devolucao.PublicadorDeDevolucao;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.multa.Multa;
import br.pucpr.biblioteca.multa.iMulta;
import java.time.LocalDate;

/**
 * Tratador do Atendente (f5 e f6, inspecao e retorno do reparo).
 * Observer: conclui a devolucao pela fachada e SO DEPOIS publica o evento; nao conhece
 * os ouvintes, apenas o publicador.
 */
public class TratadorDeDevolucao implements iDevolucao {
    private final iCirculacao circulacao;
    private final PublicadorDeDevolucao publicador;
    private final iMulta financeiro;
    private final AgendaDeInspecao agenda;
    private final PainelDeIndicadores painel;

    public TratadorDeDevolucao(iCirculacao circulacao, PublicadorDeDevolucao publicador, iMulta financeiro,
                               AgendaDeInspecao agenda, PainelDeIndicadores painel) {
        this.circulacao = circulacao;
        this.publicador = publicador;
        this.financeiro = financeiro;
        this.agenda = agenda;
        this.painel = painel;
    }

    @Override
    public Resultado registrarDevolucao(String idLivro, String idEstudante, LocalDate data) {
        try {
            ComprovanteDevolucao c = circulacao.devolver(idLivro, idEstudante, data);     // transacao ja confirmada
            EventoDevolucao evento = new EventoDevolucao(c);
            publicador.publicar(evento);                                                  // depois da confirmacao
            Multa multa = financeiro.consultarMulta(c.idDevolucao());                     // emitida (ou nao) pelo ouvinte de multa
            return Resultado.ok("devolucao do livro " + c.idLivro() + " registrada em " + c.devolvidoEm()
                    + " (vencimento " + c.vencimento() + ", dias de atraso " + evento.diasAtraso() + "); "
                    + (multa == null ? "sem multa" : multa));
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    /** f6: emite a segunda via do boleto da multa da devolucao (regra 10). */
    @Override
    public Resultado emitirBoletoMulta(String idLivro, String idEstudante) {
        try {
            ComprovanteDevolucao d = circulacao.consultarDevolucao(idLivro, idEstudante);
            Multa multa = financeiro.consultarMulta(d.idDevolucao());
            if (multa == null) {
                return Resultado.recusado("a devolucao do livro " + idLivro + " nao gerou multa");
            }
            return Resultado.ok("2a via do boleto - " + multa);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    @Override
    public Resultado inspecionarLivro(String idLivro, boolean emBomEstado) {
        try {
            SituacaoDTO s = circulacao.registrarInspecao(idLivro, emBomEstado);
            agenda.concluir(idLivro);
            return Resultado.ok(TratadorDeEmprestimo.descrever(s));
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    @Override
    public Resultado registrarRetornoDoReparo(String idLivro, boolean reparado) {
        try {
            SituacaoDTO s = circulacao.registrarRetornoDoReparo(idLivro, reparado);
            return Resultado.ok(TratadorDeEmprestimo.descrever(s));
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    /** Resumo das reacoes a devolucao, para a tela do atendente. */
    public String resumoDasReacoes() {
        return "inspecoes pendentes: " + (agenda.pendentes().isEmpty() ? "nenhuma" : String.join(", ", agenda.pendentes()))
                + "\nindicadores: " + painel
                + "\nfalhas de ouvintes: " + (publicador.falhas().isEmpty() ? "nenhuma" : String.join("; ", publicador.falhas()));
    }
}
