package br.pucpr.biblioteca.devolucao;

import br.pucpr.biblioteca.cadastro.iPessoa;
import br.pucpr.biblioteca.dominio.Pessoa;

/** ConcreteObserver: avisa o estudante por e-mail. Se o envio falhar, o publicador registra a falha. */
public class OuvinteEmail implements OuvinteDeDevolucao {
    private final ServicoDeEmail email;
    private final iPessoa pessoas;

    public OuvinteEmail(ServicoDeEmail email, iPessoa pessoas) {
        this.email = email;
        this.pessoas = pessoas;
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        Pessoa p = pessoas.consultarPessoa(evento.cpfEstudante());
        String destino = p == null ? "(sem e-mail)" : p.getEmail();
        email.enviar(destino, "Devolucao registrada",
                "livro " + evento.idLivro() + " devolvido em " + evento.devolvidoEm()
                        + "; dias de atraso: " + evento.diasAtraso());
    }
}
