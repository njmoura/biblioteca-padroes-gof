package br.pucpr.biblioteca.tratador;

import br.pucpr.biblioteca.consulta.ConsultaAcervo;
import br.pucpr.biblioteca.consulta.MetadadosLivro;
import br.pucpr.biblioteca.consulta.MontadorDeConsulta;
import br.pucpr.biblioteca.consulta.RegistroDeMetricas;
import java.util.List;

/** Consulta publica ao acervo. Conhece apenas o contrato ConsultaAcervo (Decorator: cliente). */
public class TratadorDeConsulta {
    private final ConsultaAcervo consulta;
    private final RegistroDeMetricas metricas;
    private final Runnable descartarCache;

    public TratadorDeConsulta(ConsultaAcervo consulta, RegistroDeMetricas metricas) {
        this(consulta, metricas, () -> { });
    }

    /** descartarCache e o que a montagem oferece para esvaziar o cache da instalacao (se houver). */
    public TratadorDeConsulta(ConsultaAcervo consulta, RegistroDeMetricas metricas, Runnable descartarCache) {
        this.consulta = consulta;
        this.metricas = metricas;
        this.descartarCache = descartarCache;
    }

    public List<MetadadosLivro> consultar(String termo) {
        return consulta.buscarMetadados(termo);
    }

    public int consultasMedidas() {
        return metricas.total();
    }

    /** Para a tela: numero da ultima consulta medida e de onde veio a resposta. */
    public String resumoDaUltimaConsulta() {
        RegistroDeMetricas.Medicao ultima = metricas.ultima();
        if (ultima == null) {
            return "nenhuma consulta medida (instalacao sem medicao)";
        }
        return "consulta medida n. " + metricas.total() + "; "
                + (ultima.atendidaPeloCache()
                        ? "resposta do cache (validade de " + MontadorDeConsulta.VALIDADE_DO_CACHE.toMinutes() + " min)"
                        : "lida do acervo");
    }

    /** Um livro foi incluido, alterado ou excluido: o que o cache guarda deixa de valer. */
    public void acervoAlterado() {
        descartarCache.run();
    }
}
