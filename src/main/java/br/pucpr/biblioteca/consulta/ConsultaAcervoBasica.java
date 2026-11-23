package br.pucpr.biblioteca.consulta;

import br.pucpr.biblioteca.acesso.iAcervo;
import br.pucpr.biblioteca.dominio.Livro;
import java.util.ArrayList;
import java.util.List;

/** Decorator (papel ConcreteComponent). Implementacao original: le o Acervo; nao sabe de cache nem de medicao. */
public class ConsultaAcervoBasica implements ConsultaAcervo {
    private final iAcervo acervo;

    public ConsultaAcervoBasica(iAcervo acervo) {
        this.acervo = acervo;
    }

    @Override
    public List<MetadadosLivro> buscarMetadados(String termo) {
        String procurado = termo == null ? "" : termo.toLowerCase();
        List<MetadadosLivro> achados = new ArrayList<>();
        for (Livro l : acervo.listar()) {
            if (l.getTitulo().toLowerCase().contains(procurado)) {
                achados.add(new MetadadosLivro(l.getId(), l.getTitulo()));
            }
        }
        return achados;
    }
}
