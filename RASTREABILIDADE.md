# Rastreabilidade: requisito → padrão → classes → teste

Código em `src/main/java/br/pucpr/biblioteca/`; testes em `src/test/java/br/pucpr/biblioteca/`.
O mesmo fluxo da demonstração (com 4 dias de atraso) é coberto por `FluxoCompletoTest`.

Nas tabelas, a coluna **Papel** traz os papéis do padrão GoF. As linhas marcadas com *(apoio)* não são papéis do padrão: são classes que o cenário usa junto com ele.

## Requisito 1 — Ciclo de vida do livro — State

Pacote `dominio`.

| Papel | Classes |
|---|---|
| Context | `Livro` |
| State | `EstadoLivro` (abstrata) |
| ConcreteState | `Livre`, `Reservado`, `Emprestado`, `Renovado`, `Devolvido`, `Reparando`, `Incinerado` |
| Client | `ServicoDeCirculacao`, `TratadorDeAcervo` |
| *(apoio)* | `Situacao` (enumeração gravada na persistência), `OperacaoInvalidaException`, `RenovacaoNegadaException` |

**Teste:** `R01StateTest`

- `R01_terceiraRenovacaoRecusada`
- `R01_livroDevolvidoNaoInspecionadoNaoPodeSerReservado`
- `R01_laudoIrrecuperavelLevaReparandoAIncinerado`
- `R01_eventoInvalidoERecusadoSemAlterarOLivro`
- `R01_inspecaoReprovadaSoValeParaDevolvidoEEnviarParaReparoSoParaLivre`
- `R01_estadoGravadoComoSituacaoEReconstituido`

**Respostas às perguntas do enunciado**

- *Onde está a guarda da renovação e por que é avaliada antes de alterar o vencimento:* no método `renovar` do estado de origem, isto é, em `Emprestado.renovar` e em `Renovado.renovar`. A guarda `numRenovacoes < 2` aparece no código pela negação: `if (l.getNumRenovacoes() >= Livro.MAX_RENOVACOES)` lança `RenovacaoNegadaException`. Em cada um, esse `if` vem antes de `l.prorrogar(...)`, então a recusa não altera o vencimento nem o contador.
- *Evento que chega a um estado que não o aceita:* a implementação padrão de cada evento em `EstadoLivro` lança `OperacaoInvalidaException` (método `recusa`), sem tocar no `Livro`; o estado concreto simplesmente não sobrescreve o método. Por exemplo, `enviarParaReparo` só é sobrescrito em `Livre`, e `reprovarInspecao`, só em `Devolvido`. A mensagem é, por exemplo, `operacao "reservar" recusada: o livro esta DEVOLVIDO`.
- *Como o estado é gravado e reconstituído:* grava-se `livro.getSituacao()`, o valor da enumeração `Situacao` (como texto em `RepositorioLivroMySQL`). Ao ler, o construtor de `Livro` chama `EstadoLivro.de(situacao)`, que devolve o objeto de estado correspondente.

## Requisito 2 — Políticas de cálculo de multa — Strategy

Pacote `multa`.

| Papel | Classes |
|---|---|
| Strategy | `PoliticaDeMulta` |
| ConcreteStrategy | `MultaDiaria` (ORIGINAL), `MultaComTeto` (TETO), `MultaComTolerancia` (TOLERANCIA) |
| Context | `ServicoDeMulta`; `TratadorDeMulta.simular` |
| *(apoio)* seleção | `SeletorDePolitica`, `ConfiguracaoAdministrativa` |
| *(apoio)* | `Multa` (guarda valor e política aplicada) |

**Teste:** `R02StrategyTest`

- `R02_cincoDiasDeAtraso_5ReaisNaOriginalE3ReaisNaTolerancia`
- `R02_semAtrasoValorZero`
- `R02_simulacaoPelaConfiguracaoNaoCriaMulta`
- `R02_trocaDePoliticaNaoRecalculaMultaJaEmitida`
- `R02_novaPoliticaEntraSemAlterarOServicoDeMultas`

**Respostas às perguntas do enunciado**

- *Nova política sem alterar o serviço de multas:* uma classe nova que implementa `PoliticaDeMulta` mais um `seletorDePolitica.registrar(...)` na montagem (`Aplicacao`). `ServicoDeMulta.emitirMulta` só faz `seletor.vigente()` e `politica.calcular(diasAtraso)`, sem saber qual política recebeu.
- *Por que a troca de política não recalcula multas já emitidas:* `Multa` grava `valor` e `politicaAplicada` no construtor e não tem método de recálculo. Além disso, `ServicoDeMulta.emitirMulta` devolve a multa existente quando o `idDevolucao` se repete.
- *Como são calculados os dias de atraso e os valores:* dias corridos inteiros em `EventoDevolucao.diasAtraso()` (`ChronoUnit.DAYS` entre vencimento e devolução, nunca negativo); valores em `BigDecimal`, calculados no método `calcular` de cada política; `ServicoDeMulta.emitirMulta` fixa a escala em duas casas.

## Requisito 3 — Integração com bancos incompatíveis — Adapter

Pacote `banco`.

| Papel | Classes |
|---|---|
| Target | `GatewayDeCobranca` |
| Adapter | `AdaptadorBancoA`, `AdaptadorBancoB` |
| Adaptee | `ApiBancoA`, `ApiBancoB` (simuladas) |
| Client | `ServicoDeMulta` |
| *(apoio)* notificações, caminho inverso | o banco chama `receberWebhook` (A) ou `receberNotificacao` (B) do adaptador, que traduz e entrega a `ReceptorDePagamento`, realizado por `ServicoDeQuitacao` |
| *(apoio)* | `RegistroDeCobrancas`, `Cobranca`, `SolicitacaoCobranca`, `ConfirmacaoPagamento`, `SituacaoCobranca`, `CodigoBanco` |

**Teste:** `R03AdapterTest`

- `R03_seteReaisViram700CentavosNoBancoAe7ReaisNoBancoB`
- `R03_mesmoServicoDeMultasCobraNosDoisBancosSemCondicionalPorBanco`
- `R03_notificacaoRepetidaNaoGeraSegundaQuitacao`
- `R03_notificacaoDoBancoBTambemETraduzidaPeloAdaptador`
- `R03_valorComMaisDeDuasCasasFalhaAntesDeChamarOBanco`
- `R03_cpfForaDoFormatoERecusadoPeloAdaptadorDoBancoB`
- `R03_notificacaoDeCobrancaNaoPagaNaoQuita`

**Respostas às perguntas do enunciado**

- *Onde ficam as conversões de cada banco:* todas dentro do adaptador. Em `AdaptadorBancoA`: `paraCentavos` (valor), `paraSoDigitos` (CPF), `traduzir` (situação) e `receberWebhook` (notificação). Em `AdaptadorBancoB`: `paraDecimal` (valor), `paraDocumento` (CPF), `traduzir` e `receberNotificacao`. Em `emitirBoleto`, o identificador do banco (`nossoNumero` no A, `txid` no B) vira o `idExterno` da `Cobranca`. O adaptador também traduz a situação informada na notificação e só chama `receptor.confirmar` quando ela significa pagamento.
- *Como o banco da instalação é escolhido:* uma vez, na montagem (`Aplicacao`), pelo segundo argumento da linha de comando lido em `Main`. `ServicoDeMulta` só conhece `GatewayDeCobranca`.
- *Como a quitação é protegida contra notificações repetidas:* `ServicoDeQuitacao.confirmar` chama `RegistroDeCobrancas.quitarSeAberta`, que é `synchronized` e só devolve `true` se a cobrança ainda está `ABERTA`. Na segunda notificação devolve `false` e nada é quitado de novo.

## Requisito 4 — Entrada única para a circulação — Facade

Pacote `circulacao` (fachada); o subsistema fica em `acesso`, `dominio` e `persistencia`.

| Papel | Classes |
|---|---|
| Facade | `iCirculacao` (contrato), `ServicoDeCirculacao` |
| Subsistema | `Relacionamento`, `Movimentacao`, `AcervoDeLivros` (por `iAcervo`), `Livro`, `UnidadeDeTrabalho` |
| Client | `TratadorDeEmprestimo`, `TratadorDeDevolucao`, `ExecutorDeSolicitacoes` e as solicitações (`SolicitacaoDeReserva`, `SolicitacaoDeRenovacao`); por meio dos tratadores, `Menu` |
| *(apoio)* fronteira | `ComprovanteReserva`, `ComprovanteEmprestimo`, `ComprovanteRenovacao`, `ComprovanteDevolucao`, `SituacaoDTO`; `Resultado` (pacote `tratador`) |

**Teste:** `R04FacadeTest`

- `R04_efetivarEmprestimoValidaReservaRemoveReservaCriaEmprestimoEAtualizaLivro`
- `R04_reservaDeOutroEstudanteERecusadaENadaEGravado`
- `R04_emprestimoComDataAnteriorAReservaERecusadoENadaEGravado`
- `R04_devolucaoComDataAnteriorAoEmprestimoERecusadaENadaEGravado`
- `R04_falhaNoMeioDaOperacaoDesfazOsPassosJaFeitos`

**Respostas às perguntas do enunciado**

- *Quais classes a fachada coordena e em que ordem:* em `ServicoDeCirculacao.efetivarEmprestimo` — `Relacionamento` (estudante), `Movimentacao` (reserva), `AcervoDeLivros` (livro), `livro.emprestar`, `Movimentacao` (remove a reserva e insere o empréstimo) e `AcervoDeLivros` (atualiza o livro), tudo entre `iniciar()` e `confirmar()` da `UnidadeDeTrabalho`.
- *O que é desfeito se um passo falhar:* `emTransacao` chama `transacao.desfazer()` em qualquer `RuntimeException`. Todas as escritas da operação voltam, inclusive a remoção da reserva.
- *Por que a interface não acessa `Movimentacao` diretamente:* `Menu` recebe apenas o registro `Tratadores` (montado por `Aplicacao.tratadores()`), sem referência à fachada nem a `Movimentacao`, e os tratadores lhe devolvem objetos de resultado (`Resultado`, `ResultadoSolicitacao`, `ResultadoCadastro`, `MetadadosLivro` e o mapa da simulação de multas), nunca as coleções de `Movimentacao`. Assim não duplica verificações nem conhece o armazenamento.

## Requisito 5 — Reações independentes à devolução — Observer

Pacote `devolucao`.

| Papel | Classes |
|---|---|
| Subject | `PublicadorDeDevolucao` |
| Observer | `OuvinteDeDevolucao` |
| ConcreteObserver | `OuvinteInspecao`, `OuvinteMulta`, `OuvinteIndicadores`, `OuvinteEmail` |
| *(apoio)* evento, modelo push | `EventoDevolucao` |
| *(apoio)* quem dispara | `TratadorDeDevolucao.registrarDevolucao` |
| *(apoio)* | `AgendaDeInspecao`, `PainelDeIndicadores`, `ServicoDeEmail` |

**Teste:** `R05ObserverTest`

- `R05_devolucaoAtrasadaAlimentaInspecaoMultaEIndicadores`
- `R05_devolucaoNoPrazoNaoGeraMulta`
- `R05_falhaDoEmailNaoAfetaOsDemaisNemDesfazADevolucao`
- `R05_publicacaoOcorreDepoisDaConfirmacaoDaDevolucao`
- `R05_novoOuvinteEIncluidoSemAlterarOTratador`

**Respostas às perguntas do enunciado**

- *Novo ouvinte sem alterar o tratador:* uma classe nova que implementa `OuvinteDeDevolucao` mais um `publicador.registrar(...)` em `Aplicacao`. `TratadorDeDevolucao` conhece só o `PublicadorDeDevolucao`.
- *O que acontece quando o envio de e-mail falha:* o `try/catch` de `PublicadorDeDevolucao.publicar` registra a falha (`registrarFalha`) e segue para o próximo ouvinte; o laço percorre uma cópia da lista de ouvintes. A devolução já foi confirmada pela fachada antes da publicação, então não é desfeita.
- *Por que o evento leva os dados:* o evento é uma fotografia do momento da devolução. O empréstimo ativo é removido na devolução e o `vencimento` do `Livro` é apagado por `limparCiclo()` quando a inspeção é aprovada. Levando os dados, cada ouvinte não depende de `Movimentacao` nem de `AcervoDeLivros`, nem da ordem em que é notificado.

## Requisito 6 — Solicitações de circulação como objetos — Command

Pacote `solicitacao`.

| Papel | Classes |
|---|---|
| Command | `SolicitacaoDeCirculacao` (abstrata) |
| ConcreteCommand | `SolicitacaoDeReserva`, `SolicitacaoDeRenovacao` |
| Invoker | `ExecutorDeSolicitacoes`, com `RegistroDeSolicitacoes` |
| Receiver | `iCirculacao` |
| Client | `Menu` (cria a solicitação) e `TratadorDeEmprestimo.submeter` (encaminha) |
| *(apoio)* | `ResultadoSolicitacao` |

**Teste:** `R06CommandTest`

- `R06_renovacaoExecutadaDuasVezesComMesmoIdSomaApenas30Dias`
- `R06_solicitacoesDiferentesContinuamSujeitasAoLimiteDeDuasRenovacoes`
- `R06_solicitacaoTemIdentificadorAutorEInstanteEReservaPeloExecutor`
- `R06_duasSubmissoesSimultaneasDoMesmoIdExecutamUmaVezSo`
- `R06_registroEntregaAExecucaoAUmaUnicaSubmissaoEntreVariasSimultaneas`
- `R06_solicitacaoSemIdentificadorOuSemAutorNaoECriada`

**Respostas às perguntas do enunciado**

- *Quem cria, quem invoca e quem executa:* a tela (`Menu`) cria a solicitação; `ExecutorDeSolicitacoes.submeter` invoca `executar(circulacao)`, passando o receptor; `iCirculacao` (e, por ela, o `Livro`) aplica as regras.
- *Como a repetição é detectada mesmo com duas submissões simultâneas:* `RegistroDeSolicitacoes.reservarExecucao` usa `Set.add` de um conjunto concorrente (`ConcurrentHashMap.newKeySet()`), que é atômico: só uma das duas recebe `true`. A outra recebe o resultado já gravado (`resultadoDe`) ou `emProcessamento`.
- *Por que não há operação de desfazer:* a entrega física do livro não é revertida por software; `SolicitacaoDeCirculacao` só declara `executar`.

## Requisito 7 — Rotina comum para cadastros — Template Method

Pacote `cadastro`.

| Papel | Classes e métodos |
|---|---|
| AbstractClass | `TratadorDeCadastro<T, D>`, com o método-modelo `incluir` (`final`) |
| Operações primitivas | `buscarExistente`, `validar`, `criar`, `armazenar` |
| Gancho (hook) | `verificarPreRequisitos` (vazio por padrão) |
| ConcreteClass | `TratadorDeAcervo`, `TratadorDePessoa`, `TratadorDeEstudante`, `TratadorDeAtendente` |
| *(apoio)* passo comum | `apresentar` |
| *(apoio)* interfaces preservadas | `iLivro`, `iPessoa`, `iEstudante`, `iAtendente` |
| *(apoio)* | `ResultadoCadastro`, `StatusCadastro` |

**Teste:** `R07TemplateMethodTest`

- `R07_estudanteDuplicadoRetornaOCadastroExistente`
- `R07_estudanteComCpfNaoCadastradoFalhaAntesDeQualquerGravacao`
- `R07_metodoModeloNaoPodeSerSobrescrito`
- `R07_inclusaoValidaSegueAOrdemFixaDosPassos`
- `R07_atendenteTambemExigePessoaELivroEPessoaNaoTemPreRequisito`

**Respostas às perguntas do enunciado**

- *Quais passos são fixos e quais variam:* a ordem (buscar existente, validar, verificar pré-requisitos, criar, armazenar, apresentar) está em `TratadorDeCadastro.incluir`, que é `final`. Variam as quatro operações primitivas, implementadas por cada tratador.
- *O que é um gancho e onde é usado:* é um passo com implementação padrão vazia, que a subclasse sobrescreve só se precisar. Aqui é `verificarPreRequisitos`, sobrescrito apenas em `TratadorDeEstudante` e `TratadorDeAtendente`, para exigir a pessoa cadastrada.
- *Por que a falha do pré-requisito não permite gravação parcial:* o gancho roda antes de `criar` e `armazenar`. Se lançar `ExcecaoDeDominio` (por exemplo, `PessoaNaoCadastradaException`), `incluir` retorna `RECUSADO` sem chegar a esses passos.

## Requisito 8 — Famílias compatíveis de acesso aos dados — Abstract Factory

Pacote `persistencia`, com os subpacotes `memoria` e `mysql`.

| Papel | Classes |
|---|---|
| AbstractFactory | `FabricaDePersistencia` |
| ConcreteFactory | `FabricaEmMemoria`, `FabricaMySQL` |
| AbstractProduct | `UnidadeDeTrabalho`, `RepositorioLivro`, `RepositorioMovimentacao`, `RepositorioRelacionamento` |
| ConcreteProduct (memória) | `UnidadeDeTrabalhoEmMemoria`, `RepositorioLivroEmMemoria`, `RepositorioMovimentacaoEmMemoria`, `RepositorioRelacionamentoEmMemoria` |
| ConcreteProduct (MySQL) | `UnidadeDeTrabalhoMySQL`, `RepositorioLivroMySQL`, `RepositorioMovimentacaoMySQL`, `RepositorioRelacionamentoMySQL` |
| Client | `Aplicacao`, que entrega os produtos a `AcervoDeLivros`, `Movimentacao`, `Relacionamento` e `ServicoDeCirculacao` |
| *(apoio)* escolha | `ConfiguradorDePersistencia`, `Perfil` |

**Teste:** `R08AbstractFactoryTest`

- `R08_configuracaoDeTesteForneceApenasObjetosEmMemoria`
- `R08_configuracaoDeProducaoForneceRepositoriosMySQLNaMesmaUnidadeDeTrabalho`
- `R08_fabricaRecusaUnidadeDeTrabalhoDeOutraFamilia`
- `R08_perfilAusenteOuDesconhecidoInterrompeAInicializacao`
- `R08_familiaRelacionalGravaLeEDesfazNaMesmaTransacao` (SQL exercitado contra H2 em modo MySQL)
- `R08_repositoriosRelacionaisCompartilhamAMesmaTransacao`

**Respostas às perguntas do enunciado**

- *Onde a família é escolhida:* em `ConfiguradorDePersistencia.fabricaPara(perfil)`, chamado uma única vez em `Main`. O resto do sistema só conhece `FabricaDePersistencia`.
- *Como os repositórios compartilham a mesma transação:* `Aplicacao` cria uma `UnidadeDeTrabalho` e a passa aos três métodos `criarRepositorio…`. Os três repositórios MySQL escrevem pela única conexão da `UnidadeDeTrabalhoMySQL`; `confirmar()` é um único `commit`.
- *O que impede misturar um repositório em memória com outro relacional:* o método `daFamilia` de cada fábrica verifica o tipo da unidade de trabalho recebida e lança `FamiliaIncompativelException`.
- *Oracle:* é réplica mantida pela infraestrutura; nenhuma fábrica cria repositório que escreva nele.

## Requisito 9 — Consulta com recursos opcionais — Decorator

Pacote `consulta`.

| Papel | Classes |
|---|---|
| Component | `ConsultaAcervo` |
| ConcreteComponent | `ConsultaAcervoBasica` |
| Decorator | `DecoradorDeConsulta` (abstrata) |
| ConcreteDecorator | `DecoradorDeMedicao`, `DecoradorDeCache` |
| Client | `TratadorDeConsulta` |
| *(apoio)* montagem | `MontadorDeConsulta` |
| *(apoio)* | `MetadadosLivro`, `RegistroDeMetricas`, `ArmazenamentoDeCache`, `ArmazenamentoEmMemoria` |

**Teste:** `R09DecoratorTest`

- `R09_segundaConsultaUsaCacheEAindaGeraMedicao`
- `R09_instalacaoSoComMedicaoNaoUsaCache`
- `R09_resultadoAntigoDoCacheNaoAutorizaReservaDeLivroQueNaoEstaLivre`
- `R09_aOrdemDosDecoradoresImporta`
- `R09_instalacaoPodeCombinarOsDecoradoresComoQuiser`
- `R09_falhaDoCacheNaoDerrubaAConsulta`
- `R09_cacheGuardaApenasMetadados`
- `R09_telaSabeSeARespostaVeioDoCacheEAlteracaoDoAcervoODescarta`
- `R09_livroIncluidoOuExcluidoPelosTratadoresDescartaOCache`

**Respostas às perguntas do enunciado**

- *Como a cadeia é montada e por que a ordem importa:* `MontadorDeConsulta.montar` produz Medicao(Cache(Basica)) quando `comCache` é `true` e Medicao(Basica) quando é `false`; a instalação escolhe pelo terceiro argumento da linha de comando (`CACHE`, o padrão, ou `SEMCACHE`). Com a medição por fora, a consulta atendida pelo cache também é medida; na ordem inversa, ela não seria.
- *O que acontece se o cache falhar:* `DecoradorDeCache.buscarMetadados` trata a falha de leitura do cache como ausência de entrada (`lerDoCache`) e ignora a falha de gravação (`gravarNoCache`): a consulta ao `interno` roda uma única vez e o resultado é devolvido sem cache.
- *Por que o cache não participa da decisão de reserva:* o cache só guarda `MetadadosLivro` (id e título). A reserva passa por `ServicoDeCirculacao.reservar`, que lê o `Livro` em `AcervoDeLivros`, e é o estado atual do livro que decide.

## Requisito 10 — Organização hierárquica do acervo — Composite

Pacote `organizacao`.

| Papel | Classes |
|---|---|
| Component | `ComponenteDoAcervo` (abstrata) |
| Leaf | `ItemDoAcervo` |
| Composite | `GrupoDoAcervo` |
| Client | `OrganizadorDoAcervo`, `TratadorDeOrganizacao` |
| *(apoio)* | `TipoDeGrupo` (UNIDADE, SECAO, SUBSECAO), `HierarquiaInvalidaException` |

**Teste:** `R10CompositeTest`

- `R10_unidadeComSecoesDeTresEDoisLivrosInformaCinco`
- `R10_profundidadeVariavelComSubsecao`
- `R10_moverLivroEntreSecoesNaoAlteraIdEstadoNemEmprestimo`
- `R10_grupoRecusaCicloELivroQueJaPertenceAOutroGrupo`
- `R10_niveisSeguemAOrdemUnidadeSecaoSubsecao`
- `R10_grupoComNomeDeLivroNaoEscondeOLivroELivroExcluidoSaiDaArvore`
- `R10_livroExcluidoPeloTratadorSaiDaHierarquia`
- `R10_telaInformaOTipoComoTextoEOTratadorConfere`

**Respostas às perguntas do enunciado**

- *Como a contagem percorre a árvore:* `GrupoDoAcervo.contarLivros` soma `contarLivros()` de cada filho, seja grupo ou item; `ItemDoAcervo.contarLivros` devolve 1.
- *Quais regras impedem ciclos e livros em dois grupos:* estão em `GrupoDoAcervo.adicionar`, que recusa o próprio grupo, um ancestral (`ehAncestralDeste`), qualquer componente que já tenha pai e um livro cujo id já está em algum ponto da árvore. O mesmo método recusa um grupo de tipo acima do tipo do pai (por exemplo, UNIDADE dentro de SECAO).
- *Por que mover um item não toca no `Livro` nem na `Movimentacao`:* `ItemDoAcervo` referencia o livro só pelo `idLivro`. `OrganizadorDoAcervo.mover` troca apenas o pai do item (`remover` na origem, `adicionar` no destino) e não chama `AcervoDeLivros` nem `Movimentacao`. O caminho inverso existe só na exclusão: quando um livro é excluído do acervo, `OrganizadorDoAcervo.desalocar` retira o item da árvore.

---

## Pontos de extensão

Cada padrão permite acrescentar uma variante com uma classe nova mais uma linha na montagem (`Aplicacao` ou `MontadorDeConsulta`), sem alterar as classes que usam a abstração.

### Nova política de multa (Strategy)

`ServicoDeMulta` e `SeletorDePolitica` não mudam.

```java
package br.pucpr.biblioteca.multa;

import java.math.BigDecimal;

public class MultaEmDobro implements PoliticaDeMulta {
    @Override
    public String id() { return "DOBRO"; }

    @Override
    public BigDecimal calcular(int dias) {
        return new BigDecimal("2.00").multiply(BigDecimal.valueOf(Math.max(0, dias)));
    }
}
// em Aplicacao (com o import da classe): seletorDePolitica.registrar(new MultaEmDobro());
```

### Novo ouvinte da devolução (Observer)

`TratadorDeDevolucao` e `PublicadorDeDevolucao` não mudam.

```java
package br.pucpr.biblioteca.devolucao;

import java.util.ArrayList;
import java.util.List;

public class OuvinteHistorico implements OuvinteDeDevolucao {
    private final List<String> historico = new ArrayList<>();

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        historico.add(evento.idLivro() + " devolvido em " + evento.devolvidoEm());
    }

    public List<String> historico() { return historico; }
}
// em Aplicacao (com o import da classe): publicador.registrar(new OuvinteHistorico());
```

### Novo decorador da consulta (Decorator)

`ConsultaAcervoBasica` e os outros decoradores não mudam.

```java
package br.pucpr.biblioteca.consulta;

import java.util.List;

public class DecoradorDeLimite extends DecoradorDeConsulta {
    public DecoradorDeLimite(ConsultaAcervo interno) { super(interno); }

    @Override
    public List<MetadadosLivro> buscarMetadados(String termo) {
        return interno.buscarMetadados(termo).stream().limit(10).toList();
    }
}
// em MontadorDeConsulta.montar, antes do "return consulta;": consulta = new DecoradorDeLimite(consulta);
```

### Novo banco (Adapter) — esboço

Esboço: depende de uma `ApiBancoC`, que não existe no projeto. `ServicoDeMulta` não muda. Mudam a enumeração `CodigoBanco` (novo valor `C`), a leitura do argumento em `Main` e a escolha do adaptador em `Aplicacao`.

```java
public class AdaptadorBancoC implements GatewayDeCobranca {
    @Override
    public Cobranca emitirBoleto(SolicitacaoCobranca s) {
        // converte s.valor() e s.cpf() para o formato do banco C, chama a API dele e devolve
        // new Cobranca(s.idCobranca(), CodigoBanco.C, idNoBancoC, s.valor(), linhaDigitavel)
    }

    @Override
    public SituacaoCobranca consultarSituacao(Cobranca c) {
        // traduz o status do banco C para SituacaoCobranca
    }
}
```

### Novo cadastro (Template Method) — esboço

Esboço: depende de `Editora`, de `DadosEditora` e de um lugar para guardá-las, que não existem no projeto. A ordem dos passos vem pronta de `TratadorDeCadastro.incluir`; a subclasse só escreve as quatro operações primitivas.

```java
public class TratadorDeEditora extends TratadorDeCadastro<Editora, DadosEditora> {
    @Override
    protected Editora buscarExistente(DadosEditora d) { return editoras.get(d.nome()); }

    @Override
    protected void validar(DadosEditora d) { exigirPreenchido(d.nome(), "nome"); }

    @Override
    protected Editora criar(DadosEditora d) { return new Editora(d.nome()); }

    @Override
    protected void armazenar(Editora e) { editoras.put(e.nome(), e); }
}
```
