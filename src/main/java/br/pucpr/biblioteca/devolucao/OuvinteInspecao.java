package br.pucpr.biblioteca.devolucao;

/** ConcreteObserver: agenda a inspecao. O livro continua DEVOLVIDO ate a decisao do atendente. */
public class OuvinteInspecao implements OuvinteDeDevolucao {
    private final AgendaDeInspecao agenda;

    public OuvinteInspecao(AgendaDeInspecao agenda) {
        this.agenda = agenda;
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        agenda.agendar(evento.idLivro());
    }
}
