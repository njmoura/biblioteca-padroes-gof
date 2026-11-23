package br.pucpr.biblioteca.solicitacao;

import br.pucpr.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import java.time.LocalDate;
import java.util.UUID;

/** Command (ConcreteCommand): guarda os parametros da reserva e chama o receptor. */
public class SolicitacaoDeReserva extends SolicitacaoDeCirculacao {
    private final String idLivro;
    private final String idEstudante;
    private final LocalDate data;

    public SolicitacaoDeReserva(UUID id, String autor, String idLivro, String idEstudante, LocalDate data) {
        super(id, autor);
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
        this.data = data;
    }

    @Override
    public ResultadoSolicitacao executar(iCirculacao circulacao) {
        ComprovanteReserva c = circulacao.reservar(idLivro, idEstudante, data);
        return ResultadoSolicitacao.executada(getId(), "livro " + c.idLivro() + " reservado para " + c.idEstudante());
    }
}
