# Biblioteca — empréstimo de livros com padrões GoF

Trabalho 03 de Modelagem de Sistemas Computacionais (PUCPR, Prof. Edson Emílio Scalabrin).
Implementação executável do sistema de cadastro, reserva, empréstimo, renovação e devolução de livros modelado no Trabalho 02, aplicando os dez padrões GoF pedidos no enunciado, cada um com seu cenário de verificação automatizado.

**Equipe:** Gabriel Naumann Jerônimo de Moura e Gabriel Pacheco Benin.

## Pré-requisitos

- **JDK 17 ou superior**, com a variável `JAVA_HOME` apontando para ele.
- Maven **não** precisa estar instalado: o repositório traz o Maven Wrapper (`mvnw`). Na primeira execução ele baixa o Maven e as dependências, então é preciso internet nessa vez.
- MySQL (ou Docker, para subir o do `docker-compose.yml`) só é necessário para o perfil `PRODUCAO`. Os testes e o perfil `TESTE` rodam em memória.

## Compilar e rodar os testes (um comando)

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

O comando compila o projeto e roda todos os testes. A saída lista cada cenário de verificação pelo nome (`R01_terceiraRenovacaoRecusada`, `R02_...`), agrupado por requisito, e termina com o total e `BUILD SUCCESS`.

## Gerar e executar

```powershell
.\mvnw.cmd package
```

Esse comando também roda os testes antes de gerar `target/biblioteca.jar`.

```powershell
java -jar target/biblioteca.jar TESTE
```

```powershell
java -jar target/biblioteca.jar PRODUCAO A
```

- 1º argumento (obrigatório): perfil de execução. `TESTE` usa a família em memória; `PRODUCAO` usa a família relacional (MySQL). Sem perfil, a aplicação não sobe.
- 2º argumento (opcional): banco de cobrança da instalação, `A` ou `B`. O padrão é `A`.
- 3º argumento (opcional): consulta pública `CACHE` (medição e cache, o padrão) ou `SEMCACHE` (só medição). Exemplo: `java -jar target/biblioteca.jar TESTE B SEMCACHE`.

### Perfil PRODUCAO (MySQL)

```powershell
docker compose up -d
```

O `docker-compose.yml` sobe um MySQL com banco, usuário e senha `biblioteca`, publicado na porta **3310** do computador (para não conflitar com outro MySQL nas portas 3306 ou 3307). São os valores padrão da aplicação. Para apagar os dados e recomeçar: `docker compose down -v` e `docker compose up -d`. Para apontar para outro servidor, defina as variáveis de ambiente opcionais:

| Variável | Padrão |
|---|---|
| `BIBLIOTECA_DB_URL` | `jdbc:mysql://localhost:3310/biblioteca?useSSL=false&allowPublicKeyRetrieval=true` |
| `BIBLIOTECA_DB_USUARIO` | `biblioteca` |
| `BIBLIOTECA_DB_SENHA` | `biblioteca` |

As tabelas são criadas automaticamente na primeira conexão, a partir de `src/main/resources/schema.sql`. Não há passo manual além de ter o banco no ar.

## Roteiro da demonstração pela interface

Fluxo completo: reserva → empréstimo → renovação → devolução atrasada → multa. Execute com `java -jar target/biblioteca.jar TESTE` e digite as opções do menu. O menu aparece uma vez (a opção `m` mostra de novo) e, depois de reservar, emprestar, renovar e devolver (opções `10`, `12`, `13` e `16`), a linha `agora:` informa a situação do livro.

| Opção | O que digitar | O que aparece na tela |
|---|---|---|
| `90` | — | Dados de exemplo: pessoas, estudante `E1`, atendente `A1`, livros `L1` a `L3` e duas seções do acervo |
| `10` | `L1`, `E1`, `2026-11-01` | Reserva (f1), enviada como solicitação: `EXECUTADA`; `agora: ... esta RESERVADO` |
| `11` | `L1`, `E1` | Consulta da reserva (f2) |
| `12` | `L1`, `E1`, `2026-11-01` | Empréstimo (f3): `vencimento 2026-11-08`; `agora: ... esta EMPRESTADO` |
| `13` | `L1`, `E1` | Renovação (f4), enviada como solicitação: `novo vencimento 2026-12-08`; `agora: ... esta RENOVADO` |
| `14` | — | Reenvio da **mesma** solicitação (mesmo id): `JA PROCESSADA (resultado gravado, nada foi executado de novo)`; o vencimento continua 2026-12-08 |
| `16` | `L1`, `E1`, `2026-12-13` | Devolução (f5) com 5 dias de atraso: e-mail do ouvinte, `Multa R$ 5,00 (5 dias de atraso, politica ORIGINAL)`, boleto `A1` `[ABERTA]`; `agora: ... esta DEVOLVIDO` |
| `20` | — | Reações à devolução: `inspecoes pendentes: L1`, indicadores e falhas de ouvintes |
| `21` | — | Multas emitidas e política vigente |
| `17` | `L1`, `E1` | 2ª via do boleto de multa (f6) |
| `22` | `A1` | Pagamento simulado no banco; o banco notifica e a cobrança fica `[QUITADA]` |
| `22` | `A1` | Notificação repetida: `ignorada: a cobranca ja estava quitada, sem segunda quitacao` |
| `18` | `L1`, `s` | Inspeção aprovada: `livro L1 ... esta LIVRE` |

Com o banco B (`java -jar target/biblioteca.jar TESTE B`) o roteiro é o mesmo, mas o boleto se chama `B1`.

Extras úteis na arguição:

- **Terceira renovação recusada (requisito 1):** em outro livro, para não alterar o fluxo acima: `10` e `12` com `L2`, `E1`, `2026-11-01`, depois `13` três vezes. A terceira responde `RECUSADA: ... limite de 2 renovacoes`.
- **Livro devolvido não pode ser reservado (requisito 1):** logo depois da opção `16`, a opção `10` com `L1` é recusada: `operacao "reservar" recusada: o livro esta DEVOLVIDO`.
- **Políticas de multa (requisito 2):** `9` simula as três políticas sem criar multa; `8` troca a política vigente, e as multas já emitidas não mudam.
- **Consulta com cache e medição (requisito 9):** `15` duas vezes com o mesmo termo. A primeira responde `lida do acervo`, e a segunda, `resposta do cache`; as duas são medidas.
- **Hierarquia do acervo (requisito 10):** `26` com `Biblioteca Central` conta os livros de todas as seções; `23` cria grupos; `25` move um livro de seção (`24` só aloca livro que ainda não está em nenhum grupo).

## Pacotes e padrões

Todos os pacotes ficam em `src/main/java/br/pucpr/biblioteca/`.

| Pacote | Conteúdo | Padrão |
|---|---|---|
| `dominio` | `Livro`, os sete estados, pessoas, papéis e transações | State |
| `multa` | Políticas de multa, seletor, serviço de multas | Strategy |
| `banco` | Contrato de cobrança, adaptadores dos bancos A e B, quitação | Adapter |
| `circulacao` | `iCirculacao`, `ServicoDeCirculacao`, comprovantes | Facade |
| `devolucao` | Publicador, evento e ouvintes da devolução | Observer |
| `solicitacao` | Solicitações de reserva e renovação, executor, registro | Command |
| `cadastro` | `TratadorDeCadastro` e os quatro tratadores de cadastro | Template Method |
| `persistencia` | Fábrica abstrata, família em memória e família MySQL | Abstract Factory |
| `consulta` | Contrato de consulta, decoradores de medição e cache | Decorator |
| `organizacao` | Grupos e itens do acervo, organizador | Composite |
| `acesso` | `AcervoDeLivros`, `Movimentacao`, `Relacionamento` | pontos internos de acesso do Trabalho 02 |
| `tratador` | Tratadores de serviço (empréstimo, devolução, multa, consulta, organização) | — |
| `app` | `Aplicacao` (montagem), `Tratadores` (o que a tela recebe), `Main` e `Menu` (linha de comando) | — |

A tabela completa de requisito → padrão → classes → teste está em [RASTREABILIDADE.md](RASTREABILIDADE.md).

## Modelos UML

Na pasta `docs/`:

- `docs/Trabalho03_GoF.asta` — modelo no Astah.
- `docs/Trabalho03_GoF_diagramas.pdf` — diagramas exportados, com uma capa e um diagrama por página (33 páginas): 11 diagramas de classes (base do Trabalho 02 e um por padrão), a máquina de estados do `Livro` e 20 diagramas de sequência, pelo menos um para cada cenário de verificação do enunciado. Cada página tem o tamanho do diagrama, sem redução, então o texto fica legível com o zoom normal do leitor de PDF.
- `docs/gerador/` — o programa que gera o `.asta` pela API do Astah. As classes, os atributos, as operações, as heranças, as realizações e as associações do modelo são lidos do código compilado, por isso os diagramas de classes não divergem do código. A máquina de estados e os diagramas de sequência são descritos nesse programa e foram conferidos contra o código: cada transição é um método da classe do estado de origem, e cada mensagem é uma chamada que existe, na ordem em que acontece. Para regenerar depois de alterar o código: `.\mvnw.cmd package` e `bash docs/gerador/gerar.sh` (roda no Windows com Git Bash; precisa do Astah UML instalado, em `C:/Program Files/astah-UML` ou no caminho da variável `ASTAH_HOME`, e de Python com Pillow e reportlab).

O modelo do Trabalho 02 não foi alterado; este é um arquivo novo, com os nomes do Trabalho 02 preservados onde o enunciado pede (tratadores, interfaces, `AcervoDeLivros`/`iAcervo`, `Movimentacao` e `Relacionamento`).

## Decisões e premissas

- **Renovação:** a guarda `numRenovacoes < 2` está no método `renovar` das duas classes de onde a transição parte, `Emprestado` e `Renovado`, e é avaliada antes de alterar o vencimento. No código ela aparece como a recusa quando `getNumRenovacoes() >= Livro.MAX_RENOVACOES`.
- **Inspeção:** a inspeção aprovada é o evento `aprovarInspecao` (DEVOLVIDO para LIVRE) e a reprovada é `reprovarInspecao` (DEVOLVIDO para REPARANDO). `enviarParaReparo` vale só para um livro LIVRE com dano identificado no acervo.
- **Datas:** o empréstimo não pode ter data anterior à da reserva, nem a devolução data anterior à do empréstimo.
- **Dados do ciclo no Livro:** `vencimento`, `numRenovacoes` e `reservante` ficam no `Livro`, como na solução do professor.
- **INCINERADO:** alcançado pelo evento `registrarLaudoIrrecuperavel`, de REPARANDO para INCINERADO.
- **Numeração f1–f6:** segue o Trabalho 02 — f1 reservar, f2 consultar reserva, f3 emprestar, f4 renovar, f5 registrar devolução, f6 emitir boleto de multa.
- **Nomes do Trabalho 02 preservados:** os tratadores e as interfaces `iLivro`, `iPessoa`, `iEstudante`, `iAtendente`, `iEmprestimo`, `iDevolucao`, `iMulta` e `iAcervo`.
- **Nomes da solução do professor:** `iCobranca` e `iQuitacao` do Trabalho 02 viraram `GatewayDeCobranca` e `ReceptorDePagamento`.
- **Transações:** `Reserva`, `Emprestimo` e `Devolucao` guardam os ids do livro e do estudante, e não o atendente.
- **Prazo do primeiro empréstimo:** 7 dias. Cada renovação acrescenta 30 dias.
- **Solicitações:** reserva e renovação sempre viram um objeto de solicitação e passam pelo executor, tanto pelas opções do menu quanto pelos métodos `reservarLivro` e `renovarEmprestimo` do tratador.
- **Interface:** o `Menu` recebe apenas o registro `Tratadores` e só objetos de resultado. Os textos da tela não usam acentos, de propósito, para não depender da página de código do console do Windows.
- **Cache da consulta:** guarda só id e título, vale por 5 minutos e é descartado quando um livro é incluído, alterado ou excluído pelos tratadores.

## Limitações

- Multas, cobranças, registro de solicitações, hierarquia do acervo, agenda de inspeção e indicadores ficam **só em memória**, mesmo no perfil `PRODUCAO`. A fábrica do requisito 8 cobre os repositórios de Livro, Movimentacao e Relacionamento, como pede o enunciado.
- Os bancos A e B e o envio de e-mail são simulados.
- As datas são digitadas pelo operador. O sistema confere a ordem reserva, empréstimo e devolução dentro de um ciclo, mas não impede começar um novo ciclo do mesmo livro com data anterior à devolução do ciclo anterior.
- A família relacional é testada automaticamente contra o H2 em modo de compatibilidade MySQL. Além disso, o fluxo reserva, empréstimo, renovação e devolução atrasada foi executado manualmente, pelo menu, contra o MySQL 8.4 do `docker-compose.yml`, em dois processos seguidos, conferindo as tabelas `livro`, `reserva`, `emprestimo` e `devolucao`.

## Uso de LLM

- **Ferramenta:** Claude Code (aplicativo desktop).
- **Modelo:** Claude Fable 5.2 (Anthropic).
- **Onde foi usado:** como auxílio na escrita do código de produção, dos testes, da documentação e do programa que gera os diagramas UML, e na conferência final do repositório contra o enunciado.
- **Material de entrada:** o enunciado, o Trabalho 02 da equipe, a lista de problemas e as soluções do professor.
- **Diretrizes da equipe:** seguir a forma das soluções do professor, preservar os nomes e a estrutura do Trabalho 02 e não complicar além do necessário.

Os pedidos feitos e o que foi aceito, corrigido ou descartado estão em [REGISTRO_LLM.md](REGISTRO_LLM.md).
