package br.pucpr.biblioteca.consulta;

import java.util.List;

/** Decorator (papel Decorator). Realiza o contrato, guarda a referencia "interno" e, por padrao, so delega. */
public abstract class DecoradorDeConsulta implements ConsultaAcervo {
    protected final ConsultaAcervo interno;

    protected DecoradorDeConsulta(ConsultaAcervo interno) {
        this.interno = interno;
    }

    @Override
    public List<MetadadosLivro> buscarMetadados(String termo) {
        return interno.buscarMetadados(termo);
    }
}
