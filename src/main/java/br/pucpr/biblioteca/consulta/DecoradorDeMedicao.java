package br.pucpr.biblioteca.consulta;

import java.util.List;

/** Decorator (ConcreteDecorator): mede o tempo de resposta do componente que embrulha. */
public class DecoradorDeMedicao extends DecoradorDeConsulta {
    private final RegistroDeMetricas metricas;

    public DecoradorDeMedicao(ConsultaAcervo interno, RegistroDeMetricas metricas) {
        super(interno);
        this.metricas = metricas;
    }

    @Override
    public List<MetadadosLivro> buscarMetadados(String termo) {
        long inicio = System.nanoTime();
        try {
            return interno.buscarMetadados(termo);
        } finally {
            registrar(termo, System.nanoTime() - inicio);
        }
    }

    /** Falha ao registrar metricas nunca derruba a consulta. */
    private void registrar(String termo, long nanos) {
        try {
            metricas.registrar(termo, nanos);
        } catch (RuntimeException ignorada) {
            // segue sem a medicao
        }
    }
}
