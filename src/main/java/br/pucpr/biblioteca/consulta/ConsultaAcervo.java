package br.pucpr.biblioteca.consulta;

import java.util.List;

/** Decorator (papel Component). Contrato unico de consulta de metadados do acervo. */
public interface ConsultaAcervo {
    List<MetadadosLivro> buscarMetadados(String termo);
}
