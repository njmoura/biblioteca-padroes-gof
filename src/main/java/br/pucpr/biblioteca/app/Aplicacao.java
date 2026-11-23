package br.pucpr.biblioteca.app;

import br.pucpr.biblioteca.acesso.AcervoDeLivros;
import br.pucpr.biblioteca.acesso.Movimentacao;
import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.banco.AdaptadorBancoA;
import br.pucpr.biblioteca.banco.AdaptadorBancoB;
import br.pucpr.biblioteca.banco.ApiBancoA;
import br.pucpr.biblioteca.banco.ApiBancoB;
import br.pucpr.biblioteca.banco.BancoSimulado;
import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.banco.GatewayDeCobranca;
import br.pucpr.biblioteca.banco.RegistroDeCobrancas;
import br.pucpr.biblioteca.banco.ServicoDeQuitacao;
import br.pucpr.biblioteca.cadastro.TratadorDeAcervo;
import br.pucpr.biblioteca.cadastro.TratadorDeAtendente;
import br.pucpr.biblioteca.cadastro.TratadorDeEstudante;
import br.pucpr.biblioteca.cadastro.TratadorDePessoa;
import br.pucpr.biblioteca.circulacao.ServicoDeCirculacao;
import br.pucpr.biblioteca.circulacao.iCirculacao;
import br.pucpr.biblioteca.consulta.ConsultaAcervo;
import br.pucpr.biblioteca.consulta.ConsultaAcervoBasica;
import br.pucpr.biblioteca.consulta.MontadorDeConsulta;
import br.pucpr.biblioteca.consulta.RegistroDeMetricas;
import br.pucpr.biblioteca.devolucao.AgendaDeInspecao;
import br.pucpr.biblioteca.devolucao.OuvinteEmail;
import br.pucpr.biblioteca.devolucao.OuvinteIndicadores;
import br.pucpr.biblioteca.devolucao.OuvinteInspecao;
import br.pucpr.biblioteca.devolucao.OuvinteMulta;
import br.pucpr.biblioteca.devolucao.PainelDeIndicadores;
import br.pucpr.biblioteca.devolucao.PublicadorDeDevolucao;
import br.pucpr.biblioteca.devolucao.ServicoDeEmail;
import br.pucpr.biblioteca.multa.ConfiguracaoAdministrativa;
import br.pucpr.biblioteca.multa.SeletorDePolitica;
import br.pucpr.biblioteca.multa.ServicoDeMulta;
import br.pucpr.biblioteca.organizacao.GrupoDoAcervo;
import br.pucpr.biblioteca.organizacao.OrganizadorDoAcervo;
import br.pucpr.biblioteca.organizacao.TipoDeGrupo;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import br.pucpr.biblioteca.solicitacao.ExecutorDeSolicitacoes;
import br.pucpr.biblioteca.solicitacao.RegistroDeSolicitacoes;
import br.pucpr.biblioteca.tratador.TratadorDeConsulta;
import br.pucpr.biblioteca.tratador.TratadorDeDevolucao;
import br.pucpr.biblioteca.tratador.TratadorDeEmprestimo;
import br.pucpr.biblioteca.tratador.TratadorDeMulta;
import br.pucpr.biblioteca.tratador.TratadorDeOrganizacao;

/**
 * Montagem da aplicacao: o unico lugar que conhece as classes concretas e liga umas as
 * outras. E aqui que se escolhe a familia de persistencia (pela fabrica recebida), o
 * banco da instalacao, os ouvintes da devolucao e a cadeia de decoradores da consulta.
 */
public class Aplicacao {
    // persistencia e pontos internos de acesso
    public final UnidadeDeTrabalho unidadeDeTrabalho;
    public final AcervoDeLivros acervo;
    public final Movimentacao movimentacao;
    public final Relacionamento relacionamento;

    // servicos internos
    public final iCirculacao circulacao;
    public final ConfiguracaoAdministrativa configuracao = new ConfiguracaoAdministrativa();
    public final SeletorDePolitica seletorDePolitica = new SeletorDePolitica(configuracao);
    public final RegistroDeCobrancas registroDeCobrancas = new RegistroDeCobrancas();
    public final ServicoDeQuitacao quitacao = new ServicoDeQuitacao(registroDeCobrancas);
    public final GatewayDeCobranca gateway;
    public final BancoSimulado bancoSimulado;
    public final ServicoDeMulta servicoDeMulta;
    public final PublicadorDeDevolucao publicador = new PublicadorDeDevolucao();
    public final AgendaDeInspecao agenda = new AgendaDeInspecao();
    public final PainelDeIndicadores painel = new PainelDeIndicadores();
    public final RegistroDeSolicitacoes registroDeSolicitacoes = new RegistroDeSolicitacoes();
    public final RegistroDeMetricas metricas = new RegistroDeMetricas();
    public final OrganizadorDoAcervo organizador = new OrganizadorDoAcervo(new GrupoDoAcervo("Biblioteca Central", TipoDeGrupo.UNIDADE));

    // tratadores: e so com eles que a interface conversa
    public final TratadorDePessoa tratadorDePessoa;
    public final TratadorDeEstudante tratadorDeEstudante;
    public final TratadorDeAtendente tratadorDeAtendente;
    public final TratadorDeAcervo tratadorDeAcervo;
    public final TratadorDeEmprestimo tratadorDeEmprestimo;
    public final TratadorDeDevolucao tratadorDeDevolucao;
    public final TratadorDeMulta tratadorDeMulta;
    public final TratadorDeConsulta tratadorDeConsulta;
    public final TratadorDeOrganizacao tratadorDeOrganizacao;

    public Aplicacao(FabricaDePersistencia fabrica, CodigoBanco banco, ServicoDeEmail email) {
        this(fabrica, banco, email, true);
    }

    /** comCache: a instalacao escolhe se a consulta publica usa o decorador de cache. */
    public Aplicacao(FabricaDePersistencia fabrica, CodigoBanco banco, ServicoDeEmail email, boolean comCache) {
        // Abstract Factory: uma unidade de trabalho e tres repositorios da MESMA familia
        unidadeDeTrabalho = fabrica.criarUnidadeDeTrabalho();
        acervo = new AcervoDeLivros(fabrica.criarRepositorioLivro(unidadeDeTrabalho));
        movimentacao = new Movimentacao(fabrica.criarRepositorioMovimentacao(unidadeDeTrabalho));
        relacionamento = new Relacionamento(fabrica.criarRepositorioRelacionamento(unidadeDeTrabalho));

        // Facade
        circulacao = new ServicoDeCirculacao(unidadeDeTrabalho, relacionamento, movimentacao, acervo);

        // Template Method: tratadores de cadastro
        tratadorDePessoa = new TratadorDePessoa(relacionamento);
        tratadorDeEstudante = new TratadorDeEstudante(relacionamento, tratadorDePessoa);
        tratadorDeAtendente = new TratadorDeAtendente(relacionamento, tratadorDePessoa);
        tratadorDeAcervo = new TratadorDeAcervo(acervo);

        // Adapter: o banco da instalacao e escolhido aqui, uma vez; o ServicoDeMulta so ve o GatewayDeCobranca
        if (banco == CodigoBanco.B) {
            ApiBancoB api = new ApiBancoB();
            gateway = new AdaptadorBancoB(api, quitacao);
            bancoSimulado = api;
        } else {
            ApiBancoA api = new ApiBancoA();
            gateway = new AdaptadorBancoA(api, quitacao);
            bancoSimulado = api;
        }

        // Strategy: o servico de multas calcula pela politica vigente no seletor
        servicoDeMulta = new ServicoDeMulta(seletorDePolitica, gateway, registroDeCobrancas);

        // Observer: uma nova reacao a devolucao e uma nova linha aqui, sem tocar no tratador
        publicador.registrar(new OuvinteInspecao(agenda));
        publicador.registrar(new OuvinteMulta(servicoDeMulta));
        publicador.registrar(new OuvinteIndicadores(painel));
        publicador.registrar(new OuvinteEmail(email, tratadorDePessoa));

        // Command: executor (invocador) com registro idempotente
        ExecutorDeSolicitacoes executor = new ExecutorDeSolicitacoes(registroDeSolicitacoes, circulacao);

        // Decorator: Medicao(Cache(Basica)) ou, na instalacao sem cache, Medicao(Basica)
        MontadorDeConsulta montador = new MontadorDeConsulta();
        ConsultaAcervo consulta = montador.montar(new ConsultaAcervoBasica(acervo), comCache, metricas);

        tratadorDeEmprestimo = new TratadorDeEmprestimo(circulacao, executor);
        tratadorDeDevolucao = new TratadorDeDevolucao(circulacao, publicador, servicoDeMulta, agenda, painel);
        tratadorDeMulta = new TratadorDeMulta(seletorDePolitica, servicoDeMulta, bancoSimulado);
        tratadorDeConsulta = new TratadorDeConsulta(consulta, metricas, montador::invalidarCache);
        tratadorDeOrganizacao = new TratadorDeOrganizacao(organizador, tratadorDeAcervo);

        // livro excluido do acervo sai tambem da hierarquia (o item so guarda o id do livro)
        tratadorDeAcervo.aoExcluirLivro(organizador::desalocar);
        // livro incluido, alterado ou excluido: o cache da consulta publica e descartado
        tratadorDeAcervo.aoAlterarAcervo(tratadorDeConsulta::acervoAlterado);
    }

    /** O que a interface recebe: so os tratadores. */
    public Tratadores tratadores() {
        return new Tratadores(tratadorDePessoa, tratadorDeEstudante, tratadorDeAtendente, tratadorDeAcervo,
                tratadorDeEmprestimo, tratadorDeDevolucao, tratadorDeMulta, tratadorDeConsulta, tratadorDeOrganizacao);
    }
}
