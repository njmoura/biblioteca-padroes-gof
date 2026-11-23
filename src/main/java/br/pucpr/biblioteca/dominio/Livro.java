package br.pucpr.biblioteca.dominio;

import java.time.LocalDate;

/**
 * State (papel Context). Guarda o estado corrente e os dados do ciclo atual
 * (vencimento, numRenovacoes, reservante) e delega cada evento ao objeto de estado.
 */
public class Livro {
    public static final int PRAZO_EMPRESTIMO_DIAS = 7;
    public static final int DIAS_POR_RENOVACAO = 30;
    public static final int MAX_RENOVACOES = 2;

    private final String id;
    private String titulo;
    private EstadoLivro estado;
    private LocalDate vencimento;      // [0..1]
    private int numRenovacoes;
    private String reservante;         // id do estudante do ciclo atual [0..1]

    /** Livro novo: nasce LIVRE (transicao inicial / inserirLivro). */
    public Livro(String id, String titulo) {
        this(id, titulo, Situacao.LIVRE, null, 0, null);
    }

    /** Reconstituicao a partir da persistencia: a Situacao gravada vira o objeto de estado. */
    public Livro(String id, String titulo, Situacao situacao, LocalDate vencimento, int numRenovacoes, String reservante) {
        this.id = id;
        this.titulo = titulo;
        this.estado = EstadoLivro.de(situacao);
        this.vencimento = vencimento;
        this.numRenovacoes = numRenovacoes;
        this.reservante = reservante;
    }

    public Livro copia() {
        return new Livro(id, titulo, getSituacao(), vencimento, numRenovacoes, reservante);
    }

    public void atualizar(String titulo) { this.titulo = titulo; }

    // ---- eventos da maquina de estados: o Livro apenas delega ao estado corrente ----
    public void reservar(Estudante e) { estado.reservar(this, e); }

    public void emprestar(Estudante e, LocalDate hoje) { estado.emprestar(this, e, hoje); }

    public void renovar() { estado.renovar(this); }

    public void devolver() { estado.devolver(this); }

    public void aprovarInspecao() { estado.aprovarInspecao(this); }

    public void reprovarInspecao() { estado.reprovarInspecao(this); }

    public void enviarParaReparo() { estado.enviarParaReparo(this); }

    public void concluirReparo() { estado.concluirReparo(this); }

    public void registrarLaudoIrrecuperavel() { estado.registrarLaudoIrrecuperavel(this); }

    public void excluir() { estado.excluir(this); }

    // ---- consultas ----
    public String getId() { return id; }

    public String getTitulo() { return titulo; }

    public Situacao getSituacao() { return estado.situacao(); }

    public LocalDate getVencimento() { return vencimento; }

    public int getNumRenovacoes() { return numRenovacoes; }

    public String getReservante() { return reservante; }

    // ---- operacoes de pacote (~): so os estados alteram o Livro ----
    void mudarPara(EstadoLivro novo) { this.estado = novo; }

    void prorrogar(int dias) {
        this.vencimento = this.vencimento.plusDays(dias);
        this.numRenovacoes++;
    }

    void limparCiclo() {
        this.vencimento = null;
        this.numRenovacoes = 0;
        this.reservante = null;
    }

    void definirReservante(String idEstudante) { this.reservante = idEstudante; }

    void definirVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    @Override
    public String toString() {
        return "Livro " + id + " \"" + titulo + "\" [" + getSituacao() + "]"
                + (vencimento != null
                        ? ", vencimento " + vencimento + ", " + numRenovacoes + (numRenovacoes == 1 ? " renovacao" : " renovacoes")
                        : "");
    }
}
