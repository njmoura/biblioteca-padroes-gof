package br.pucpr.biblioteca.solicitacao;

import br.pucpr.biblioteca.circulacao.iCirculacao;
import java.time.Instant;
import java.util.UUID;

/**
 * Command (papel Command). Uma solicitacao de circulacao como objeto: identificador,
 * autor, instante de criacao e a operacao executar(). Nao ha desfazer(): a entrega
 * fisica do livro nao e revertida por software.
 */
public abstract class SolicitacaoDeCirculacao {
    private final UUID id;
    private final String autor;
    private final Instant criadaEm;

    protected SolicitacaoDeCirculacao(UUID id, String autor) {
        if (id == null) {
            throw new IllegalArgumentException("a solicitacao precisa de um identificador");
        }
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("a solicitacao precisa de um autor");
        }
        this.id = id;
        this.autor = autor;
        this.criadaEm = Instant.now();
    }

    /** Executa a solicitacao no receptor; as regras ficam no dominio, nao aqui. */
    public abstract ResultadoSolicitacao executar(iCirculacao circulacao);

    public UUID getId() { return id; }

    public String getAutor() { return autor; }

    public Instant getCriadaEm() { return criadaEm; }
}
