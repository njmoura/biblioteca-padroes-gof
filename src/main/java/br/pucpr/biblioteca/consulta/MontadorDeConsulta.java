package br.pucpr.biblioteca.consulta;

import java.time.Duration;

/**
 * Monta a cadeia de decoradores conforme a instalacao: medicao e cache sao escolhas
 * independentes. A ordem importa: a medicao fica por fora, entao a consulta atendida
 * pelo cache tambem e medida.
 */
public class MontadorDeConsulta {
    public static final Duration VALIDADE_DO_CACHE = Duration.ofMinutes(5);

    private DecoradorDeCache cache;          // o cache da cadeia montada, se a instalacao tiver um

    /** Instalacao com medicao: Medicao(Cache(Basica)) ou, sem cache, Medicao(Basica). */
    public ConsultaAcervo montar(ConsultaAcervo basica, boolean comCache, RegistroDeMetricas metricas) {
        return montar(basica, true, comCache, metricas);
    }

    /** As quatro instalacoes possiveis: Basica, Cache(Basica), Medicao(Basica) e Medicao(Cache(Basica)). */
    public ConsultaAcervo montar(ConsultaAcervo basica, boolean comMedicao, boolean comCache, RegistroDeMetricas metricas) {
        ConsultaAcervo consulta = basica;
        cache = null;
        if (comCache) {
            cache = new DecoradorDeCache(consulta, new ArmazenamentoEmMemoria(VALIDADE_DO_CACHE),
                    metricas::registrarAcertoDeCache);
            consulta = cache;                                                // Cache(Basica)
        }
        if (comMedicao) {
            consulta = new DecoradorDeMedicao(consulta, metricas);           // a medicao por fora
        }
        return consulta;
    }

    /** Descarta o que o cache da cadeia montada guarda (o acervo mudou). Sem cache, nao faz nada. */
    public void invalidarCache() {
        if (cache != null) {
            cache.invalidar();
        }
    }
}
