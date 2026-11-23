package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.consulta.ArmazenamentoDeCache;
import br.pucpr.biblioteca.consulta.ConsultaAcervo;
import br.pucpr.biblioteca.consulta.ConsultaAcervoBasica;
import br.pucpr.biblioteca.consulta.DecoradorDeCache;
import br.pucpr.biblioteca.consulta.DecoradorDeMedicao;
import br.pucpr.biblioteca.consulta.MetadadosLivro;
import br.pucpr.biblioteca.consulta.MontadorDeConsulta;
import br.pucpr.biblioteca.consulta.RegistroDeMetricas;
import br.pucpr.biblioteca.dominio.Situacao;
import br.pucpr.biblioteca.tratador.TratadorDeConsulta;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Requisito 9 - Servico de consulta com recursos opcionais (Decorator). */
class R09DecoratorTest {

    @Test
    void R09_segundaConsultaUsaCacheEAindaGeraMedicao() {
        Aplicacao app = Cenario.novo();
        AtomicInteger idasAoAcervo = new AtomicInteger();
        ConsultaAcervo basica = new ConsultaAcervoBasica(app.acervo);
        ConsultaAcervo basicaContada = termo -> {                             // conta quantas vezes a original foi chamada
            idasAoAcervo.incrementAndGet();
            return basica.buscarMetadados(termo);
        };
        RegistroDeMetricas metricas = new RegistroDeMetricas();
        ConsultaAcervo consulta = new MontadorDeConsulta().montar(basicaContada, true, metricas);   // Medicao(Cache(Basica))

        List<MetadadosLivro> primeira = consulta.buscarMetadados("Dom");
        List<MetadadosLivro> segunda = consulta.buscarMetadados("Dom");

        assertEquals(List.of(new MetadadosLivro("L1", "Dom Casmurro")), primeira);
        assertEquals(primeira, segunda);
        assertEquals(1, idasAoAcervo.get());                                  // a segunda veio do cache
        assertEquals(2, metricas.total());                                    // e mesmo assim foi medida
    }

    @Test
    void R09_instalacaoSoComMedicaoNaoUsaCache() {
        Aplicacao app = Cenario.novo();
        AtomicInteger idasAoAcervo = new AtomicInteger();
        ConsultaAcervo basica = new ConsultaAcervoBasica(app.acervo);
        RegistroDeMetricas metricas = new RegistroDeMetricas();
        ConsultaAcervo consulta = new MontadorDeConsulta().montar(termo -> {
            idasAoAcervo.incrementAndGet();
            return basica.buscarMetadados(termo);
        }, false, metricas);                                                  // Medicao(Basica)

        consulta.buscarMetadados("Dom");
        consulta.buscarMetadados("Dom");

        assertEquals(2, idasAoAcervo.get());
        assertEquals(2, metricas.total());
    }

    @Test
    void R09_resultadoAntigoDoCacheNaoAutorizaReservaDeLivroQueNaoEstaLivre() {
        Aplicacao app = Cenario.novo();
        List<MetadadosLivro> antes = app.tratadorDeConsulta.consultar("Dom");   // popula o cache enquanto L1 esta LIVRE
        Cenario.emprestar(app, "L1", "E1");                                     // L1 deixa de estar LIVRE

        List<MetadadosLivro> doCache = app.tratadorDeConsulta.consultar("Dom");
        boolean reservou = app.tratadorDeEmprestimo.reservarLivro("L1", "E2", Cenario.DIA).sucesso();

        assertEquals(antes, doCache);                                         // o cache so tem id e titulo
        assertEquals(2, app.tratadorDeConsulta.consultasMedidas());
        assertFalse(reservou);                                                // quem decide e o estado atual do Livro
        assertEquals(Situacao.EMPRESTADO, app.acervo.consultar("L1").getSituacao());
        assertTrue(app.tratadorDeEmprestimo.reservarLivro("L2", "E2", Cenario.DIA).sucesso());
    }

    @Test
    void R09_aOrdemDosDecoradoresImporta() {
        Aplicacao app = Cenario.novo();
        ConsultaAcervo basica = new ConsultaAcervoBasica(app.acervo);
        RegistroDeMetricas metricas = new RegistroDeMetricas();
        // ordem invertida, montada a mao: Cache(Medicao(Basica)) - a medicao fica por dentro
        ConsultaAcervo invertida = new DecoradorDeCache(new DecoradorDeMedicao(basica, metricas), MontadorDeConsulta.VALIDADE_DO_CACHE);

        invertida.buscarMetadados("Dom");
        invertida.buscarMetadados("Dom");

        assertEquals(1, metricas.total());                                    // a consulta atendida pelo cache nao foi medida
    }

    @Test
    void R09_instalacaoPodeCombinarOsDecoradoresComoQuiser() {
        Aplicacao app = Cenario.novo();
        ConsultaAcervo basica = new ConsultaAcervoBasica(app.acervo);
        RegistroDeMetricas metricas = new RegistroDeMetricas();
        MontadorDeConsulta montador = new MontadorDeConsulta();

        assertSame(basica, montador.montar(basica, false, false, metricas));                           // Basica
        assertInstanceOf(DecoradorDeCache.class, montador.montar(basica, false, true, metricas));      // Cache(Basica)
        assertInstanceOf(DecoradorDeMedicao.class, montador.montar(basica, true, false, metricas));    // Medicao(Basica)
        assertInstanceOf(DecoradorDeMedicao.class, montador.montar(basica, true, true, metricas));     // Medicao(Cache(Basica))
    }

    @Test
    void R09_falhaDoCacheNaoDerrubaAConsulta() {
        Aplicacao app = Cenario.novo();
        AtomicInteger idasAoAcervo = new AtomicInteger();
        ConsultaAcervo basica = new ConsultaAcervoBasica(app.acervo);
        ConsultaAcervo basicaContada = termo -> {
            idasAoAcervo.incrementAndGet();
            return basica.buscarMetadados(termo);
        };
        ArmazenamentoDeCache foraDoAr = new ArmazenamentoDeCache() {          // falha ao ler, ao gravar e ao limpar
            @Override
            public List<MetadadosLivro> ler(String termo) { throw new IllegalStateException("cache fora do ar"); }

            @Override
            public void gravar(String termo, List<MetadadosLivro> lista) { throw new IllegalStateException("cache fora do ar"); }

            @Override
            public void limpar() { throw new IllegalStateException("cache fora do ar"); }
        };
        DecoradorDeCache cache = new DecoradorDeCache(basicaContada, foraDoAr);

        List<MetadadosLivro> resposta = cache.buscarMetadados("Dom");

        assertEquals(List.of(new MetadadosLivro("L1", "Dom Casmurro")), resposta);   // a consulta respondeu
        assertEquals(1, idasAoAcervo.get());                                  // e a original foi chamada uma unica vez
        assertEquals(0, cache.acertos());
        cache.invalidar();                                                    // nem a limpeza derruba
    }

    @Test
    void R09_cacheGuardaApenasMetadados() {
        List<String> componentes = new ArrayList<>();
        for (RecordComponent c : MetadadosLivro.class.getRecordComponents()) {
            componentes.add(c.getName());
        }

        assertEquals(List.of("id", "titulo"), componentes);                   // nada de situacao, vencimento ou reservante
    }

    @Test
    void R09_telaSabeSeARespostaVeioDoCacheEAlteracaoDoAcervoODescarta() {
        Aplicacao app = Cenario.novo();
        RegistroDeMetricas metricas = new RegistroDeMetricas();
        MontadorDeConsulta montador = new MontadorDeConsulta();
        ConsultaAcervo consulta = montador.montar(new ConsultaAcervoBasica(app.acervo), true, metricas);
        TratadorDeConsulta tratador = new TratadorDeConsulta(consulta, metricas, montador::invalidarCache);

        tratador.consultar("");
        assertEquals("consulta medida n. 1; lida do acervo", tratador.resumoDaUltimaConsulta());
        tratador.consultar("");
        assertEquals("consulta medida n. 2; resposta do cache (validade de 5 min)", tratador.resumoDaUltimaConsulta());

        app.tratadorDeAcervo.inserirLivro("L9", "Quincas Borba");
        tratador.acervoAlterado();                                            // a montagem avisa que o acervo mudou
        List<MetadadosLivro> depois = tratador.consultar("");

        assertTrue(depois.contains(new MetadadosLivro("L9", "Quincas Borba")));
        assertEquals("consulta medida n. 3; lida do acervo", tratador.resumoDaUltimaConsulta());
    }

    @Test
    void R09_livroIncluidoOuExcluidoPelosTratadoresDescartaOCache() {
        Aplicacao app = Cenario.novo();
        assertEquals(2, app.tratadorDeConsulta.consultar("").size());
        assertEquals(2, app.tratadorDeConsulta.consultar("").size());      // segunda vez: resposta do cache

        app.tratadorDeAcervo.inserirLivro("L3", "Quincas Borba");
        assertEquals(3, app.tratadorDeConsulta.consultar("").size());      // a inclusao descartou o cache

        app.tratadorDeAcervo.excluirLivro("L3");
        assertEquals(2, app.tratadorDeConsulta.consultar("").size());      // a exclusao tambem
    }
}
