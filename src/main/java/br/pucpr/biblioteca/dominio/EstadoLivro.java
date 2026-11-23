package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/**
 * State (papel State). Declara todos os eventos do ciclo de vida do Livro.
 * A implementacao padrao RECUSA o evento sem alterar o livro; cada estado
 * concreto sobrescreve apenas os eventos validos para ele.
 */
abstract class EstadoLivro {

    void reservar(Livro l, Estudante e) { throw recusa("reservar"); }

    void emprestar(Livro l, Estudante e, LocalDate hoje) { throw recusa("emprestar"); }

    void renovar(Livro l) { throw recusa("renovar"); }

    void devolver(Livro l) { throw recusa("devolver"); }

    void aprovarInspecao(Livro l) { throw recusa("aprovar inspecao"); }

    void reprovarInspecao(Livro l) { throw recusa("reprovar inspecao"); }

    void enviarParaReparo(Livro l) { throw recusa("enviar para reparo"); }

    void concluirReparo(Livro l) { throw recusa("concluir reparo"); }

    void registrarLaudoIrrecuperavel(Livro l) { throw recusa("registrar laudo irrecuperavel"); }

    void excluir(Livro l) { throw recusa("excluir"); }

    abstract Situacao situacao();

    protected OperacaoInvalidaException recusa(String operacao) {
        return new OperacaoInvalidaException(situacao(), operacao);
    }

    /** Reconstitui o objeto de estado a partir do valor gravado na persistencia. */
    static EstadoLivro de(Situacao s) {
        switch (s) {
            case LIVRE: return Livre.INSTANCIA;
            case RESERVADO: return Reservado.INSTANCIA;
            case EMPRESTADO: return Emprestado.INSTANCIA;
            case RENOVADO: return Renovado.INSTANCIA;
            case DEVOLVIDO: return Devolvido.INSTANCIA;
            case REPARANDO: return Reparando.INSTANCIA;
            case INCINERADO: return Incinerado.INSTANCIA;
            default: throw new IllegalArgumentException("situacao sem estado correspondente: " + s);
        }
    }
}
