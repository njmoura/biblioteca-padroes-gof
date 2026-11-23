package br.pucpr.biblioteca.devolucao;

/** Modulo de indicadores: acompanha as devolucoes. */
public class PainelDeIndicadores {
    private int devolucoes;
    private int devolucoesComAtraso;
    private int diasDeAtrasoAcumulados;

    public void registrar(int diasAtraso) {
        devolucoes++;
        if (diasAtraso > 0) {
            devolucoesComAtraso++;
            diasDeAtrasoAcumulados += diasAtraso;
        }
    }

    public int getDevolucoes() { return devolucoes; }

    public int getDevolucoesComAtraso() { return devolucoesComAtraso; }

    public int getDiasDeAtrasoAcumulados() { return diasDeAtrasoAcumulados; }

    @Override
    public String toString() {
        return devolucoes + (devolucoes == 1 ? " devolucao, " : " devolucoes, ") + devolucoesComAtraso + " com atraso, "
                + diasDeAtrasoAcumulados
                + (diasDeAtrasoAcumulados == 1 ? " dia de atraso acumulado" : " dias de atraso acumulados");
    }
}
