package br.pucpr.biblioteca.consulta;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Decorator (ConcreteDecorator): guarda os metadados por um tempo de validade.
 * So guarda id e titulo; a decisao de reserva nunca passa por aqui.
 * O cache e opcional ate na falha: se o armazenamento falhar ao ler ou ao gravar,
 * a consulta e atendida pelo componente interno, chamado uma unica vez.
 */
public class DecoradorDeCache extends DecoradorDeConsulta {
    private final ArmazenamentoDeCache armazenamento;
    private final Consumer<String> aoAcertar;
    private final AtomicInteger acertos = new AtomicInteger();

    public DecoradorDeCache(ConsultaAcervo interno, Duration validade) {
        this(interno, new ArmazenamentoEmMemoria(validade));
    }

    public DecoradorDeCache(ConsultaAcervo interno, ArmazenamentoDeCache armazenamento) {
        this(interno, armazenamento, termo -> { });
    }

    /** aoAcertar e avisado a cada consulta atendida pelo cache (por exemplo, o registro de metricas). */
    public DecoradorDeCache(ConsultaAcervo interno, ArmazenamentoDeCache armazenamento, Consumer<String> aoAcertar) {
        super(interno);
        this.armazenamento = armazenamento;
        this.aoAcertar = aoAcertar;
    }

    @Override
    public List<MetadadosLivro> buscarMetadados(String termo) {
        List<MetadadosLivro> guardada = lerDoCache(termo);       // 1) tenta o cache; falha conta como ausencia
        if (guardada != null) {
            acertos.incrementAndGet();
            avisarAcerto(termo);
            return guardada;
        }
        List<MetadadosLivro> lista = List.copyOf(interno.buscarMetadados(termo));   // 2) consulta real, uma unica vez
        gravarNoCache(termo, lista);                             // 3) tenta guardar; falha e ignorada
        return lista;
    }

    /** Descarta o que esta guardado; a proxima consulta volta ao componente interno. */
    public void invalidar() {
        try {
            armazenamento.limpar();
        } catch (RuntimeException falhaDoCache) {
            // sem cache: nada a descartar
        }
    }

    public int acertos() { return acertos.get(); }

    private List<MetadadosLivro> lerDoCache(String termo) {
        try {
            return armazenamento.ler(termo);
        } catch (RuntimeException falhaDoCache) {
            return null;
        }
    }

    private void gravarNoCache(String termo, List<MetadadosLivro> lista) {
        try {
            armazenamento.gravar(termo, lista);
        } catch (RuntimeException falhaDoCache) {
            // a consulta ja foi respondida; segue sem guardar
        }
    }

    private void avisarAcerto(String termo) {
        try {
            aoAcertar.accept(termo);
        } catch (RuntimeException ignorada) {
            // quem observa o cache nunca derruba a consulta
        }
    }
}
