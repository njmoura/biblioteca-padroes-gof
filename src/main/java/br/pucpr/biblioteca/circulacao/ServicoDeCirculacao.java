package br.pucpr.biblioteca.circulacao;

import br.pucpr.biblioteca.acesso.Movimentacao;
import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.acesso.iAcervo;
import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Reserva;
import br.pucpr.biblioteca.dominio.ReservaInvalidaException;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Facade (papel Facade). Coordena Relacionamento, Movimentacao, Acervo e Livro em
 * operacoes completas e transacionais, devolvendo comprovantes (DTOs).
 * As regras continuam no Livro (State) e nas classes do subsistema.
 */
public class ServicoDeCirculacao implements iCirculacao {
    private final UnidadeDeTrabalho transacao;
    private final Relacionamento relacionamento;
    private final Movimentacao movimentacao;
    private final iAcervo acervo;

    public ServicoDeCirculacao(UnidadeDeTrabalho transacao, Relacionamento relacionamento,
                               Movimentacao movimentacao, iAcervo acervo) {
        this.transacao = transacao;
        this.relacionamento = relacionamento;
        this.movimentacao = movimentacao;
        this.acervo = acervo;
    }

    @Override
    public ComprovanteReserva reservar(String idLivro, String idEstudante, LocalDate data) {
        return emTransacao(() -> {
            Estudante e = relacionamento.consultarEstudante(idEstudante);
            if (e == null) {
                throw new ReservaInvalidaException("estudante " + idEstudante + " nao cadastrado");
            }
            Livro livro = exigirLivro(idLivro);
            livro.reservar(e);                                   // LIVRE -> RESERVADO (o estado decide)
            movimentacao.inserir(new Reserva(data, idLivro, idEstudante));
            acervo.atualizar(livro);
            return new ComprovanteReserva(idLivro, idEstudante, data);
        });
    }

    @Override
    public ComprovanteReserva consultarReserva(String idLivro, String idEstudante) {
        Reserva r = movimentacao.consultarReserva(idLivro, idEstudante);
        if (r == null) {
            throw new ReservaInvalidaException("nao ha reserva do livro " + idLivro + " para o estudante " + idEstudante);
        }
        return new ComprovanteReserva(r.getIdLivro(), r.getIdEstudante(), r.getData());
    }

    /** Operacao atomica: valida a reserva, livro.emprestar, remove a reserva, registra o emprestimo, atualiza o Livro. */
    @Override
    public ComprovanteEmprestimo efetivarEmprestimo(String idLivro, String idEstudante, LocalDate data) {
        return emTransacao(() -> {
            Estudante e = relacionamento.consultarEstudante(idEstudante);
            Reserva r = movimentacao.consultarReserva(idLivro, idEstudante);
            exigirReservaDo(e, r, idLivro, idEstudante);
            if (data.isBefore(r.getData())) {                    // nada foi alterado ainda
                throw new ExcecaoDeDominio("data do emprestimo (" + data + ") anterior a data da reserva (" + r.getData() + ")");
            }
            Livro livro = exigirLivro(idLivro);
            livro.emprestar(e, data);                            // RESERVADO -> EMPRESTADO
            movimentacao.remover(r);
            movimentacao.inserir(new Emprestimo(data, idLivro, idEstudante));
            acervo.atualizar(livro);
            return new ComprovanteEmprestimo(idLivro, idEstudante, data, livro.getVencimento());
        });
    }

    @Override
    public ComprovanteRenovacao renovar(String idLivro, String idEstudante) {
        return emTransacao(() -> {
            exigirEmprestimo(idLivro, idEstudante);
            Livro livro = exigirLivro(idLivro);
            livro.renovar();                                     // guarda numRenovacoes < 2 esta no estado
            acervo.atualizar(livro);
            return new ComprovanteRenovacao(idLivro, idEstudante, livro.getVencimento(), livro.getNumRenovacoes());
        });
    }

    @Override
    public ComprovanteDevolucao devolver(String idLivro, String idEstudante, LocalDate data) {
        return emTransacao(() -> {
            Emprestimo emprestimo = exigirEmprestimo(idLivro, idEstudante);
            if (data.isBefore(emprestimo.getData())) {           // nada foi alterado ainda
                throw new ExcecaoDeDominio("data da devolucao (" + data + ") anterior a data do emprestimo (" + emprestimo.getData() + ")");
            }
            Estudante e = relacionamento.consultarEstudante(idEstudante);
            Livro livro = exigirLivro(idLivro);
            LocalDate vencimento = livro.getVencimento();
            livro.devolver();                                    // EMPRESTADO/RENOVADO -> DEVOLVIDO
            Devolucao d = new Devolucao(UUID.randomUUID().toString(), data, idLivro, idEstudante, vencimento);
            movimentacao.inserir(d);
            movimentacao.remover(emprestimo);
            acervo.atualizar(livro);
            return new ComprovanteDevolucao(d.getId(), idLivro, idEstudante, e == null ? null : e.getCpf(), vencimento, data);
        });
    }

    @Override
    public ComprovanteDevolucao consultarDevolucao(String idLivro, String idEstudante) {
        Devolucao d = movimentacao.consultarDevolucao(idLivro, idEstudante);
        if (d == null) {
            throw new ExcecaoDeDominio("nao ha devolucao do livro " + idLivro + " pelo estudante " + idEstudante);
        }
        Estudante e = relacionamento.consultarEstudante(idEstudante);
        return new ComprovanteDevolucao(d.getId(), idLivro, idEstudante, e == null ? null : e.getCpf(), d.getVencimento(), d.getData());
    }

    @Override
    public SituacaoDTO registrarInspecao(String idLivro, boolean emBomEstado) {
        return emTransacao(() -> {
            Livro livro = exigirLivro(idLivro);
            if (emBomEstado) {
                livro.aprovarInspecao();                         // DEVOLVIDO -> LIVRE
            } else {
                livro.reprovarInspecao();                        // DEVOLVIDO -> REPARANDO
            }
            acervo.atualizar(livro);
            return situacaoDe(livro);
        });
    }

    @Override
    public SituacaoDTO registrarRetornoDoReparo(String idLivro, boolean reparado) {
        return emTransacao(() -> {
            Livro livro = exigirLivro(idLivro);
            if (reparado) {
                livro.concluirReparo();                          // REPARANDO -> LIVRE
            } else {
                livro.registrarLaudoIrrecuperavel();             // REPARANDO -> INCINERADO
            }
            acervo.atualizar(livro);
            return situacaoDe(livro);
        });
    }

    @Override
    public SituacaoDTO consultarSituacao(String idLivro) {
        return situacaoDe(exigirLivro(idLivro));
    }

    // ---- apoio ----

    /**
     * Tudo ou nada: se qualquer passo falhar, as escritas ja feitas sao desfeitas.
     * E synchronized porque a unidade de trabalho (uma conexao, uma fotografia) e compartilhada.
     */
    private synchronized <T> T emTransacao(Supplier<T> operacao) {
        transacao.iniciar();
        try {
            T resultado = operacao.get();
            transacao.confirmar();
            return resultado;
        } catch (RuntimeException falha) {
            try {
                transacao.desfazer();
            } catch (RuntimeException falhaAoDesfazer) {
                falha.addSuppressed(falhaAoDesfazer);            // a falha original nao pode ser mascarada
            }
            throw falha;
        }
    }

    private void exigirReservaDo(Estudante e, Reserva r, String idLivro, String idEstudante) {
        if (e == null) {
            throw new ReservaInvalidaException("estudante " + idEstudante + " nao cadastrado");
        }
        if (r == null) {
            throw new ReservaInvalidaException("nao ha reserva do livro " + idLivro + " para o estudante " + idEstudante);
        }
    }

    private Emprestimo exigirEmprestimo(String idLivro, String idEstudante) {
        Emprestimo emprestimo = movimentacao.consultarEmprestimo(idLivro, idEstudante);
        if (emprestimo == null) {
            throw new ExcecaoDeDominio("nao ha emprestimo do livro " + idLivro + " para o estudante " + idEstudante);
        }
        return emprestimo;
    }

    private Livro exigirLivro(String idLivro) {
        Livro livro = acervo.consultar(idLivro);
        if (livro == null) {
            throw new ExcecaoDeDominio("livro " + idLivro + " nao cadastrado");
        }
        return livro;
    }

    private SituacaoDTO situacaoDe(Livro l) {
        return new SituacaoDTO(l.getId(), l.getTitulo(), l.getSituacao(), l.getVencimento(), l.getNumRenovacoes());
    }
}
