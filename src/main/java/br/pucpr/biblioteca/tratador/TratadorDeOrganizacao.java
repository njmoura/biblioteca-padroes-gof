package br.pucpr.biblioteca.tratador;

import br.pucpr.biblioteca.cadastro.iLivro;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.organizacao.OrganizadorDoAcervo;
import br.pucpr.biblioteca.organizacao.TipoDeGrupo;
import java.util.List;

/** Tratador do Administrador para organizar o acervo em unidades, secoes e subsecoes (Composite). */
public class TratadorDeOrganizacao {
    private final OrganizadorDoAcervo organizador;
    private final iLivro livros;

    public TratadorDeOrganizacao(OrganizadorDoAcervo organizador, iLivro livros) {
        this.organizador = organizador;
        this.livros = livros;
    }

    /** Para a tela: o tipo chega como texto digitado e e conferido aqui. */
    public Resultado criarGrupo(String nomeDoPai, String nome, String tipo) {
        TipoDeGrupo tipoDeGrupo;
        try {
            tipoDeGrupo = TipoDeGrupo.valueOf(tipo == null ? "" : tipo.trim().toUpperCase());
        } catch (IllegalArgumentException tipoDesconhecido) {
            return Resultado.recusado("tipo invalido: use UNIDADE, SECAO ou SUBSECAO");
        }
        return criarGrupo(nomeDoPai, nome, tipoDeGrupo);
    }

    public Resultado criarGrupo(String nomeDoPai, String nome, TipoDeGrupo tipo) {
        try {
            organizador.criarGrupo(nomeDoPai, nome, tipo);
            return Resultado.ok(tipo + " " + nome + " criada dentro de " + nomeDoPai);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    public Resultado alocarLivro(String idLivro, String nomeDoGrupo) {
        if (livros.consultarLivro(idLivro) == null) {
            return Resultado.recusado("livro " + idLivro + " nao cadastrado");
        }
        try {
            organizador.alocarLivro(idLivro, nomeDoGrupo);
            return Resultado.ok("livro " + idLivro + " alocado em " + nomeDoGrupo);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    public Resultado moverLivro(String idLivro, String nomeDoDestino) {
        try {
            organizador.mover(idLivro, nomeDoDestino);
            return Resultado.ok("livro " + idLivro + " movido para " + nomeDoDestino);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    /** Conta e lista os livros de um grupo (pelo nome) ou de um item (pelo id do livro). */
    public Resultado contarEListar(String nome) {
        try {
            int total = organizador.contarLivros(nome);
            List<String> ids = organizador.listarIds(nome);
            String quantidade = total == 0 ? "nenhum livro"
                    : total + (total == 1 ? " livro (" : " livros (") + String.join(", ", ids) + ")";
            return Resultado.ok(nome + ": " + quantidade);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    public String raiz() {
        return organizador.getRaiz().getNome();
    }
}
