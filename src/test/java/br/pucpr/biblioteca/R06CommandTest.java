package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.solicitacao.RegistroDeSolicitacoes;
import br.pucpr.biblioteca.solicitacao.ResultadoSolicitacao;
import br.pucpr.biblioteca.solicitacao.ResultadoSolicitacao.Status;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeCirculacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeReserva;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Requisito 6 - Solicitacoes de circulacao como objetos (Command). */
class R06CommandTest {

    @Test
    void R06_renovacaoExecutadaDuasVezesComMesmoIdSomaApenas30Dias() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        SolicitacaoDeRenovacao s42 = new SolicitacaoDeRenovacao(UUID.randomUUID(), "app do estudante", "L1", "E1");

        ResultadoSolicitacao primeira = app.tratadorDeEmprestimo.submeter(s42);
        ResultadoSolicitacao repetida = app.tratadorDeEmprestimo.submeter(s42);   // reenvio da MESMA solicitacao

        assertEquals(Status.EXECUTADA, primeira.status());
        assertFalse(primeira.repetida());
        assertTrue(repetida.repetida());                                      // nada foi executado de novo
        assertEquals(primeira.status(), repetida.status());                   // devolve o resultado gravado
        assertEquals(primeira.mensagem(), repetida.mensagem());
        assertEquals(primeira.id(), repetida.id());
        Livro livro = app.acervo.consultar("L1");
        assertEquals(Cenario.VENCIMENTO.plusDays(30), livro.getVencimento()); // so +30 dias
        assertEquals(1, livro.getNumRenovacoes());
    }

    @Test
    void R06_solicitacoesDiferentesContinuamSujeitasAoLimiteDeDuasRenovacoes() {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");

        Status s1 = app.tratadorDeEmprestimo.submeter(new SolicitacaoDeRenovacao(UUID.randomUUID(), "balcao", "L1", "E1")).status();
        Status s2 = app.tratadorDeEmprestimo.submeter(new SolicitacaoDeRenovacao(UUID.randomUUID(), "balcao", "L1", "E1")).status();
        SolicitacaoDeRenovacao terceira = new SolicitacaoDeRenovacao(UUID.randomUUID(), "balcao", "L1", "E1");
        ResultadoSolicitacao r3 = app.tratadorDeEmprestimo.submeter(terceira);

        assertEquals(Status.EXECUTADA, s1);
        assertEquals(Status.EXECUTADA, s2);
        assertEquals(Status.RECUSADA, r3.status());                           // a regra continua no dominio (Livro)
        ResultadoSolicitacao r3Repetida = app.tratadorDeEmprestimo.submeter(terceira);
        assertEquals(Status.RECUSADA, r3Repetida.status());                   // a recusa tambem fica gravada
        assertEquals(r3.mensagem(), r3Repetida.mensagem());
        assertTrue(r3Repetida.repetida());
        assertEquals(Cenario.VENCIMENTO.plusDays(60), app.acervo.consultar("L1").getVencimento());
    }

    @Test
    void R06_solicitacaoTemIdentificadorAutorEInstanteEReservaPeloExecutor() {
        Aplicacao app = Cenario.novo();
        UUID id = UUID.randomUUID();
        SolicitacaoDeReserva s = new SolicitacaoDeReserva(id, "tela de balcao", "L1", "E1", Cenario.DIA);

        ResultadoSolicitacao r = app.tratadorDeEmprestimo.submeter(s);

        assertEquals(id, s.getId());
        assertEquals("tela de balcao", s.getAutor());
        assertNotNull(s.getCriadaEm());
        assertEquals(Status.EXECUTADA, r.status());
        assertEquals(Situacao.RESERVADO, app.acervo.consultar("L1").getSituacao());
    }

    @Test
    void R06_duasSubmissoesSimultaneasDoMesmoIdExecutamUmaVezSo() throws Exception {
        Aplicacao app = Cenario.novo();
        Cenario.emprestar(app, "L1", "E1");
        AtomicInteger execucoes = new AtomicInteger();
        CountDownLatch dentroDoExecutar = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        // renovacao que para no meio do executar(), para a segunda submissao chegar com a primeira em andamento
        SolicitacaoDeCirculacao lenta = new SolicitacaoDeCirculacao(UUID.randomUUID(), "app do estudante") {
            @Override
            public ResultadoSolicitacao executar(iCirculacao circulacao) {
                execucoes.incrementAndGet();
                dentroDoExecutar.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new SolicitacaoDeRenovacao(getId(), getAutor(), "L1", "E1").executar(circulacao);
            }
        };
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<ResultadoSolicitacao> primeira = threads.submit(() -> app.tratadorDeEmprestimo.submeter(lenta));
            assertTrue(dentroDoExecutar.await(5, TimeUnit.SECONDS));              // a primeira esta executando
            Future<ResultadoSolicitacao> segunda = threads.submit(() -> app.tratadorDeEmprestimo.submeter(lenta));

            ResultadoSolicitacao respostaDaSegunda = segunda.get(5, TimeUnit.SECONDS);   // responde sem esperar a primeira
            assertEquals(Status.EM_PROCESSAMENTO, respostaDaSegunda.status());
            liberar.countDown();
            assertEquals(Status.EXECUTADA, primeira.get(5, TimeUnit.SECONDS).status());
        } finally {
            liberar.countDown();
            threads.shutdownNow();
        }

        assertEquals(1, execucoes.get());                                     // executou uma vez so
        assertEquals(1, app.acervo.consultar("L1").getNumRenovacoes());
        assertEquals(Cenario.VENCIMENTO.plusDays(30), app.acervo.consultar("L1").getVencimento());
        assertTrue(app.tratadorDeEmprestimo.submeter(lenta).repetida());      // depois, devolve o resultado gravado
        assertEquals(1, execucoes.get());
    }

    @Test
    void R06_registroEntregaAExecucaoAUmaUnicaSubmissaoEntreVariasSimultaneas() throws Exception {
        RegistroDeSolicitacoes registro = new RegistroDeSolicitacoes();
        UUID id = UUID.randomUUID();
        int submissoes = 32;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(submissoes);
        try {
            List<Future<Boolean>> respostas = new ArrayList<>();
            for (int i = 0; i < submissoes; i++) {
                respostas.add(threads.submit(() -> {
                    largada.await();
                    return registro.reservarExecucao(id);
                }));
            }
            largada.countDown();
            int ganhadoras = 0;
            for (Future<Boolean> resposta : respostas) {
                if (resposta.get(5, TimeUnit.SECONDS)) {
                    ganhadoras++;
                }
            }
            assertEquals(1, ganhadoras);                                      // insercao atomica: so uma recebe true
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    void R06_solicitacaoSemIdentificadorOuSemAutorNaoECriada() {
        assertThrows(IllegalArgumentException.class, () -> new SolicitacaoDeRenovacao(null, "balcao", "L1", "E1"));
        assertThrows(IllegalArgumentException.class, () -> new SolicitacaoDeReserva(UUID.randomUUID(), " ", "L1", "E1", Cenario.DIA));
    }
}
