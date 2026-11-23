package br.pucpr.biblioteca;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.devolucao.ServicoDeEmail;
import br.pucpr.biblioteca.persistencia.ConfiguradorDePersistencia;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.Perfil;
import java.time.LocalDate;

/** Apoio aos testes: monta a aplicacao no perfil TESTE (familia em memoria) com cadastros basicos. */
final class Cenario {
    static final LocalDate DIA = LocalDate.of(2026, 11, 1);          // emprestimo neste dia vence em 08/11
    static final LocalDate VENCIMENTO = DIA.plusDays(7);
    static final String CPF_ANA = "11111111111";
    static final String CPF_BRUNO = "22222222222";

    private Cenario() { }

    static Aplicacao novo() {
        return novo(CodigoBanco.A, (para, assunto, texto) -> { });
    }

    static Aplicacao novo(CodigoBanco banco, ServicoDeEmail email) {
        return novo(new ConfiguradorDePersistencia().fabricaPara(Perfil.TESTE), banco, email);
    }

    static Aplicacao novo(FabricaDePersistencia fabrica, CodigoBanco banco, ServicoDeEmail email) {
        Aplicacao app = new Aplicacao(fabrica, banco, email);
        app.tratadorDePessoa.inserirPessoa(CPF_ANA, "Ana", "Silva", "ana@exemplo.br");
        app.tratadorDePessoa.inserirPessoa(CPF_BRUNO, "Bruno", "Souza", "bruno@exemplo.br");
        app.tratadorDeEstudante.inserirEstudante("E1", CPF_ANA);
        app.tratadorDeEstudante.inserirEstudante("E2", CPF_BRUNO);
        app.tratadorDeAcervo.inserirLivro("L1", "Dom Casmurro");
        app.tratadorDeAcervo.inserirLivro("L2", "O Cortico");
        return app;
    }

    /** Reserva e empresta o livro ao estudante em DIA (vencimento em VENCIMENTO). */
    static void emprestar(Aplicacao app, String idLivro, String idEstudante) {
        app.tratadorDeEmprestimo.reservarLivro(idLivro, idEstudante, DIA);
        app.tratadorDeEmprestimo.emprestarLivro(idLivro, idEstudante, DIA);
    }
}
